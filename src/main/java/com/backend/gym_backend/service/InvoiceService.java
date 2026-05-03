package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.InvoiceCreatedEvent;
import com.backend.gym_backend.dto.RazorpayWebhookEvent;
import com.backend.gym_backend.entity.Invoice;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.repo.InvoiceRepository;
import com.backend.gym_backend.repo.OwnerPaymentRepository;
import com.backend.gym_backend.repo.OwnerRepository;
import com.backend.gym_backend.repo.SubscriptionRepository;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Slf4j
@Service
public class InvoiceService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private CommonService commonService;

    @Autowired
    private OwnerPaymentRepository ownerPaymentRepository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Retryable(value = {OptimisticLockException.class, OptimisticLockingFailureException.class}, maxAttempts = 5, backoff = @Backoff(multiplier = 2, delay = 100))
    @Transactional
    public void handleInvoiceLogic(RazorpayWebhookEvent.InvoiceEntity entity, String event) {
        log.info("Webhook Invoice Thread: {}", Thread.currentThread().getName());
        Invoice invoice = invoiceRepository.findByRazorpayInvoiceId(entity.getId()).orElseGet(
                () -> {
                    try{
                        Invoice newInvoice = new Invoice();
                        newInvoice.setRazorpayInvoiceId(entity.getId());
                        newInvoice.setCreatedAt(LocalDateTime.now());
                        return newInvoice;
                    } catch (DataIntegrityViolationException e) {
                        log.info("Another thread inserting same row with id {}", entity.getId());
                        return invoiceRepository.findByRazorpayInvoiceId(entity.getId()).orElseThrow();
                    }
                }
        );

        if (invoice.getStatus() != null && com.backend.gym_backend.enums.Invoice.PAID.equals(invoice.getStatus()) && "invoice.paid".equals(event)) {
            log.info("Skipping subscription update: Already paid.");
            return;
        }
        String email = null;

        if (entity.getCustomer_details() != null) {
            email = entity.getCustomer_details().getEmail();
        }

        if (entity.getSubscription_id()!=null){
            invoice.setRazorpaySubscriptionId(entity.getSubscription_id());
        }

        if (entity.getPayment_id()!=null){
            invoice.setRazorpayPaymentId(entity.getPayment_id());
        }

        Owner owner = null;
        if (email != null) {
            owner = ownerRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Owner not found"));
            if (owner == null) {
                log.warn("Owner not found for subscription {}", entity.getId());
            }
        }

        com.backend.gym_backend.entity.Subscription subscription = null;

        if (entity.getSubscription_id() != null) {
            subscription = subscriptionRepository
                    .findByRazorpaySubscriptionId(entity.getSubscription_id())
                    .orElse(null);
        } else {
            log.warn("No subscription_id found for invoice {}", entity.getId());
        }
        invoice.setCurrency(entity.getCurrency());
        BigDecimal amount = BigDecimal.valueOf(entity.getAmount())
                .divide(BigDecimal.valueOf(100));

        invoice.setAmount(amount);
        invoice.setInvoiceUrl(entity.getShort_url());
        BigDecimal paidAmount = entity.getAmount_paid() != null
                ? BigDecimal.valueOf(entity.getAmount_paid()).divide(BigDecimal.valueOf(100))
                : amount;

        invoice.setAmountPaid(paidAmount);
        invoice.setOwner(owner);
        if (entity.getBilling_end() != null) {
            invoice.setBillingEnd(commonService.convertEpochToLocalDate(Long.parseLong(entity.getBilling_end())));
        }
        if (entity.getBilling_start() != null) {
            invoice.setBillingStart(commonService.convertEpochToLocalDate(Long.valueOf(entity.getBilling_start())));
        }
        switch (event) {
            case "invoice.paid" -> invoice.setStatus(com.backend.gym_backend.enums.Invoice.PAID);
            case "invoice.failed" -> invoice.setStatus(com.backend.gym_backend.enums.Invoice.FAILED);
        }

        if (entity.getIssued_at() != null) {
            invoice.setIssuedAt(commonService.convertEpochToLocalDate(entity.getIssued_at()));
        }
        if (entity.getPaid_at() != null) {
            invoice.setPaidAt(commonService.convertEpochToLocalDate(entity.getPaid_at()));
        }
        invoice.setUpdatedAt(LocalDateTime.now());
        invoice.setSubscription(subscription);
        try {
            invoiceRepository.saveAndFlush(invoice);
            log.info("invoice record saved successfully of event {}", event);
        } catch (DataIntegrityViolationException e) {
            log.warn("Concurrent update blocked for invoice: {}", invoice.getId());
        }

        applicationEventPublisher.publishEvent(
                new InvoiceCreatedEvent(
                        invoice.getId(),
                        entity.getPayment_id(),
                        entity.getSubscription_id()
                )
        );
    }
}
