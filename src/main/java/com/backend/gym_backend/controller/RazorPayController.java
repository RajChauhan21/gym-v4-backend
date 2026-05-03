package com.backend.gym_backend.controller;

import com.backend.gym_backend.service.RazorpayPaymentService;
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

    @PostMapping("/create-subscription")
    public ResponseEntity<String> createSubscription(@RequestParam("o") Integer ownerId, @RequestParam("p") Integer planId) throws RazorpayException {
        return new ResponseEntity<>(razorpayPaymentService.createSubscription(planId, ownerId), HttpStatus.ACCEPTED);
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
        eventListenerService.cancelSubscriptionInRazorpay(subsId);
        return ResponseEntity.ok("Simulation completed");
    }

    @GetMapping("/test")
    public String runTest() {
        webhookTestRunner.runParallelTest();
        return "Triggered";
    }
}

