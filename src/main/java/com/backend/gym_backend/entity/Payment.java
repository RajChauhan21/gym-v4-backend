package com.backend.gym_backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer amountPaid;

    private Integer amountDue;

    private LocalDate date;

    private String method;

    @JsonBackReference("pay")
    @ManyToOne
    private Member member;
}
