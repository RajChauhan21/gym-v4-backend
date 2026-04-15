package com.backend.gym_backend.service;

import com.backend.gym_backend.entity.Plan;
import com.backend.gym_backend.repo.PlanRepository;
import com.backend.gym_backend.response.InvoicePaidEvent;
import com.backend.gym_backend.response.PaymentCaptureEvent;
import com.backend.gym_backend.response.PaymentPayload;
import com.backend.gym_backend.response.SubscriptionActivatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Subscription;
import com.razorpay.Utils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RazorpayPaymentService {


    @Value("${razorpay.api.key}")
    private String apiKey;

    @Value("${razorpay.api.secret}")
    private String secretKey;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    private ObjectMapper objectMapper;

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
        subRequest.put("total_count", 1200);

        log.info("subs object {}",subRequest);

        Subscription razorSub =
                razorpayClient.subscriptions.create(subRequest);

        // ✅ STEP 3: Save in DB
        subscriptionService.ownerSubscribesToPlan(
                ownerId,
                planId,
                razorSub.get("id").toString(),
                "CREATE"
        );

        // ✅ STEP 4: Return subscription_id
        return razorSub.get("id").toString();
    }

    public ResponseEntity<?> getWebHookResponse(String payload, HttpServletRequest request) {
        String signature = request.getHeader("X-Razorpay-Signature");
        // 🔐 Step 1: Verify signature (VERY IMPORTANT)
        boolean isValid = verifySignature(payload, signature);

        if (!isValid) {
            return ResponseEntity.status(400).body("Invalid signature");
        }

        try {
            JSONObject json = new JSONObject(payload);
            String eventType = json.getString("event");

            switch (eventType) {

                case "subscription.activated":
                    handleSubscriptionActivated(payload);
                    break;

                case "payment.captured":
                    handlePaymentCaptured(payload);
                    break;

                case "invoice.paid":
                    handleInvoicePaid(payload);
                    break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<?> sendWebHookResponse() {
        return new ResponseEntity<>(null, HttpStatus.ACCEPTED);
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
        log.info("subscription response {payload}", payload);
        SubscriptionActivatedEvent event =
                objectMapper.readValue(payload, SubscriptionActivatedEvent.class);

        String subId = event.getPayload()
                .getSubscription()
                .getEntity()
                .getId();

//        subscriptionService.activateSubscription(subId);
    }

    private void handlePaymentCaptured(String payload) throws Exception {
        log.info("payment response {payload}", payload);
        PaymentCaptureEvent event =
                objectMapper.readValue(payload, PaymentCaptureEvent.class);

        var entity = event.getPayload().getPayment().getEntity();

//        paymentService.savePayment(
//                entity.getId(),
//                entity.getAmount()
//        );

    }

    private void handleInvoicePaid(String payload) throws Exception {
        log.info("invoice response {payload}", payload);
        InvoicePaidEvent event =
                objectMapper.readValue(payload, InvoicePaidEvent.class);

        event.getPayload().getInvoice().getEntity();

//        invoiceService.saveInvoice(
//                entity.getId(),
//                entity.getSubscription_id()
//        );
    }
}
