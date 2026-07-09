package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.VerifySubscriptionRequest;
import com.backend.gym_backend.service.RazorpayPaymentService;
import com.backend.gym_backend.service.SubscriptionCancellationService;
import com.backend.gym_backend.service.SubscriptionEventListenerService;
import com.backend.gym_backend.service.WebhookTestRunner;
import com.razorpay.RazorpayException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/razorpay")
public class RazorPayController {

    @Autowired
    private RazorpayPaymentService razorpayPaymentService;

    @Autowired
    private SubscriptionEventListenerService eventListenerService;

    @Autowired
    private WebhookTestRunner webhookTestRunner;

    @Autowired
    private SubscriptionCancellationService subscriptionCancellationService;

    @PostMapping("/create-subscription")
    public ResponseEntity<String> createSubscription(@RequestParam("o") Integer ownerId, @RequestParam("p") Integer planId) throws RazorpayException {
        return new ResponseEntity<>(razorpayPaymentService.createSubscription(planId, ownerId), HttpStatus.ACCEPTED);
    }

    @PostMapping("/verify-subscription-payment")
    public ResponseEntity<?> verifySubscriptionPayment(@RequestBody VerifySubscriptionRequest request){
        return new ResponseEntity<>(razorpayPaymentService.verifySubscriptionPayment(request),HttpStatus.ACCEPTED);
    }

    @PostMapping("/upgrade-subscription")
    public ResponseEntity<String> upgradeSubscription(@RequestParam("o") Integer ownerId, @RequestParam("p") Integer planId) throws RazorpayException {
        return new ResponseEntity<>(razorpayPaymentService.upgradeSubscription(planId, ownerId), HttpStatus.ACCEPTED);
    }

    @GetMapping("/cancel-subscription")
    public ResponseEntity<String> cancelSubscription(@RequestParam("o") Integer ownerId) throws RuntimeException, RazorpayException {
        return new ResponseEntity<>(razorpayPaymentService.cancelSubscription(ownerId), HttpStatus.ACCEPTED);
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> captureWebHookResponse(@RequestBody String payload, HttpServletRequest request) {
        // Extract signature while still in the request thread
        String signature = request.getHeader("X-Razorpay-Signature");

        // Hand off values to async service
        razorpayPaymentService.getWebHookResponse(payload, signature);

        // Return 202 Accepted immediately so Razorpay doesn't time out
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    @GetMapping("/simulate/fail")
    public ResponseEntity<?> simulateRazorpayCancelApiFail(@RequestParam("s") String subsId) {
        subscriptionCancellationService.cancelSubscriptionInRazorpay(subsId);
        return ResponseEntity.ok("Simulation completed");
    }

    @GetMapping("/test")
    public String runTest() {
        webhookTestRunner.runParallelTest();
        return "Triggered";
    }
}

