package com.backend.gym_backend.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Builder
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class Gym {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true)
    private String name;

    private String website;

    private String image;

    private String imagePublicId;

    private String location;

    private String googleMapUrl;

    @JsonManagedReference()
    @OneToOne(mappedBy = "gym")
    private Owner owner;

    @JsonManagedReference(value = "plans")
    @OneToMany(mappedBy = "gym",cascade = CascadeType.ALL)
    private List<MemberShip> memberShips = new ArrayList<>();
}
