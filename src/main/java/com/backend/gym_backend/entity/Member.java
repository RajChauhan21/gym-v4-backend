package com.backend.gym_backend.entity;

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
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private String email;

    private String phone;

    private String address;

    private LocalDate joined;

    private LocalDate startDate;

    private LocalDate expiry;

    private Integer dueAmount;

    private Integer isActive;

    private Integer sourceId;

    @JsonBackReference("mem")
    @ManyToOne
    private Owner owner;

    @JsonBackReference("pack")
    @ManyToOne
    private MemberShip memberShip;

    @JsonManagedReference("pay")
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL)
    private List<Payment> payments;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
