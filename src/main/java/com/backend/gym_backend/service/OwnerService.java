package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.OwnerDetailsRequestDto;
import com.backend.gym_backend.dto.OwnerDetailsResponseDto;
import com.backend.gym_backend.entity.Gym;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.repo.GymRepository;
import com.backend.gym_backend.repo.OwnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OwnerService {

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private GymRepository gymRepository;


    public OwnerDetailsResponseDto save(OwnerDetailsRequestDto ownerDetailsRequestDto) {
        Owner owner = Owner.builder()
                .id(null)
                .phone(ownerDetailsRequestDto.getPhone())
                .email(ownerDetailsRequestDto.getEmail())
                .name(ownerDetailsRequestDto.getOwnerName())
                .build();

        Owner save = ownerRepository.save(owner);

        return OwnerDetailsResponseDto.builder()
                .ownerId(save.getId())
                .ownerName(save.getName())
                .email(save.getEmail())
                .phone(save.getPhone())
                .build();
    }

    public OwnerDetailsRequestDto update(OwnerDetailsRequestDto ownerDetailsRequestDto) {
        if (!ownerRepository.existsById(ownerDetailsRequestDto.getOwnerId())) {
            throw new RuntimeException("User not found");
        }
        Owner owner = Owner.builder()
                .id(ownerDetailsRequestDto.getOwnerId())
                .phone(ownerDetailsRequestDto.getPhone())
                .email(ownerDetailsRequestDto.getEmail())
                .name(ownerDetailsRequestDto.getOwnerName())
                .build();

        ownerRepository.save(owner);
        return ownerDetailsRequestDto;
    }

    public OwnerDetailsResponseDto findById(int id) {
        if (!ownerRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        Owner owner = ownerRepository.findById(id).get();
        OwnerDetailsResponseDto responseDto = OwnerDetailsResponseDto.builder()
                .website(owner.getGym().getWebsite())
                .email(owner.getEmail())
                .phone(owner.getPhone())
                .googleMapUrl(owner.getGym().getGoogleMapUrl())
                .ownerName(owner.getName())
                .gymName(owner.getGym().getName())
                .location(owner.getGym().getLocation())
                .ownerId(owner.getId())
                .gymId(owner.getGym().getId())
                .build();

        return responseDto;
    }
    public List<OwnerDetailsResponseDto> findAllOwners(){
        List<Owner> all = ownerRepository.findAll();
        List<OwnerDetailsResponseDto> owners = new ArrayList<>();

        for (Owner o : all){
            OwnerDetailsResponseDto responseDto = OwnerDetailsResponseDto.builder()
                    .website(o.getGym().getWebsite())
                    .email(o.getEmail())
                    .phone(o.getPhone())
                    .googleMapUrl(o.getGym().getGoogleMapUrl())
                    .ownerName(o.getName())
                    .gymName(o.getGym().getName())
                    .location(o.getGym().getLocation())
                    .ownerId(o.getId())
                    .gymId(o.getGym().getId())
                    .build();

            owners.add(responseDto);
        }

        return owners;
    }


    public String deleteById(int id) {
        if (!ownerRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        ownerRepository.deleteById(id);
        return "deleted";
    }
}
