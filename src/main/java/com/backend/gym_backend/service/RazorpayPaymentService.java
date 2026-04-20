package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.RazorpayWebhookEvent;
import com.backend.gym_backend.entity.Invoice;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.entity.OwnerPayment;
import com.backend.gym_backend.entity.Plan;
import com.backend.gym_backend.enums.Payment;
import com.backend.gym_backend.repo.*;
import com.backend.gym_backend.response.InvoicePaidEvent;
import com.backend.gym_backend.response.PaymentCaptureEvent;
import com.backend.gym_backend.response.SubscriptionActivatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Subscription;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@Slf4j
@RequiredArgsConstructor
public class RazorpayPaymentService {


    private final ObjectMapper objectMapper;
    @Value("${razorpay.api.key}")
    private String apiKey;

    @Value("${razorpay.api.secret}")
    private String secretKey;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private OwnerPaymentRepository ownerPaymentRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    public String createSubscriptions(int planId, int ownerId) throws RazorpayException {
        if (!planRepository.existsById(planId)) {
            throw new RuntimeException("Plan not found");
        }
        Plan plan = planRepository.findById(planId).get();

        //provide api and secret key, so that we get connected to razorpay services
        RazorpayClient razorpayClient = new RazorpayClient(apiKey, secretKey);

        JSONObject planRequest = new JSONObject();
        planRequest.put("period", "monthly");
        planRequest.put("interval", 1);
        planRequest.put("item", new JSONObject()
                .put("name", plan.getName())
                .put("amount", plan.getPrice() * 100) // paise
                .put("currency", "INR"));
        System.out.println("plan " + planRequest);

        if (plan.getRazorPayPlanId().isEmpty()) {
            com.razorpay.Plan razorPlan = razorpayClient.plans.create(planRequest);
            plan.setRazorPayPlanId(razorPlan.get("id"));
            planRepository.save(plan);
        }
        JSONObject subRequest = new JSONObject();
        subRequest.put("plan_id", plan.getRazorPayPlanId());
        subRequest.put("customer_notify", 1); //send notification to customer
        subRequest.put("total_count", 9999); // months
        System.out.println("Subs " + subRequest);

        Subscription razorPaySubscription = razorpayClient.subscriptions.create(subRequest);

        subscriptionService.ownerSubscribesToPlan(ownerId, planId, razorPaySubscription.get("id"), "CREATE");
        return razorPaySubscription.get("id");
    }

    public String createSubscription(int planId, int ownerId) throws RazorpayException {

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan not found"));

        RazorpayClient razorpayClient = new RazorpayClient(apiKey, secretKey);

        // ✅ STEP 1: Ensure Razorpay Plan exists
        if (plan.getRazorPayPlanId() == null || plan.getRazorPayPlanId().isEmpty()) {

            synchronized (this) { // prevent duplicate creation

                // Double check (important in concurrent env)
                plan = planRepository.findById(planId).get();

                if (plan.getRazorPayPlanId() == null || plan.getRazorPayPlanId().isEmpty()) {

                    JSONObject planRequest = new JSONObject();
                    planRequest.put("period", "monthly"); // ideally from DB
                    planRequest.put("interval", 1);
                    log.info("Plan object {}", planRequest);
                    JSONObject item = new JSONObject();
                    item.put("name", plan.getName());
                    item.put("amount", plan.getPrice() * 100); // paise
                    item.put("currency", "INR");

                    planRequest.put("item", item);

                    com.razorpay.Plan razorPlan = razorpayClient.plans.create(planRequest);

                    plan.setRazorPayPlanId(razorPlan.get("id").toString());
                    planRepository.save(plan);
                }
            }
        }

        // ✅ STEP 2: Create subscription
        JSONObject subRequest = new JSONObject();
        subRequest.put("plan_id", plan.getRazorPayPlanId());
        subRequest.put("customer_notify", 1);
        subRequest.put("total_count", 120);
        long startAt = (System.currentTimeMillis() / 1000) + (10 * 60); // start after 10 mins
        subRequest.put("start_at", startAt);

        log.info("subs object {}", subRequest);

        Subscription razorSub =
                razorpayClient.subscriptions.create(subRequest);

        // ✅ STEP 3: Save in DB
        subscriptionService.ownerSubscribesToPlan(
                ownerId,
                planId,
                razorSub.get("id").toString(),
                "CREATE"
        );

        Subscription fetchSubs = razorpayClient.subscriptions.fetch(razorSub.get("id").toString());
        log.info("subs fetch response {}", fetchSubs);

        // ✅ STEP 4: Return subscription_id
        return razorSub.get("id").toString();
    }

