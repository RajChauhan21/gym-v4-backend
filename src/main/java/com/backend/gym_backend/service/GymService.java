package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.GymDetailsRequestDto;
import com.backend.gym_backend.dto.GymDetailsResponseDto;
import com.backend.gym_backend.entity.Gym;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.repo.GymRepository;
import com.backend.gym_backend.repo.OwnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GymService {

    @Autowired
    private GymRepository gymRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    public GymDetailsResponseDto save(GymDetailsRequestDto requestDto){
        Gym gym = new Gym();
        gym.setWebsite(requestDto.getWebsite());
        gym.setName(requestDto.getName());
        gym.setLocation(requestDto.getLocation());
        gym.setGoogleMapUrl(requestDto.getGoogleMapUrl());
        gym.setId(null);
        if (ownerRepository.existsById(requestDto.getOwnerId())){
            Owner owner = ownerRepository.findById(requestDto.getOwnerId()).get();
            gym.setOwner(owner);
            owner.setGym(gym);
            ownerRepository.save(owner);
        }
        else{
            throw new RuntimeException("owner not found");
        }

        return GymDetailsResponseDto.builder()
                .id(requestDto.getGymId())
                .name(requestDto.getName())
                .googleMapUrl(requestDto.getGoogleMapUrl())
                .location(requestDto.getLocation())
                .website(requestDto.getWebsite())
                .owner(ownerRepository.findById(requestDto.getOwnerId()).get())
                .build();
    }

    public GymDetailsResponseDto update(GymDetailsRequestDto requestDto){
        Gym gym = new Gym();
        gym.setWebsite(requestDto.getWebsite());
        gym.setName(requestDto.getName());
        gym.setLocation(requestDto.getLocation());
        gym.setGoogleMapUrl(requestDto.getGoogleMapUrl());
        gym.setId(requestDto.getGymId());
        if (ownerRepository.existsById(requestDto.getOwnerId())){
            Owner owner = ownerRepository.findById(requestDto.getOwnerId()).get();
            gym.setOwner(owner);
            owner.setGym(gym);
            ownerRepository.save(owner);
        }
        else{
            throw new RuntimeException("owner not found");
        }

        return GymDetailsResponseDto.builder()
                .name(requestDto.getName())
                .id(requestDto.getGymId())
                .googleMapUrl(requestDto.getGoogleMapUrl())
                .location(requestDto.getLocation())
                .website(requestDto.getWebsite())
                .owner(ownerRepository.findById(requestDto.getOwnerId()).get())
                .build();
    }

    public GymDetailsResponseDto findById(Integer id){
        if (gymRepository.findById(id).isEmpty()){
            throw new RuntimeException("Id not found");
        }

        Gym gym = gymRepository.findById(id).get();

        return GymDetailsResponseDto.builder()
                .name(gym.getName())
                .googleMapUrl(gym.getGoogleMapUrl())
                .location(gym.getLocation())
                .website(gym.getWebsite())
                .owner(gym.getOwner())
                .build();
    }

    public String deleteById(Integer gymId){
        if (gymRepository.findById(gymId).isEmpty()){
            throw new RuntimeException("owner not found");
        }
        Owner owner = gymRepository.findById(gymId).get().getOwner();
        if (owner!=null){
            owner.setGym(null);
            ownerRepository.save(owner);
        }

        gymRepository.deleteById(gymId);
        return "deleted successfully";
    }
}
