package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.RazorpayWebhookEvent;
import com.backend.gym_backend.entity.Invoice;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.entity.OwnerPayment;
import com.backend.gym_backend.enums.Payment;
import com.backend.gym_backend.repo.InvoiceRepository;
import com.backend.gym_backend.repo.OwnerPaymentRepository;
import com.backend.gym_backend.repo.OwnerRepository;
import com.backend.gym_backend.repo.SubscriptionRepository;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class OwnerPaymentService {

    @Autowired
    private OwnerPaymentRepository ownerPaymentRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private CommonService commonService;

    private static int count = 0;

    @Retryable(retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2,random = true))
    @Transactional
    public void handlePaymentLogic(RazorpayWebhookEvent.PaymentEntity entity, String event) {
        log.info("Webhook Payment Thread: {}", Thread.currentThread().getName());
        count++;

        if (count==1){
            log.error("Simulated failure on attempt {}",count);
            throw new OptimisticLockException("Simulated retry");
        }

        log.info("Processing payment {} | attempt {}", entity.getId(), count);
        OwnerPayment ownerPayment = ownerPaymentRepository.findByRazorpayPaymentId(entity.getId()).orElseGet(
                () -> {
                    try{
                        OwnerPayment newPayment = new OwnerPayment();
                        newPayment.setRazorpayPaymentId(entity.getId());
                        newPayment.setCreatedAt(LocalDateTime.now());
                        return newPayment;
                    } catch (DataIntegrityViolationException e) {
                        log.info("Another thread inserting same row with id {}", entity.getId());
                        return ownerPaymentRepository.findByRazorpayPaymentId(entity.getId()).orElseThrow();
                    }
                }
        );

        String email = null;
        Owner owner = null;
        Invoice invoice = null;

        if (entity.getEmail() != null) {
            email = entity.getEmail();
        }

        if (email != null) {
            owner = ownerRepository.findByEmail(email)
                    .orElse(null);
            if (owner == null) {
                log.warn("Owner not found for subscription {}", entity.getId());
            }
        }

        if (entity.getInvoice_id() != null) {
            invoice = invoiceRepository.findByRazorpayInvoiceId(entity.getInvoice_id()).orElse(null);
        }
        ownerPayment.setAmount(BigDecimal.valueOf(entity.getAmount())
                .divide(BigDecimal.valueOf(100)));
        ownerPayment.setEmail(entity.getEmail());
        if (entity.getCreated_at() != null) {
            ownerPayment.setCapturedAt(commonService.convertEpochToLocalDate(Long.valueOf(entity.getCreated_at())));
        }
        ownerPayment.setUpdatedAt(LocalDateTime.now());
        if (entity.getCurrency() != null) {
            ownerPayment.setCurrency(entity.getCurrency());
        }
        Payment newStatus = switch (event) {
            case "payment.captured" -> Payment.CAPTURED;
            case "payment.failed" -> Payment.FAILED;
            case "payment.authorized" -> Payment.AUTHORIZED;
            default -> null;
        };
        if (newStatus != null &&
                (ownerPayment.getStatus() == null
                        || getPaymentStatusPriority(newStatus) >= getPaymentStatusPriority(ownerPayment.getStatus()))) {

            ownerPayment.setStatus(newStatus);

        } else {
            log.info("Skipping payment downgrade from {} to {}",
                    ownerPayment.getStatus(), newStatus);
        }

        if (entity.getContact() != null) {
            ownerPayment.setContact(entity.getContact());
        }
        ownerPayment.setOwner(owner);
        if (invoice != null) {
            ownerPayment.setInvoice(invoice);

            if (invoice.getSubscription() != null) {
                ownerPayment.setSubscription(invoice.getSubscription());
            }
        }

        if (entity.getMethod() != null) {
            ownerPayment.setMethod(entity.getMethod());
        }

        //fallback for subscription
        if (ownerPayment.getSubscription() == null && entity.getNotes() != null && entity.getNotes().get("subsId") != null) {
            com.backend.gym_backend.entity.Subscription subscription = subscriptionRepository.findByRazorpaySubscriptionId(entity.getNotes().get("subsId")).orElse(null);
            ownerPayment.setSubscription(subscription);
            log.info("linked subs in payments logic via notes");
        }
        //avoid creating subs in payment @Transactional, to prevent deadlocks

        try {
            ownerPaymentRepository.save(ownerPayment);
            log.info("payment record saved successfully of event {}", event);
        }
         catch (DataIntegrityViolationException e) {
            ownerPaymentRepository
                    .findByRazorpayPaymentId(entity.getId())
                    .orElseThrow();
            log.warn("error occurred for DataIntegrityViolationException {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelay = 300000) // 5 mins
    public void linkPaymentsToSubscriptions() {
        List<OwnerPayment> payments = ownerPaymentRepository
                .findTop50BySubscriptionIsNullOrderByCreatedAtAsc();

        for (OwnerPayment payment : payments) {

            if (payment.getInvoice() != null &&
                    payment.getInvoice().getSubscription() != null) {

                payment.setSubscription(payment.getInvoice().getSubscription());
                ownerPaymentRepository.save(payment);
            }
        }
        System.out.println("Scheduler triggered");
    }

    @Recover
    public void recoverOptimisticLock(Exception ex, RazorpayWebhookEvent.PaymentEntity entity, String event) {

        log.warn("Final failure after retries for payment {}. Reason: {}",
                entity.getId(), ex.getClass().getSimpleName());

        // optional: fetch existing record
        ownerPaymentRepository.findByRazorpayPaymentId(entity.getId())
                .ifPresent(p -> log.info("Recovered existing payment {}", p.getId()));
    }

    private int getPaymentStatusPriority(Payment status) {
        return switch (status) {
            case CREATED -> 0;
            case AUTHORIZED -> 1;
            case CAPTURED -> 2;
            case FAILED -> 3; // optional: treat as terminal
        };
    }
}
