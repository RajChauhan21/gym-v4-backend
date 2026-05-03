package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.RazorpayWebhookEvent;
import com.backend.gym_backend.entity.Plan;
import com.backend.gym_backend.repo.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private OwnerPaymentService ownerPaymentService;

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
        long startAt = (System.currentTimeMillis() / 1000) + (60); // start after 1 mins
//        subRequest.put("start_at", startAt);
        JSONObject notes = new JSONObject();
        notes.put("ownerId", ownerId);
        notes.put("planId", planId);
        subRequest.put("notes", notes);

        log.info("subs object {}", subRequest);

        com.razorpay.Subscription razorSub =
                razorpayClient.subscriptions.create(subRequest);

        // ✅ STEP 4: Return subscription_id
        return razorSub.get("id").toString();
    }
//    @Async
    public void getWebHookResponse(String payload, String signature) {
        // 1. Verify signature first
//        if (!verifySignature(payload, signature)) {
//            log.error("Invalid Razorpay signature");
//            return;
//        }
        log.info("Webhook Thread: {}", Thread.currentThread().getName());
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
                subscriptionService.handleSubscriptionLogic(data.getSubscription().getEntity(), event.getEvent());
            }

            // Handle Payment if present (even if it's inside a subscription event)
            if (data.getPayment() != null) {
                ownerPaymentService.handlePaymentLogic(data.getPayment().getEntity(), event.getEvent());
            }

            // Handle Invoice if present
            if (data.getInvoice() != null) {
                invoiceService.handleInvoiceLogic(data.getInvoice().getEntity(), event.getEvent());
            }

        } catch (Exception e) {
            log.error("Error processing webhook payload: ", e);
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
}