    @Async
    public void getWebHookResponses(String payload, String signature) { // Changed to void and String signature

        // 🔐 Step 1: Verify signature
        boolean isValid = verifySignature(payload, signature);

        if (!isValid) {
            log.error("Invalid Razorpay signature");
            return;
        }

        try {
            JSONObject json = new JSONObject(payload);
            String eventType = json.getString("event");
            log.info("Processing event: {}", eventType);

            switch (eventType) {
                case "subscription.activated": //imp event
                case "subscription.authenticated":
                    handleSubscriptionActivated(payload);
                    break;
                case "payment.captured": //imp event
                case "payment.authorized":
                    handlePaymentCaptured(payload);
                    break;
                case "invoice.paid":
                    handleInvoicePaid(payload);
                    break;
            }
        } catch (Exception e) {
            log.error("Error processing webhook: ", e);
        }
    }

    @Async
    public void getWebHookResponse(String payload, String signature) {
        // 1. Verify signature first
//        if (!verifySignature(payload, signature)) {
//            log.error("Invalid Razorpay signature");
//            return;
//        }

        try {
            // 2. Parse using Jackson into the Unified DTO
            ObjectMapper mapper = new ObjectMapper();
            RazorpayWebhookEvent event = mapper.readValue(payload, RazorpayWebhookEvent.class);

            log.info("Processing event: {}", event.getEvent());
            RazorpayWebhookEvent.Payload data = event.getPayload();
            log.info("class {}", event);

            // 3. Process entities independently (Handles multiple entities in one event)

            // Handle Subscription if present
            if (data.getSubscription() != null) {
                handleSubscriptionLogic(data.getSubscription().getEntity(), event.getEvent());
            }

            // Handle Payment if present (even if it's inside a subscription event)
            if (data.getPayment() != null) {
                handlePaymentLogic(data.getPayment().getEntity(), event.getEvent());
            }

            // Handle Invoice if present
            if (data.getInvoice() != null) {
                handleInvoiceLogic(data.getInvoice().getEntity(), event.getEvent());
            }

        } catch (Exception e) {
            log.error("Error processing webhook payload: ", e);
        }
    }

    private void handleInvoiceLogic(RazorpayWebhookEvent.InvoiceEntity entity, String event) {
        Invoice invoice = invoiceRepository.findByRazorpayInvoiceId(entity.getId()).orElseGet(
                () -> {
                    Invoice newInvoice = new Invoice();
                    newInvoice.setRazorpayInvoiceId(entity.getId());
                    newInvoice.setCreatedAt(LocalDateTime.now());
                    return newInvoice;
                }
        );

        if (invoice.getStatus() != null && invoice.getStatus().equals(com.backend.gym_backend.enums.Invoice.PAID)) {
            log.info("Skipping status update: Already paid.");
            return;
        }

        com.backend.gym_backend.entity.Subscription subscription = subscriptionRepository.findByRazorpaySubscriptionId(entity.getSubscription_id()).orElse(null);

        invoice.setCurrency(entity.getCurrency());
        invoice.setAmount(entity.getAmount());
        invoice.setInvoiceUrl(entity.getShort_url());
        invoice.setAmountPaid(entity.getAmount());
        invoice.setBillingEnd(convertEpochToLocalDate(Long.valueOf(entity.getBilling_end())));
        invoice.setBillingStart(convertEpochToLocalDate(Long.valueOf(entity.getBilling_start())));
        invoice.setStatus(com.backend.gym_backend.enums.Invoice.PAID);
        invoice.setIssuedAt(convertEpochToLocalDate(entity.getIssued_at()));
        invoice.setPaidAt(convertEpochToLocalDate(entity.getPaid_at()));
        invoice.setUpdatedAt(LocalDateTime.now());
        invoice.setSubscription(subscription);

    }

