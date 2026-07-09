package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.SubscriptionActivatedEvent;
import com.backend.gym_backend.dto.SubscriptionLinkedEvent;
import com.backend.gym_backend.entity.*;
import com.backend.gym_backend.enums.Retry;
import com.backend.gym_backend.enums.SubscriptionStatus;
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
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
public class SubscriptionEventListenerService {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionCancellationService subscriptionCancellationService;

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private OwnerPaymentRepository ownerPaymentRepository;

    @Autowired
    private SubscriptionCancelRetryRepository retryRepository;

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSubscriptionActivated(SubscriptionActivatedEvent event) {
        log.info("subs cancel event triggered");
        Subscription subscription = subscriptionRepository
                .findById(event.getSubscriptionId())
                .orElseThrow();

        Owner owner = subscription.getOwner();

        List<Subscription> activeSubs =
                subscriptionRepository.findByOwnerAndStatusIn(owner, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PARTIALLY_ACTIVE))
                        .orElse(List.of());

        for (Subscription oldSub : activeSubs) {

            if (oldSub.getRazorpaySubscriptionId()
                    .equals(subscription.getRazorpaySubscriptionId())) {
                continue;
            }

            if (oldSub.getCreatedAt().isBefore(subscription.getCreatedAt())) {
                try {
                    subscriptionCancellationService.cancelSubscriptionInRazorpay(oldSub.getRazorpaySubscriptionId());
                    subscriptionCancellationService.markSubscriptionCancelled(oldSub);
                } catch (RuntimeException e) {
                    log.info(e.toString());
                }
            }
        }
    }

    //this method tries to link subscription with payment and invoice
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void linkInvoices(SubscriptionLinkedEvent event) {
        log.info("link invoice event triggered for subs id {}", event.getRazorpaySubscriptionId());
        if (event.getRazorpaySubscriptionId() == null) {
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

            if (!payments.isEmpty()) {
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
