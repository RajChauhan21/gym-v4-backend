package com.backend.gym_backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Builder
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Razorpay IDs
    @Column(unique = true)
    private String razorpayInvoiceId;     // inv_xxx (UNIQUE)
//    private String razorpaySubscriptionId; // sub_xxx

    // Relations
    @JsonManagedReference("inv")
    @OneToMany(mappedBy = "invoice")
    private List<OwnerPayment> payments;

    @JsonBackReference("invoice")
    @ManyToOne
    private Owner owner;

    @JsonBackReference("invoice")
    @ManyToOne
    private Subscription subscription;

    // Financials
    private Integer amount;       // in paise
    private Integer amountPaid;   // in paise
    private String currency;      // INR

    // Status
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private com.backend.gym_backend.enums.Invoice status;        // CREATED, PAID, FAILED

    // Billing cycle
    private Long billingStart;    // epoch (nullable initially)
    private Long billingEnd;      // ⭐ next billing date

    // Razorpay timestamps
    private Long issuedAt;        // created_at / issued_at
    private Long paidAt;          // paid_at

    // Invoice access
    private String invoiceUrl;    // short_url (for download)

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
