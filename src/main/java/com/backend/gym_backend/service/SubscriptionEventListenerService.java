package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.SubscriptionActivatedEvent;
import com.backend.gym_backend.dto.SubscriptionLinkedEvent;
import com.backend.gym_backend.entity.*;
import com.backend.gym_backend.enums.Retry;
import com.backend.gym_backend.repo.InvoiceRepository;
import com.backend.gym_backend.repo.OwnerPaymentRepository;
import com.backend.gym_backend.repo.SubscriptionCancelRetryRepository;
import com.backend.gym_backend.repo.SubscriptionRepository;
import com.razorpay.RazorpayClient;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class SubscriptionEventListenerService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private OwnerPaymentRepository ownerPaymentRepository;

    @Autowired
    private SubscriptionCancelRetryRepository retryRepository;

    @Value("${razorpay.fail.simulation:false}") //default value is false
    private boolean failSimulation;

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSubscriptionActivated(SubscriptionActivatedEvent event) {
        log.info("subs cancel event triggered");
        Subscription subscription = subscriptionRepository
                .findById(event.getSubscriptionId())
                .orElseThrow();

        Owner owner = subscription.getOwner();

        List<Subscription> activeSubs =
                subscriptionRepository.findByOwnerAndStatus(owner, com.backend.gym_backend.enums.Subscription.ACTIVE)
                        .orElse(List.of());

        for (Subscription oldSub : activeSubs) {

            if (oldSub.getRazorpaySubscriptionId()
                    .equals(subscription.getRazorpaySubscriptionId())) {
                continue;
            }

            if (oldSub.getCreatedAt().isBefore(subscription.getCreatedAt())) {
                try {
                    cancelSubscriptionInRazorpay(oldSub.getRazorpaySubscriptionId());
                    markSubscriptionCancelled(oldSub);
                } catch (RuntimeException e) {
                    log.info(e.toString());
                }
            }
        }
    }

    @Async
    public void cancelSubscriptionInRazorpay(String subsId) {
        try {
            if (failSimulation){
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

    @Scheduled(fixedDelay = 60000) // every 1 min
    public void retryFailedCancellations() {
        log.info("scheduler for cancellation has been triggered");
        List<SubscriptionCancelRetry> retries =
                retryRepository.findByStatusAndNextRetryAtBefore(Retry.PENDING, LocalDateTime.now());

        if (!retries.isEmpty()){
            for (SubscriptionCancelRetry retry : retries) {
                processSingleRetry(retry);
            }
        }
    }

    @Transactional
    public void processSingleRetry(SubscriptionCancelRetry retry) {
        try {
            razorpayClient.subscriptions.cancel(retry.getRazorpaySubscriptionId());

            retry.setStatus(Retry.SUCCESS);

            // ✅ update subscription
            Subscription subscription = subscriptionRepository
                    .findByRazorpaySubscriptionId(retry.getRazorpaySubscriptionId())
                    .orElse(null);

            if (subscription != null) {
                subscription.setStatus(com.backend.gym_backend.enums.Subscription.CANCELLED);
                subscription.setUpdatedAt(LocalDateTime.now());
                subscriptionRepository.save(subscription);
            }

            log.info("subscription retry succeeded and cancelled");

        } catch (Exception ex) {

            int count = retry.getRetryCount() + 1;
            retry.setRetryCount(count);

            if (count >= 5) {
                retry.setStatus(Retry.FAILED);
            } else {
                retry.setNextRetryAt(LocalDateTime.now().plusMinutes(5 * count));
            }

            log.info("subscription failed again for id {}",retry.getRazorpaySubscriptionId());
        }
        finally {
            retryRepository.save(retry);
            log.info("Retry saved with status {}", retry.getStatus());
        }


    }

    @Transactional
    public void markSubscriptionCancelled(Subscription sub) {
        sub.setStatus(com.backend.gym_backend.enums.Subscription.CANCELLED);
        sub.setUpdatedAt(LocalDateTime.now());
        subscriptionRepository.save(sub);
    }

    //this method tries to link subscription with payment and invoice
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void linkInvoices(SubscriptionLinkedEvent event) {
        log.info("link invoice event triggered for subs id {}",event.getRazorpaySubscriptionId());
        if (event.getRazorpaySubscriptionId()==null){
            return;
        }

        Subscription subscription = subscriptionRepository
                .findByRazorpaySubscriptionId(event.getRazorpaySubscriptionId())
                .orElseThrow();

        List<Invoice> invoices =
                invoiceRepository.findByRazorpaySubscriptionIdAndSubscriptionIsNull(
                        event.getRazorpaySubscriptionId()
                );

        if (invoices.isEmpty()) return;

        for (Invoice invoice : invoices) {
            invoice.setSubscription(subscription);
            log.info("Linked invoice {} to subscription {}", invoice.getId(), subscription.getId());

            List<OwnerPayment> payments =
                    ownerPaymentRepository.findByInvoiceAndSubscriptionIsNull(invoice);

            if (!payments.isEmpty()){
                for (OwnerPayment payment : payments) {
                    if (payment.getSubscription() == null) {
                        payment.setSubscription(subscription);
                    }
                }
                ownerPaymentRepository.saveAll(payments);
            }
            invoiceRepository.save(invoice);
        }
    }
}
