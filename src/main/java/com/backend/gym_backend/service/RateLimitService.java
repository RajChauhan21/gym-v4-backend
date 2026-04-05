package com.backend.gym_backend.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {


    private final Cache<String,Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();

    private Bucket newBucket() {

        Bandwidth limit = Bandwidth.classic(
                100, // 10 requests --> 100 req
                Refill.greedy(100, Duration.ofMinutes(1)) /// fill 10 tokens in 30 seconds --> 1 mins
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public Bucket resolveBucket(String key) {

        return cache.get(key, k -> newBucket());
    }
}