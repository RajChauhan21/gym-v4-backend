package com.backend.gym_backend.service;

import com.backend.gym_backend.response.PaymentPayload;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RazorpayPaymentService {


//    @Value("${razorpay.api.key}")
    private String apiKey;

//    @Value("${razorpay.api.secret}")
    private String secretKey;

    PaymentPayload paymentPayload = null;

    public String createOrder(double amount, String currency) throws RazorpayException {
        String id = UUID.randomUUID().toString();

        //provide api and secret key, so that we get connected to razorpay services
        RazorpayClient razorpayClient = new RazorpayClient(apiKey, secretKey);

        JSONObject orderRequest = new JSONObject();
//        int parsedAmount = Integer.parseInt(amount);
//        System.out.println(parsedAmount);
        orderRequest.put("amount", (int) amount * 100); //convert into paise
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", id);

        Order order = razorpayClient.orders.create(orderRequest);
        return order.toString();
    }

    public ResponseEntity<?> getWebHookResponse(PaymentPayload paymentPayload) {
        this.paymentPayload = paymentPayload;

        return new ResponseEntity<>(paymentPayload, HttpStatus.ACCEPTED);
    }

    public ResponseEntity<?> sendWebHookResponse() {
        return new ResponseEntity<>(paymentPayload, HttpStatus.ACCEPTED);
    }
}
