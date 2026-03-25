package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.FeatureRequestDto;
import com.backend.gym_backend.dto.FeatureResponseDto;
import com.backend.gym_backend.entity.Feature;
import com.backend.gym_backend.repo.FeatureRepository;
import com.backend.gym_backend.repo.PlanFeatureRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FeatureService {

    @Autowired
    private FeatureRepository featureRepository;

    @Autowired
    private PlanFeatureRepository planFeatureRepository;

    @Transactional
    public FeatureResponseDto save(FeatureRequestDto requestDto){
        Feature feature = new Feature();

        feature.setId(null);
        feature.setName(requestDto.getName());
        feature.setDescription(requestDto.getDescription());

        Feature save = featureRepository.save(feature);

        return FeatureResponseDto.builder()
                .id(save.getId())
                .features(save.getFeatures())
                .name(save.getName())
                .description(save.getDescription())
                .build();
    }

    @Transactional
    public FeatureResponseDto update(FeatureRequestDto requestDto){

        Feature feature = new Feature();

        feature.setId(requestDto.getId());
        feature.setName(requestDto.getName());
        feature.setDescription(requestDto.getDescription());

        Feature save = featureRepository.save(feature);

        return FeatureResponseDto.builder()
                .id(save.getId())
                .features(save.getFeatures())
                .name(save.getName())
                .description(save.getDescription())
                .build();
    }

    public FeatureResponseDto findById(Integer id){
        if (!featureRepository.existsById(id)){
            throw new RuntimeException("Id not found");
        }
       Feature feature = featureRepository.findById(id).get();

        return FeatureResponseDto.builder()
                .id(feature.getId())
                .features(feature.getFeatures())
                .name(feature.getName())
                .description(feature.getDescription())
                .build();
    }

    public List<FeatureResponseDto> getAllFeatures(){
        List<Feature> all = featureRepository.findAll();
        List<FeatureResponseDto> features = new ArrayList<>();
        for (Feature f : all){
            FeatureResponseDto build = FeatureResponseDto.builder()
                    .id(f.getId())
                    .features(f.getFeatures())
                    .description(f.getDescription())
                    .name(f.getName())
                    .build();

            features.add(build);
        }

        return features;
    }

    @Transactional
    public String deleteById(Integer id){
        if (!featureRepository.existsById(id)){
            throw new RuntimeException("Id not found");
        }

        featureRepository.deleteById(id);

        return "Deleted Successfully";
    }
}
