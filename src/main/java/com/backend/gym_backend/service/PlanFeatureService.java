package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.PlanFeatureResponseDto;
import com.backend.gym_backend.entity.Feature;
import com.backend.gym_backend.entity.Plan;
import com.backend.gym_backend.entity.PlanFeature;
import com.backend.gym_backend.repo.FeatureRepository;
import com.backend.gym_backend.repo.PlanFeatureRepository;
import com.backend.gym_backend.repo.PlanRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PlanFeatureService {

    @Autowired
    private PlanFeatureRepository planFeatureRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private FeatureRepository featureRepository;

    @Transactional
    public PlanFeatureResponseDto addFeaturesToPlan(Integer fId, Integer pId) {
        if (!planRepository.existsById(pId)) {
            throw new RuntimeException("Plan id not found");
        }

        if (!featureRepository.existsById(fId)) {
            throw new RuntimeException("Feature id not found");
        }

        Plan plan = planRepository.findById(pId).get();
        Feature feature = featureRepository.findById(fId).get();

        PlanFeature planFeature = new PlanFeature();
        planFeature.setFeature(feature);
        planFeature.setPlan(plan);
        PlanFeature save = planFeatureRepository.save(planFeature);

        return PlanFeatureResponseDto.builder()
                .id(save.getId())
                .feature(save.getFeature())
                .plan(save.getPlan())
                .build();
    }

    @Transactional
    public PlanFeatureResponseDto updateFeaturesInPlan(Integer fPId, Integer fId, Integer pId) {

        if (!planFeatureRepository.existsById(fPId)) {
            throw new RuntimeException("Feature-Plan id not found");
        }

        if (!planFeatureRepository.existsById(pId)) {
            throw new RuntimeException("Plan id not found");
        }

        if (!featureRepository.existsById(fId)) {
            throw new RuntimeException("Feature id not found");
        }

        PlanFeature planFeature = planFeatureRepository.findById(fPId).get();

        Plan plan = planRepository.findById(pId).get();
        Feature feature = featureRepository.findById(fId).get();

        planFeature.setFeature(feature);
        planFeature.setPlan(plan);
        PlanFeature save = planFeatureRepository.save(planFeature);

        return PlanFeatureResponseDto.builder()
                .id(save.getId())
                .feature(save.getFeature())
                .plan(save.getPlan())
                .build();
    }

    public PlanFeatureResponseDto findById(Integer id) {
        if (!planFeatureRepository.existsById(id)) {
            throw new RuntimeException("Feature-Plan id not found");
        }

        PlanFeature planFeature = planFeatureRepository.findById(id).get();

        return PlanFeatureResponseDto.builder()
                .id(planFeature.getId())
                .plan(planFeature.getPlan())
                .feature(planFeature.getFeature())
                .build();
    }

    public List<PlanFeatureResponseDto> findAllPlanFeatures(){
        List<PlanFeature> all = planFeatureRepository.findAll();
        List<PlanFeatureResponseDto> planFeatures = new ArrayList<>();

        for(PlanFeature p : all){
            PlanFeatureResponseDto build = PlanFeatureResponseDto.builder()
                    .id(p.getId())
                    .plan(p.getPlan())
                    .feature(p.getFeature())
                    .build();

            planFeatures.add(build);
        }

        return planFeatures;
    }

    public String deleteById(Integer id) {
        if (!planFeatureRepository.existsById(id)) {
            throw new RuntimeException("Feature-Plan id not found");
        }

        planFeatureRepository.deleteById(id);

        return "deleted Successfully";
    }
}

