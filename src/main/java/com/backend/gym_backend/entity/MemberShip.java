package com.backend.gym_backend.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
public class MemberShip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private Integer validity;

    private Integer price;

    @JsonManagedReference("pack")
    @OneToMany(mappedBy = "memberShip")
    private List<Member> members;

    @JsonBackReference("plans")
    @ManyToOne
    private Gym gym;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
