package com.backend.gym_backend.service;

import com.backend.gym_backend.entity.Subscription;
import com.backend.gym_backend.entity.SubscriptionCancelRetry;
import com.backend.gym_backend.enums.Retry;
import com.backend.gym_backend.enums.SubscriptionStatus;
import com.backend.gym_backend.repo.SubscriptionCancelRetryRepository;
import com.backend.gym_backend.repo.SubscriptionRepository;
import com.razorpay.RazorpayClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class SubscriptionCancellationService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionCancelRetryRepository retryRepository;

    @Value("${razorpay.fail.simulation:false}") //default value is false
    private boolean failSimulation;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSubscriptionCancelled(Subscription sub) {
        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.saveAndFlush(sub);
    }

    @Scheduled(fixedDelay = 60000) // every 1 min
    public void retryFailedCancellations() {
        log.info("scheduler for cancellation has been triggered");
        List<SubscriptionCancelRetry> retries =
                retryRepository.findByStatusAndNextRetryAtBefore(Retry.PENDING, LocalDateTime.now());

        if (!retries.isEmpty()) {
            for (SubscriptionCancelRetry retry : retries) {
                subscriptionService.processSingleRetry(retry);
            }
        }
    }

    @Async
    public void cancelSubscriptionInRazorpay(String subsId) {
        try {
            if (failSimulation) {
                throw new RuntimeException("Razorpay simulated exception");
            }
            razorpayClient.subscriptions.cancel(subsId);
        } catch (Exception e) {
            log.error("Razorpay External Api Cancel Failed", e);
            SubscriptionCancelRetry retry = new SubscriptionCancelRetry();
            retry.setStatus(Retry.PENDING);
            retry.setRazorpaySubscriptionId(subsId);
            retry.setRetryCount(0);
            retry.setCreatedAt(LocalDateTime.now());
            retry.setNextRetryAt(LocalDateTime.now().plusMinutes(5));
            retryRepository.save(retry);
        }
    }


}
