package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.InvoiceCreatedEvent;
import com.backend.gym_backend.entity.Invoice;
import com.backend.gym_backend.entity.Subscription;
import com.backend.gym_backend.repo.InvoiceRepository;
import com.backend.gym_backend.repo.OwnerPaymentRepository;
import com.backend.gym_backend.repo.SubscriptionRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class InvoiceEventListenerService {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private OwnerPaymentRepository ownerPaymentRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Async
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInvoiceLinking(InvoiceCreatedEvent event) {

        log.info("Invoice linking started for invoiceId {}", event.getInvoiceId());

        Invoice invoice = invoiceRepository
                .findById(event.getInvoiceId())
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if (invoice == null) {
            log.info("invoice not found");
            return;
        }

        // 🔹 Step 1: Find payment using Razorpay payment_id
        String paymentId = event.getPaymentId();

        if (paymentId == null) {
            log.warn("No paymentId found for invoice {}", invoice.getId());
            return;
        }
        Subscription subscription;

        if (!event.getSubscriptionId().isEmpty()) {
            subscription = subscriptionRepository.findByRazorpaySubscriptionId(event.getSubscriptionId()).orElse(null);
        } else {
            subscription = null;
        }

        ownerPaymentRepository.findByRazorpayPaymentId(paymentId)
                .ifPresentOrElse(payment -> {

                    // 🔹 Step 2: Link invoice to payment
                    if (payment.getInvoice() == null) {
                        payment.setInvoice(invoice);
                    }

                    // 🔹 Step 3: Link subscription via invoice (if available)
                    if (payment.getSubscription() == null && invoice.getSubscription() != null) {
                        payment.setSubscription(invoice.getSubscription());
                        log.info("Linked subscription {} to payment {} with invoice support",
                                invoice.getSubscription().getId(), payment.getId());
                    }

                    //handled payment linking with subscription
                    if (payment.getSubscription() == null && subscription != null) {
                        log.info("linked subscription with payment without invoice support");
                        payment.setSubscription(subscription);
                    }

                    // 🔹 Step 4: Save payment (IMPORTANT - no cascade)
                    ownerPaymentRepository.save(payment);

                    log.info("Linked payment {} with invoice {}", payment.getId(), invoice.getId());

                }, () -> {
                    log.warn("Payment not found for paymentId {}", paymentId);
                });

        // 🔹 Step 5: Fallback - if subscription missing in invoice, try fetch again
        if (invoice.getSubscription() == null && subscription != null) {

            invoice.setSubscription(subscription);
            invoiceRepository.save(invoice);

            log.info("Fixed missing subscription {} for invoice {}",
                    subscription.getId(), invoice.getId());
        }

        log.info("Invoice linking completed for invoiceId {}", invoice.getId());
    }
}
