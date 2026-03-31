package com.backend.gym_backend.service;

import io.github.bucket4j.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    private Bucket newBucket() {

        Bandwidth limit = Bandwidth.classic(
                10, // 10 requests
                Refill.greedy(10, Duration.ofSeconds(30)) /// fill 10 tokens in 30 seconds
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public Bucket resolveBucket(String key) {

        return cache.computeIfAbsent(key, k -> newBucket());
    }
}