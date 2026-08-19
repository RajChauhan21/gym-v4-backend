package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.GymDetailsRequest;
import com.backend.gym_backend.dto.GymDetailsResponse;
import com.backend.gym_backend.entity.Gym;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.enums.SubscriptionStatus;
import com.backend.gym_backend.repo.GymRepository;
import com.backend.gym_backend.repo.OwnerRepository;
import com.backend.gym_backend.repo.SubscriptionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GymService {

    @Autowired
    private GymRepository gymRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private CommonService commonService;

    @Transactional
    public GymDetailsResponse save(GymDetailsRequest requestDto) {
        if (commonService.checkSubscriptionOfOwner(requestDto.getOwnerId())==null){
            throw new RuntimeException("100");
        }

        Gym gym;

        // -------- CREATE or UPDATE --------
        if (requestDto.getGymId() == null) {

            // CREATE
            if (gymRepository.existsByName(requestDto.getGymName())) {
                throw new RuntimeException("Gym name already exists");
            }

            gym = new Gym();

        } else {

            // UPDATE
            gym = gymRepository.findById(requestDto.getGymId())
                    .orElseThrow(() -> new RuntimeException("Gym not found"));

            if (gymRepository.existsByNameAndIdNot(requestDto.getGymName(), requestDto.getGymId())) {
                throw new RuntimeException("Gym name already exists");
            }
        }

        // -------- SET GYM DATA --------
        gym.setName(requestDto.getGymName());
        gym.setWebsite(requestDto.getWebsite());
        gym.setLocation(requestDto.getLocation());
        gym.setGoogleMapUrl(requestDto.getGoogleMapUrl());
        if (gym.getCreatedAt()==null){
            gym.setCreatedAt(LocalDateTime.now());
        }
        gym.setUpdatedAt(LocalDateTime.now());

        Owner owner = ownerRepository.findById(requestDto.getOwnerId()).orElse(null);

//        Gym savedGym = gymRepository.saveAndFlush(gym);
        owner.setGym(gym);
        gym.setOwner(owner);

       Owner savedOwner =  ownerRepository.saveAndFlush(owner); //this will save gym entity automatically

        return GymDetailsResponse.builder()
                .gymId(savedOwner.getGym().getId())
                .gymName(savedOwner.getGym().getName())
                .googleMapUrl(savedOwner.getGym().getGoogleMapUrl())
                .location(savedOwner.getGym().getLocation())
                .website(savedOwner.getGym().getWebsite())
                .ownerName(savedOwner.getGym().getOwner() != null ? savedOwner.getGym().getOwner().getName() : null)
                .email(savedOwner.getGym().getOwner() != null ? savedOwner.getGym().getOwner().getEmail() : null)
                .number(savedOwner.getGym().getOwner() != null ? savedOwner.getGym().getOwner().getPhone() : null)
                .ownerId(savedOwner.getId() != null ? savedOwner.getId() : null)
                .gymImage(savedOwner.getGym().getImage())
                .ownerImage(savedOwner != null ? savedOwner.getImage() : null)
                .build();
    }

    @Transactional
    public GymDetailsResponse update(GymDetailsRequest requestDto) throws Exception {
        if (!gymRepository.existsById(requestDto.getGymId())) {
            throw new RuntimeException("gym id not found");
        }
        Owner newOwner = null;
        Gym gym = new Gym();
        gym.setWebsite(requestDto.getWebsite());
        gym.setName(requestDto.getGymName());
        gym.setLocation(requestDto.getLocation());
        gym.setGoogleMapUrl(requestDto.getGoogleMapUrl());
        gym.setId(requestDto.getGymId());
        if (requestDto.getOwnerId() != null && ownerRepository.existsById(requestDto.getOwnerId())) {
            Owner owner = ownerRepository.findById(requestDto.getOwnerId()).get();
            gym.setOwner(owner);
            owner.setGym(gym);
            newOwner = ownerRepository.save(owner);
        }

        Gym save = newOwner.getGym();
        return GymDetailsResponse.builder()
                .gymId(save.getId())
                .gymName(save.getName())
                .googleMapUrl(save.getGoogleMapUrl())
                .location(save.getLocation())
                .website(save.getWebsite())
                .ownerName(save.getOwner().getName())
                .email(save.getOwner().getEmail())
                .number(save.getOwner().getPhone())
                .ownerId(save.getOwner().getId())
                .gymImage(save.getImage())
                .ownerImage(newOwner.getImage())
                .build();
    }

    public GymDetailsResponse findById(Integer id) {
        if (gymRepository.findById(id).isEmpty()) {
            throw new RuntimeException("Id not found");
        }

        Gym gym = gymRepository.findById(id).get();

        return GymDetailsResponse.builder()
                .gymId(gym.getId())
                .gymName(gym.getName())
                .googleMapUrl(gym.getGoogleMapUrl())
                .location(gym.getLocation())
                .website(gym.getWebsite())
                .ownerName(gym.getOwner().getName())
                .email(gym.getOwner().getEmail())
                .number(gym.getOwner().getPhone())
                .ownerId(gym.getOwner().getId())
                .gymImage(gym.getImage())
                .ownerImage(gym.getOwner().getImage())
                .build();
    }

    public List<GymDetailsResponse> findAllGyms() {
        List<Gym> all = gymRepository.findAll();
        List<GymDetailsResponse> gyms = new ArrayList<>();

        for (Gym gym : all) {
            GymDetailsResponse build = GymDetailsResponse.builder()
                    .gymId(gym.getId())
                    .gymName(gym.getName())
                    .googleMapUrl(gym.getGoogleMapUrl())
                    .location(gym.getLocation())
                    .website(gym.getWebsite())
                    .ownerName(gym.getOwner().getName())
                    .email(gym.getOwner().getEmail())
                    .number(gym.getOwner().getPhone())
                    .ownerId(gym.getOwner().getId())
                    .gymImage(gym.getImage())
                    .ownerImage(gym.getOwner().getImage())
                    .build();

            gyms.add(build);
        }

        return gyms;
    }

    @Transactional
    public String deleteById(Integer gymId) {
        if (gymRepository.findById(gymId).isEmpty()) {
            throw new RuntimeException("gym not found");
        }
        Owner owner = gymRepository.findById(gymId).get().getOwner();
        if (owner != null) {
            owner.setGym(null);
            ownerRepository.save(owner);
        }

        gymRepository.deleteById(gymId);
        gymRepository.flush();
        return "deleted successfully";
    }
}
