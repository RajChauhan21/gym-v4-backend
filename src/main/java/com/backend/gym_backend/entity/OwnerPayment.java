package com.backend.gym_backend.entity;

import com.backend.gym_backend.enums.Payment;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Builder
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class OwnerPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Razorpay IDs
    @Column(unique = true)
    private String razorpayPaymentId;     // pay_xxx (UNIQUE)

//    private String razorpayInvoiceId;     // inv_xxx (nullable)
//    private String razorpaySubscriptionId; // sub_xxx

    // Relations
    @JsonBackReference("inv")
    @ManyToOne
    private Invoice invoice;

    @JsonBackReference("payments")
    @ManyToOne
    private Owner owner;

    @JsonBackReference("payment")
    @ManyToOne
    private Subscription subscription;

    // Financials
    private BigDecimal amount;       // in paise
    private String currency;      // INR

    // Status
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Payment status;       // AUTHORIZED, CAPTURED, FAILED

    // Payment details
    private String method;        // upi, card, netbanking
    private String email;
    private String contact;

    // Razorpay timestamps
//    private Long createdAtEpoch;  // from webhook
    private LocalDate capturedAt;      // when success

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
