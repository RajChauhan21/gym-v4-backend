package com.backend.gym_backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class PlanFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonBackReference("pl")
    @ManyToOne
    private Plan plan;

    @JsonBackReference("feat")
    @ManyToOne
    private Feature feature;
}
