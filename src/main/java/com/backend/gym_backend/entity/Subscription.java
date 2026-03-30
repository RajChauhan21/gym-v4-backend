package com.backend.gym_backend.entity;

import com.backend.gym_backend.enums.Status;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

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

    @Enumerated(EnumType.STRING)
    private Status status;

    @JsonBackReference("own")
    @ManyToOne
    private Owner owner;

    @JsonBackReference("subs")
    @ManyToOne
    private Plan plan;

}
