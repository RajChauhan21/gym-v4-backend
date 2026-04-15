package com.backend.gym_backend.controller;

import com.backend.gym_backend.service.RazorpayPaymentService;
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

    @PostMapping("/create-subscription")
    public ResponseEntity<String> createSubscription(@RequestParam("o") Integer ownerId, @RequestParam("p") Integer planId) throws RazorpayException {
        return new ResponseEntity<>(razorpayPaymentService.createSubscription(planId,ownerId), HttpStatus.ACCEPTED);
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> receiveWebHookResponse(@RequestBody String payload, HttpServletRequest request){
        return new ResponseEntity<>(razorpayPaymentService.getWebHookResponse(payload,request), HttpStatus.ACCEPTED);
    }
}

