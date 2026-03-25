package com.backend.gym_backend.entity;

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

    private String email;

    private String phone;

    @JsonBackReference()
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    private Gym gym;

    @JsonManagedReference("own")
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<Subscription> subscription;
}
