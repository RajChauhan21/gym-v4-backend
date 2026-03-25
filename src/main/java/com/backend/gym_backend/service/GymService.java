package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.GymDetailsRequestDto;
import com.backend.gym_backend.dto.GymDetailsResponseDto;
import com.backend.gym_backend.entity.Gym;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.repo.GymRepository;
import com.backend.gym_backend.repo.OwnerRepository;
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

    public GymDetailsResponseDto save(GymDetailsRequestDto requestDto) {
        Gym gym = new Gym();
        gym.setWebsite(requestDto.getWebsite());
        gym.setName(requestDto.getName());
        gym.setLocation(requestDto.getLocation());
        gym.setGoogleMapUrl(requestDto.getGoogleMapUrl());
        gym.setId(null);
        if (requestDto.getOwnerId()!=null && ownerRepository.existsById(requestDto.getOwnerId())) {
            Owner owner = ownerRepository.findById(requestDto.getOwnerId()).get();
            gym.setOwner(owner);
            owner.setGym(gym);
            ownerRepository.save(owner);
        }

        Gym save = gymRepository.save(gym);


        return GymDetailsResponseDto.builder()
                .id(save.getId())
                .name(save.getName())
                .googleMapUrl(save.getGoogleMapUrl())
                .location(save.getLocation())
                .website(save.getWebsite())
                .owner(save.getOwner())
                .build();
    }

    public GymDetailsResponseDto update(GymDetailsRequestDto requestDto) throws Exception {
        if (!gymRepository.existsById(requestDto.getGymId())){
            throw new RuntimeException("gym id not found");
        }
        Gym gym = new Gym();
        gym.setWebsite(requestDto.getWebsite());
        gym.setName(requestDto.getName());
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


        return GymDetailsResponseDto.builder()
                .name(save.getName())
                .id(save.getId())
                .googleMapUrl(save.getGoogleMapUrl())
                .location(save.getLocation())
                .website(save.getWebsite())
                .owner(save.getOwner())
                .build();
    }

    public GymDetailsResponseDto findById(Integer id) {
        if (gymRepository.findById(id).isEmpty()) {
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

    public List<GymDetailsResponseDto> findAllGyms(){
        List<Gym> all = gymRepository.findAll();
        List<GymDetailsResponseDto> gyms = new ArrayList<>();

        for(Gym g : all){
            GymDetailsResponseDto build = GymDetailsResponseDto.builder()
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

    public String deleteById(Integer gymId) {
        if (gymRepository.findById(gymId).isEmpty()) {
            throw new RuntimeException("owner not found");
        }
        Owner owner = gymRepository.findById(gymId).get().getOwner();
        if (owner != null) {
            owner.setGym(null);
            ownerRepository.save(owner);
        }

        gymRepository.deleteById(gymId);
        return "deleted successfully";
    }
}
