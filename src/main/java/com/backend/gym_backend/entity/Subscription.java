package com.backend.gym_backend.entity;

import com.backend.gym_backend.enums.SubscriptionStatus;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private Integer price;

    private LocalDate startDate;

    private LocalDate endDate;

    @Version
    private Long version;

    private LocalDate subscriptionStartDate;

    private LocalDate subscriptionEndDate;

    @Column(unique = true)
    private String razorpaySubscriptionId;

    private LocalDate nextBillingDate;

    private String email;

    private String contact;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private SubscriptionStatus status;

    @JsonBackReference("own")
    @ManyToOne
    private Owner owner;

    @JsonManagedReference("invoice")
    @OneToMany(mappedBy = "subscription")
    private List<Invoice> invoices;

    @JsonManagedReference("payment")
    @OneToMany(mappedBy = "subscription")
    private List<OwnerPayment> payments;

    @JsonBackReference("subs")
    @ManyToOne
    private Plan plan;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
