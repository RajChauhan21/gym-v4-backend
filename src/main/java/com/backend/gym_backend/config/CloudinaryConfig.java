package com.backend.gym_backend.config;


import com.cloudinary.Cloudinary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {

    @Value("${cloudinary_cloud_name}")
    private String cloudName;

    @Value("${cloudinary_api_key}")
    private String apiKey;

    @Value("${cloudinary_secret_key}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary(){
        Map<String, String> config = new HashMap<>();
        config.put("cloud_name",cloudName);
        config.put("api_key",apiKey);
        config.put("api_secret",apiSecret);
        return new Cloudinary(config);

    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(3000)) // Set connection timeout
                .setReadTimeout(Duration.ofMillis(3000))    // Set read timeout
                .build();
    }
}
