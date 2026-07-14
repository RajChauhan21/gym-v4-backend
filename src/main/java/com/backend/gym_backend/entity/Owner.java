package com.backend.gym_backend.entity;

import com.backend.gym_backend.enums.OAuthProvider;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cascade;

import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Owner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    private String phone;

    private String image;

    private String imagePublicId;

    @Enumerated(EnumType.STRING)
    private OAuthProvider provider;

    private String providerId;

    private Integer templateId;

    @JsonBackReference()
    @OneToOne(cascade = CascadeType.ALL)
    private Gym gym;

    @JsonManagedReference("own")
    @OneToMany(mappedBy = "owner", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Subscription> subscriptions;

    @JsonManagedReference("mem")
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Member> members;

    @JsonManagedReference("invoice")
    @OneToMany(mappedBy = "owner")
    private List<Invoice> invoices;

    @JsonManagedReference("payments")
    @OneToMany(mappedBy = "owner")
    private List<OwnerPayment> payments;

    @JsonManagedReference("ref")
    @OneToOne(mappedBy = "owner")
    private RefreshToken refreshToken;
}
