package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.GymDetailsRequest;
import com.backend.gym_backend.dto.GymDetailsResponse;
import com.backend.gym_backend.entity.Gym;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.repo.GymRepository;
import com.backend.gym_backend.repo.OwnerRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GymService {

    @Autowired
    private GymRepository gymRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Transactional
    public GymDetailsResponse save(GymDetailsRequest requestDto) {
        Gym gym = new Gym();
        gym.setWebsite(requestDto.getWebsite());
        gym.setName(requestDto.getGymName());
        gym.setLocation(requestDto.getLocation());
        gym.setGoogleMapUrl(requestDto.getGoogleMapUrl());
        gym.setId(null);
        if (requestDto.getOwnerId()!=null && ownerRepository.existsById(requestDto.getOwnerId())) {
            Owner owner = ownerRepository.findById(requestDto.getOwnerId()).get();
            owner.setName(requestDto.getOwnerName());
            owner.setPhone(requestDto.getNumber());

            if (!requestDto.getEmail().equals(owner.getEmail()) && ownerRepository.findByEmail(requestDto.getEmail()).isPresent()){
                throw new RuntimeException("Email already exists");
            }
            gym.setOwner(owner);
            owner.setGym(gym);
            ownerRepository.save(owner);
        }

        Gym save = gymRepository.save(gym);


        return GymDetailsResponse.builder()
                .id(save.getId())
                .name(save.getName())
                .googleMapUrl(save.getGoogleMapUrl())
                .location(save.getLocation())
                .website(save.getWebsite())
                .owner(save.getOwner())
                .build();
    }

    @Transactional
    public GymDetailsResponse update(GymDetailsRequest requestDto) throws Exception {
        if (!gymRepository.existsById(requestDto.getGymId())){
            throw new RuntimeException("gym id not found");
        }
        Gym gym = new Gym();
        gym.setWebsite(requestDto.getWebsite());
        gym.setName(requestDto.getGymName());
        gym.setLocation(requestDto.getLocation());
        gym.setGoogleMapUrl(requestDto.getGoogleMapUrl());
        gym.setId(requestDto.getGymId());
        if (requestDto.getOwnerId()!=null && ownerRepository.existsById(requestDto.getOwnerId())) {
            Owner owner = ownerRepository.findById(requestDto.getOwnerId()).get();
            gym.setOwner(owner);
            owner.setGym(gym);
            ownerRepository.save(owner);
        }

        Gym save = gymRepository.save(gym);


        return GymDetailsResponse.builder()
                .name(save.getName())
                .id(save.getId())
                .googleMapUrl(save.getGoogleMapUrl())
                .location(save.getLocation())
                .website(save.getWebsite())
                .owner(save.getOwner())
                .build();
    }

    public GymDetailsResponse findById(Integer id) {
        if (gymRepository.findById(id).isEmpty()) {
            throw new RuntimeException("Id not found");
        }

        Gym gym = gymRepository.findById(id).get();

        return GymDetailsResponse.builder()
                .name(gym.getName())
                .googleMapUrl(gym.getGoogleMapUrl())
                .location(gym.getLocation())
                .website(gym.getWebsite())
                .owner(gym.getOwner())
                .build();
    }

    public List<GymDetailsResponse> findAllGyms(){
        List<Gym> all = gymRepository.findAll();
        List<GymDetailsResponse> gyms = new ArrayList<>();

        for(Gym g : all){
            GymDetailsResponse build = GymDetailsResponse.builder()
                    .name(g.getName())
                    .id(g.getId())
                    .googleMapUrl(g.getGoogleMapUrl())
                    .location(g.getLocation())
                    .website(g.getWebsite())
                    .owner(g.getOwner())
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
