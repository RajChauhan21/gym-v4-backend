package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.RazorpayWebhookEvent;
import com.backend.gym_backend.entity.Invoice;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.entity.OwnerPayment;
import com.backend.gym_backend.entity.Plan;
import com.backend.gym_backend.enums.Payment;
import com.backend.gym_backend.enums.Status;
import com.backend.gym_backend.repo.*;
import com.backend.gym_backend.response.InvoicePaidEvent;
import com.backend.gym_backend.response.PaymentCaptureEvent;
import com.backend.gym_backend.response.SubscriptionActivatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Subscription;
import com.razorpay.Utils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;

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
        if (!verifySignature(payload, signature)) {
            log.error("Invalid Razorpay signature");
            return;
        }

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

    @Transactional
    private void handleInvoiceLogic(RazorpayWebhookEvent.InvoiceEntity entity, String event) {
        Invoice invoice = invoiceRepository.findByRazorpayInvoiceId(entity.getId()).orElseGet(
                () -> {
                    Invoice newInvoice = new Invoice();
                    newInvoice.setRazorpayInvoiceId(entity.getId());
                    newInvoice.setCreatedAt(LocalDateTime.now());
                    return newInvoice;
                }
        );

        if (invoice.getStatus() != null && com.backend.gym_backend.enums.Invoice.PAID.equals(invoice.getStatus()) && "invoice.paid".equals(event)) {
            log.info("Skipping status update: Already paid.");
            return;
        }
        String email = null;

        if (entity.getCustomer_details() != null) {
            email = entity.getCustomer_details().getEmail();
        }

        Owner owner = null;
        if (email != null) {
            owner = ownerRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Owner not found"));
            if (owner == null) {
                log.warn("Owner not found for subscription {}", entity.getId());
            }
        }

        com.backend.gym_backend.entity.Subscription subscription = null;

        if (entity.getSubscription_id() != null) {
            subscription = subscriptionRepository
                    .findByRazorpaySubscriptionId(entity.getSubscription_id())
                    .orElse(null);
        } else {
            log.warn("No subscription_id found for invoice {}", entity.getId());
        }
        invoice.setCurrency(entity.getCurrency());
        BigDecimal amount = BigDecimal.valueOf(entity.getAmount())
                .divide(BigDecimal.valueOf(100));

        invoice.setAmount(amount);
        invoice.setInvoiceUrl(entity.getShort_url());
        BigDecimal paidAmount = entity.getAmount_paid() != null
                ? BigDecimal.valueOf(entity.getAmount_paid()).divide(BigDecimal.valueOf(100))
                : amount;

        invoice.setAmountPaid(paidAmount);
        invoice.setOwner(owner);
        if (entity.getBilling_end() != null) {
            invoice.setBillingEnd(convertEpochToLocalDate(Long.parseLong(entity.getBilling_end())));
        }
        if (entity.getBilling_start() != null) {
            invoice.setBillingStart(convertEpochToLocalDate(Long.valueOf(entity.getBilling_start())));
        }
        switch (event) {
            case "invoice.paid" -> invoice.setStatus(com.backend.gym_backend.enums.Invoice.PAID);
            case "invoice.failed" -> invoice.setStatus(com.backend.gym_backend.enums.Invoice.FAILED);
        }

        if (entity.getIssued_at() != null) {
            invoice.setIssuedAt(convertEpochToLocalDate(entity.getIssued_at()));
        }
        if (entity.getPaid_at() != null) {
            invoice.setPaidAt(convertEpochToLocalDate(entity.getPaid_at()));
        }
        invoice.setUpdatedAt(LocalDateTime.now());
        invoice.setSubscription(subscription);
        try {
            invoiceRepository.save(invoice);
            log.info("invoice record saved successfully of event {}", event);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent update blocked for invoice: {}", invoice.getId());
        }
        if (entity.getPayment_id() != null) {
            ownerPaymentRepository.findByRazorpayPaymentId(entity.getPayment_id())
                    .ifPresent(payment -> {
                        if (invoice.getPayments() == null) {
                            invoice.setPayments(new ArrayList<>());
                        }

                        if (!invoice.getPayments().contains(payment)) {
                            invoice.getPayments().add(payment);
                        }

                        if (invoice.getSubscription() != null) {
                            if (payment.getSubscription() == null) {
                                payment.setSubscription(invoice.getSubscription());
                            }
                        }
                        log.info("Linked payment {} with invoice {}", payment.getId(), invoice.getId());
                        if (payment.getInvoice() == null) {
                            payment.setInvoice(invoice);
                        }
                        ownerPaymentRepository.save(payment);
                    });
        }
    }

    @Transactional
    private void handlePaymentLogic(RazorpayWebhookEvent.PaymentEntity entity, String event) {
        OwnerPayment ownerPayment = ownerPaymentRepository.findByRazorpayPaymentId(entity.getId()).orElseGet(
                () -> {
                    OwnerPayment newPayment = new OwnerPayment();
                    newPayment.setRazorpayPaymentId(entity.getId());
                    newPayment.setCreatedAt(LocalDateTime.now());
                    return newPayment;
                }
        );

        boolean isDuplicateCaptured =
                Payment.CAPTURED.equals(ownerPayment.getStatus())
                        && "payment.captured".equals(event);

//        if (Payment.ACTIVE.equals(ownerPayment.getStatus())) {
//            log.info("Already captured. Skipping duplicate webhook.");
//            return;
//        }
//        Owner owner = ownerRepository.findByEmail(entity.getEmail()).get();

        String email = null;
        Owner owner = null;
        Invoice invoice = null;

        if (entity.getEmail() != null) {
            email = entity.getEmail();
        }

        if (email != null) {
            owner = ownerRepository.findByEmail(email)
                    .orElse(null);
            if (owner == null) {
                log.warn("Owner not found for subscription {}", entity.getId());
            }
        }

        if (entity.getInvoice_id() != null) {
            invoice = invoiceRepository.findByRazorpayInvoiceId(entity.getInvoice_id()).orElse(null);
        }
        ownerPayment.setAmount(BigDecimal.valueOf(entity.getAmount())
                .divide(BigDecimal.valueOf(100)));
        ownerPayment.setEmail(entity.getEmail());
        if (entity.getCreated_at() != null) {
            ownerPayment.setCapturedAt(convertEpochToLocalDate(Long.valueOf(entity.getCreated_at())));
        }
        ownerPayment.setUpdatedAt(LocalDateTime.now());
        if (entity.getCurrency() != null) {
            ownerPayment.setCurrency(entity.getCurrency());
        }
        switch (event) {
            case "payment.captured":
                if (!isDuplicateCaptured) ownerPayment.setStatus(Payment.CAPTURED);
                break;
            case "payment.failed":
                ownerPayment.setStatus(Payment.FAILED);
                break;
            case "payment.authorized":
                ownerPayment.setStatus(Payment.AUTHORIZED);
                break;
//            default:
//                ownerPayment.setStatus(Payment.CREATED);
        }
        if (entity.getContact() != null) {
            ownerPayment.setContact(entity.getContact());
        }
        ownerPayment.setOwner(owner);
        if (invoice != null) {
            ownerPayment.setInvoice(invoice);

            if (invoice.getSubscription() != null) {
                ownerPayment.setSubscription(invoice.getSubscription());
            }
        }

        if (entity.getMethod() != null) {
            ownerPayment.setMethod(entity.getMethod());
        }
        if (ownerPayment.getSubscription() == null) {
            com.backend.gym_backend.entity.Subscription subscription = subscriptionRepository
                    .findTopByEmailOrderByCreatedAtDesc(entity.getEmail()) // or contact
                    .orElse(null);

            if (subscription != null) {
                ownerPayment.setSubscription(subscription);
            }
        }
        try {
            ownerPaymentRepository.save(ownerPayment);
            log.info("payment record saved successfully of event {}", event);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent update blocked for payment: {}", ownerPayment.getId());
        }

    }
    @Transactional
    private void handleSubscriptionLogic(RazorpayWebhookEvent.SubscriptionEntity entity, String event) {
        com.backend.gym_backend.entity.Subscription subscription = subscriptionRepository.findByRazorpaySubscriptionId(entity.getId())
                .orElseGet(() -> {
                    com.backend.gym_backend.entity.Subscription newSub = new com.backend.gym_backend.entity.Subscription();
                    newSub.setRazorpaySubscriptionId(entity.getId());
                    newSub.setCreatedAt(LocalDateTime.now());
                    return newSub;
                });

        boolean isDuplicateActive =
                Status.ACTIVE.equals(subscription.getStatus())
                        && "active".equals(entity.getStatus());

//        if (Status.CANCELLED.equals(subscription.getStatus())) {
//            log.info("Subscription already cancelled, ignoring updates");
//            return;
//        }
//        if (Status.ACTIVE.equals(subscription.getStatus())
//                && "active".equals(entity.getStatus())) {
//            log.info("Already active subscription, skipping");
//            return;
//        }
        String email = null;

        if (entity.getCustomer_email() != null) {
            email = entity.getCustomer_email();
        }

        Owner owner = null;
        if (email != null) {
            owner = ownerRepository.findByEmail(email)
                    .orElse(null);
            if (owner == null) {
                log.warn("Owner not found for subscription {}", entity.getId());
            }
        }
        if (entity.getCharge_at() != null) {
            subscription.setNextBillingDate(convertEpochToLocalDate(entity.getCharge_at()));
        }
        if (entity.getCurrent_start() != null) {
            subscription.setStartDate(convertEpochToLocalDate(entity.getCurrent_start()));
        }
        if (entity.getCurrent_end() != null) {
            subscription.setEndDate(convertEpochToLocalDate(entity.getCurrent_end()));
        }
        if (entity.getStart_at() != null) {
            subscription.setSubscriptionStartDate(convertEpochToLocalDate(entity.getStart_at()));
        }
        if (entity.getEnd_at() != null) {
            subscription.setSubscriptionEndDate(convertEpochToLocalDate(entity.getEnd_at()));
        }
        if (entity.getCustomer_contact() != null) {
            subscription.setContact(entity.getCustomer_contact());
        }
        if (entity.getCustomer_email() != null) {
            subscription.setEmail(entity.getCustomer_email());
        }
        String status = entity.getStatus();

        switch (status) {
            case "created" -> subscription.setStatus(Status.CREATED);
            case "authenticated" -> subscription.setStatus(Status.AUTHENTICATED);
            case "active" -> {
                if(!isDuplicateActive) subscription.setStatus(Status.ACTIVE);
            }
            case "cancelled" -> subscription.setStatus(Status.CANCELLED);
            case "completed" -> subscription.setStatus(Status.COMPLETED);
            default -> subscription.setStatus(Status.CREATED);
        }
        subscription.setUpdatedAt(LocalDateTime.now());
        subscription.setOwner(owner);
        try {
            subscriptionRepository.save(subscription);
            log.info("subscription record saved successfully of event {}", event);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent update blocked for subscription: {}", subscription.getId());
        }
    }

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
