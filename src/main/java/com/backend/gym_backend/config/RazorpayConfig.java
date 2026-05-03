package com.backend.gym_backend.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RazorpayConfig {

    @Value("${razorpay.api.key}")
    private String apiKey;

    @Value("${razorpay.api.secret}")
    private String secretKey;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        return new RazorpayClient(apiKey,secretKey);
    }

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
