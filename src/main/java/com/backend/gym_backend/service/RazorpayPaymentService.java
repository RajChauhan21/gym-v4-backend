package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.RazorpayWebhookEvent;
import com.backend.gym_backend.entity.Plan;
import com.backend.gym_backend.repo.PlanRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
    public void getWebHookResponse(String payload, String signature) { // Changed to void and String signature

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
}
