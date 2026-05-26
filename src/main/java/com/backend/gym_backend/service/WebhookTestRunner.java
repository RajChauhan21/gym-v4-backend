package com.backend.gym_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class WebhookTestRunner {

    @Autowired
    private RestTemplate restTemplate;

    public void sendInvoice(String subsId, String payId, String invId, String email) {
        log.info("Invoice Thread: {}", Thread.currentThread().getName());

        try {
            Thread.sleep(new Random().nextInt(30)); // ✅ add here
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String url = "https://moralistically-unregretted-brice.ngrok-free.dev/razorpay/webhook";

//        Map<String, Object> body = Map.of(
//                "event", "invoice.paid",
//                "payload", Map.of("invoice", Map.of("entity", Map.of("id", "inv_test_1")))
//        );

        String body = """
                {
                    "entity": "event",
                    "account_id": "acc_Qeet7Q4yotpNLG",
                    "event": "invoice.paid",
                    "contains": [
                        "payment",
                        "order",
                        "invoice"
                    ],
                    "payload": {
                        "payment": {
                            "entity": {
                                "id": "%s",
                                "entity": "payment",
                                "amount": 79900,
                                "currency": "INR",
                                "base_amount": 79900,
                                "status": "captured",
                                "order_id": "order_SkRrtoWixXer6n",
                                "invoice_id": "%s",
                                "international": false,
                                "method": "upi",
                                "amount_refunded": 0,
                                "amount_transferred": 0,
                                "refund_status": null,
                                "captured": true,
                                "description": "Recurring Payment via Subscription",
                                "card_id": null,
                                "bank": null,
                                "wallet": null,
                                "vpa": "testuser@razorpay",
                                "email": "%s",
                                "contact": "+918878784874",
                                "token_id": "token_SjmAhQRRZ7WZK8",
                                "notes": [],
                                "fee": 944,
                                "tax": 144,
                                "error_code": null,
                                "error_description": null,
                                "error_source": null,
                                "error_step": null,
                                "error_reason": null,
                                "acquirer_data": {
                                    "rrn": "001000100001"
                                },
                                "created_at": 1777714615,
                                "reward": null,
                                "upi": {
                                    "vpa": "testuser@razorpay",
                                    "flow": "collect"
                                }
                            }
                        },
                        "order": {
                            "entity": {
                                "id": "order_SkRrtoWixXer6n",
                                "entity": "order",
                                "amount": 79900,
                                "amount_paid": 79900,
                                "amount_due": 0,
                                "currency": "INR",
                                "receipt": null,
                                "offer_id": null,
                                "status": "paid",
                                "attempts": 1,
                                "notes": [],
                                "created_at": 1777714613,
                                "description": null,
                                "checkout": null
                            }
                        },
                        "invoice": {
                            "entity": {
                                "id": "%s",
                                "entity": "invoice",
                                "receipt": null,
                                "invoice_number": null,
                                "customer_id": null,
                                "customer_details": {
                                    "id": null,
                                    "name": null,
                                    "email": "vivek@gmail.com",
                                    "contact": "+918878784874",
                                    "gstin": null,
                                    "billing_address": null,
                                    "shipping_address": null,
                                    "customer_name": null,
                                    "customer_email": "vivek@gmail.com",
                                    "customer_contact": "+918878784874"
                                },
                                "order_id": "order_SkRrtoWixXer6n",
                                "subscription_id": "%s",
                                "payment_id": "%s",
                                "status": "paid",
                                "expire_by": null,
                                "issued_at": 1777714613,
                                "paid_at": 1777714800,
                                "cancelled_at": null,
                                "expired_at": null,
                                "sms_status": null,
                                "email_status": null,
                                "date": 1777714613,
                                "terms": null,
                                "partial_payment": false,
                                "gross_amount": 79900,
                                "tax_amount": 0,
                                "taxable_amount": 79900,
                                "amount": 79900,
                                "amount_paid": 79900,
                                "amount_due": 0,
                                "first_payment_min_amount": null,
                                "currency": "INR",
                                "currency_symbol": "₹",
                                "description": null,
                                "notes": [],
                                "comment": null,
                                "short_url": "https://rzp.io/rzp/xZXJPGdj",
                                "view_less": true,
                                "billing_start": 1785349800,
                                "billing_end": 1788028200,
                                "type": "invoice",
                                "group_taxes_discounts": false,
                                "supply_state_code": null,
                                "subscription_status": null,
                                "user_id": null,
                                "created_at": 1777714613,
                                "idempotency_key": null,
                                "reminder_status": null,
                                "ref_num": null
                            }
                        }
                    },
                    "created_at": 1777714800
                }
                """.formatted(payId, invId, email, invId, subsId, payId);

        restTemplate.postForObject(url, body, String.class);
    }

    public void sendPayment(String paymentId, String invoiceId, String email) {
        log.info("Payment Thread: {}", Thread.currentThread().getName());

        try {
            Thread.sleep(new Random().nextInt(30)); // ✅ add here
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String url = "https://moralistically-unregretted-brice.ngrok-free.dev/razorpay/webhook";
        String body = """
                {
                    "entity": "event",
                    "account_id": "acc_Qeet7Q4yotpNLG",
                    "event": "payment.captured",
                    "contains": [
                        "payment"
                    ],
                    "payload": {
                        "payment": {
                            "entity": {
                                "id": "%s",
                                "entity": "payment",
                                "amount": 79900,
                                "currency": "INR",
                                "base_amount": 79900,
                                "status": "captured",
                                "order_id": "order_SkRrtoWixXer6n",
                                "invoice_id": "%s",
                                "international": false,
                                "method": "upi",
                                "amount_refunded": 0,
                                "amount_transferred": 0,
                                "refund_status": null,
                                "captured": true,
                                "description": "Recurring Payment via Subscription",
                                "card_id": null,
                                "bank": null,
                                "wallet": null,
                                "vpa": "testuser@razorpay",
                                "email": "%s",
                                "contact": "+918878784874",
                                "token_id": "token_SjmAhQRRZ7WZK8",
                                "notes": [],
                                "fee": 944,
                                "tax": 144,
                                "error_code": null,
                                "error_description": null,
                                "error_source": null,
                                "error_step": null,
                                "error_reason": null,
                                "acquirer_data": {
                                    "rrn": "001000100001"
                                },
                                "created_at": 1777714615,
                                "reward": null,
                                "upi": {
                                    "vpa": "testuser@razorpay",
                                    "flow": "collect"
                                }
                            }
                        }
                    },
                    "created_at": 1777714800
                }
                """.formatted(paymentId, invoiceId, email);

        restTemplate.postForObject(url, body, String.class);
    }

    public void sendSubscription(String subsId, String email, String payId, String invId) {
        log.info("Subscription Thread: {}", Thread.currentThread().getName());

        try {
            Thread.sleep(new Random().nextInt(30)); // ✅ add here
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String url = "https://moralistically-unregretted-brice.ngrok-free.dev/razorpay/webhook";

        String body = """
                {
                    "entity": "event",
                    "account_id": "acc_Qeet7Q4yotpNLG",
                    "event": "subscription.activated",
                    "contains": [
                        "subscription",
                        "payment"
                    ],
                    "payload": {
                        "subscription": {
                            "entity": {
                                "id": "%s",
                                "entity": "subscription",
                                "plan_id": "plan_SeEHwq49uixRvn",
                                "customer_id": null,
                                "customer_email": "%s",
                                "customer_contact": "+919321834217",
                                "status": "active",
                                "current_start": 1776579578,
                                "current_end": 1779129000,
                                "ended_at": null,
                                "quantity": 1,
                                "notes": {
                                    "planId": "1",
                                    "ownerId": "17"
                                },
                                "charge_at": 1779129000,
                                "start_at": 1776579578,
                                "end_at": 1805394600,
                                "auth_attempts": 0,
                                "total_count": 12,
                                "paid_count": 1,
                                "customer_notify": true,
                                "created_at": 1776500906,
                                "expire_by": null,
                                "short_url": null,
                                "has_scheduled_changes": false,
                                "change_scheduled_at": null,
                                "source": "api",
                                "payment_method": "upi",
                                "offer_id": null,
                                "remaining_count": 11
                            }
                        },
                        "payment": {
                            "entity": {
                                "id": "%s",
                                "entity": "payment",
                                "amount": 79900,
                                "currency": "INR",
                                "status": "captured",
                                "order_id": "order_SetDvRO9TQxebl",
                                "invoice_id": "%s",
                                "international": false,
                                "method": "upi",
                                "amount_refunded": 0,
                                "amount_transferred": 0,
                                "refund_status": null,
                                "captured": "1",
                                "description": "Start Subscription",
                                "card_id": null,
                                "bank": null,
                                "wallet": null,
                                "vpa": "failed@okhdfcbank",
                                "email": "%s",
                                "contact": "+919321834217",
                                "customer_id": null,
                                "token_id": "token_SfFYxiQaB3js57",
                                "notes": {
                                    "dashboard": "true"
                                },
                                "fee": 826,
                                "tax": 126,
                                "error_code": null,
                                "error_description": null,
                                "acquirer_data": {
                                    "rrn": "001000100002",
                                    "upi_transaction_id": "npci_txn_id_for_SfFYxLgrwcVto1"
                                },
                                "created_at": 1776579578
                            }
                        }
                    },
                    "created_at": 1776579587
                }
                """.formatted(subsId, email, payId, invId, email);

        restTemplate.postForObject(url, body, String.class);
    }

    public void runParallelTests() {

        ExecutorService executor = Executors.newFixedThreadPool(5);

        executor.submit(() -> sendInvoice("subsid", "payId", "invId", "vivek@gmail.com"));

        for (int i = 0; i < 5; i++) {
            executor.submit(() -> sendPayment("payId", "invId", "vivek@gmail.com"));
        }
        for (int i = 0; i < 5; i++) {
            executor.submit(() -> sendSubscription("subsId", "vivek@gmail.com", "PayId", "InvId"));
        }
        executor.shutdown();
    }

    public void runParallelTesty() {
        int threadCount = 11;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // Use a local helper to wrap tasks with latch logic
        java.util.function.Consumer<Runnable> submitTask = (task) -> {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();   // I'm at the starting line
                    startLatch.await();      // Wait for the gunshot 🔫
                    task.run();
                } catch (Exception e) {
                    log.warn("Error in thread {}: {}", Thread.currentThread().getName(), e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        };

        // 1. Queue all tasks
        submitTask.accept(() -> sendInvoice("subsid", "payId", "invId", "vivek@gmail.com"));

        for (int i = 0; i < 5; i++) {
            submitTask.accept(() -> sendInvoice("subsid", "payId", "invId", "vivek@gmail.com"));
            submitTask.accept(() -> sendSubscription("subsId", "vivek@gmail.com", "PayId", "InvId"));
        }

        try {
            // 2. Wait for all 11 threads to be blocked at startLatch.await()
            readyLatch.await();

            log.info("🔥 Releasing all threads at once for true concurrency...");

            // 3. The Gunshot: All threads proceed at the same microsecond
            startLatch.countDown();

            // 4. Wait for the dust to settle
            doneLatch.await();
            log.info("✅ All parallel tasks completed.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }
    }

    public void runParallelTest() {

        int groups = 2; // number of different ID sets
        int perGroupTasks = 7; // 1 invoice + 3 payment + 3 subscription
        int threadCount = groups * perGroupTasks;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        java.util.function.Consumer<Runnable> submitTask = (task) -> {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    log.info("Thread: {}", Thread.currentThread().getName());

                    task.run();

                } catch (Exception e) {
                    log.warn("Error in thread {}: {}", Thread.currentThread().getName(), e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        };

        // 🔹 Create multiple groups
        for (int g = 1; g <= groups; g++) {

            String subId = "sub_" + g;
            String payId = "pay_" + g;
            String invId = "inv_" + g;
            String email = "";
            if (g == 1) {
                email = "vivek@gmail.com";
            } else {
                email = "yadav@gmail.com";
            }

            // 1 invoice per group
            String finalEmail = email;
            submitTask.accept(() ->
                    sendInvoice(subId, payId, invId, finalEmail)
            );

            // 5 payments + 5 subscriptions per group
            for (int i = 0; i < 5; i++) {

                submitTask.accept(() ->
                        sendPayment(payId, invId, finalEmail)
                );

                submitTask.accept(() ->
                        sendSubscription(subId, finalEmail, payId, invId)
                );
            }
        }

        try {
            readyLatch.await();

            log.info("🔥 Releasing all threads at once...");

            startLatch.countDown();

            doneLatch.await();

            log.info("✅ All parallel tasks completed.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }
    }

}