    private void handlePaymentLogic(RazorpayWebhookEvent.PaymentEntity entity, String event) {
        OwnerPayment ownerPayment = ownerPaymentRepository.findByRazorpayPaymentId(entity.getId()).orElseGet(
                () -> {
                    OwnerPayment newPayment = new OwnerPayment();
                    newPayment.setRazorpayPaymentId(entity.getId());
                    newPayment.setCreatedAt(LocalDateTime.now());
                    return newPayment;
                }
        );

        if (ownerPayment.getStatus() != null && ownerPayment.getStatus().equals(Payment.ACTIVE)) {
            log.info("Skipping status update: Already active.");
            return;
        }
        Owner owner = ownerRepository.findByEmail(entity.getEmail()).get();
        Invoice invoice = null;
        if (entity.getInvoice_id() != null) {
            invoice = invoiceRepository.findByRazorpayInvoiceId(entity.getInvoice_id()).orElse(null);
        }
        ownerPayment.setAmount(entity.getAmount());
        ownerPayment.setEmail(entity.getEmail());
        if (entity.getCreated_at() != null) {
            ownerPayment.setCapturedAt(convertEpochToLocalDate(Long.valueOf(entity.getCreated_at())));
        }
        ownerPayment.setUpdatedAt(LocalDateTime.now());
        ownerPayment.setCurrency(entity.getCurrency());
        ownerPayment.setStatus(entity.getStatus().equals("authorized") ? Payment.AUTHORIZED : entity.getStatus().equals("failed") ? Payment.FAILED : entity.getStatus().equals("captured") ? Payment.ACTIVE : Payment.CREATED);
        ownerPayment.setContact(entity.getContact());
//        ownerPayment.setSubscription();
        ownerPayment.setOwner(owner);
        ownerPayment.setInvoice(invoice);

        try {
            ownerPaymentRepository.save(ownerPayment);
            log.info("payment record saved successfully of event {}", event);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent update blocked for payment: {}", ownerPayment.getId());
        }

    }

    private void handleSubscriptionLogic(RazorpayWebhookEvent.SubscriptionEntity entity, String event) {
    }


//    public ResponseEntity<?> sendWebHookResponse() {
//        return new ResponseEntity<>(null, HttpStatus.ACCEPTED);
//    }

    private boolean verifySignature(String payload, String signature) {
        try {
            return Utils.verifyWebhookSignature(
                    payload,
                    signature,
                    secretKey
            );
        } catch (Exception e) {
            return false;
        }
    }

    private void handleSubscriptionActivated(String payload) throws Exception {
        log.info("subscription response {}", payload);
        SubscriptionActivatedEvent event =
                objectMapper.readValue(payload, SubscriptionActivatedEvent.class);

        String subId = event.getPayload()
                .getSubscription()
                .getEntity()
                .getId();

        subscriptionService.activateSubscription(event);
    }

    private void handlePaymentCaptured(String payload) throws Exception {
        log.info("payment response {}", payload);
        PaymentCaptureEvent event =
                objectMapper.readValue(payload, PaymentCaptureEvent.class);

        var entity = event.getPayload().getPayment().getEntity();

//        paymentService.savePayment(
//                entity.getId(),
//                entity.getAmount()
//        );

    }

    private void handleInvoicePaid(String payload) throws Exception {
        log.info("invoice response {}", payload);
        InvoicePaidEvent event =
                objectMapper.readValue(payload, InvoicePaidEvent.class);

        event.getPayload().getInvoice().getEntity();

//        invoiceService.saveInvoice(
//                entity.getId(),
//                entity.getSubscription_id()
//        );
    }

    public LocalDate convertEpochToLocalDate(Long epochSeconds) {
        if (epochSeconds == null) {
            return null;
        }
        return Instant.ofEpochSecond(epochSeconds)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }


}
