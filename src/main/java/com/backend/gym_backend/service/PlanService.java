package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.PlanRequest;
import com.backend.gym_backend.dto.PlanResponse;
import com.backend.gym_backend.entity.Plan;
import com.backend.gym_backend.repo.PlanRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlanService {

    @Autowired
    private PlanRepository planRepository;

    @Transactional
    public PlanResponse save(PlanRequest requestDto){
        Plan plan = new Plan();
        plan.setId(null);
        plan.setName(requestDto.getName());
        plan.setDays(requestDto.getDays());
        plan.setPrice(requestDto.getPrice());
        plan.setMemberLimit(requestDto.getMemberLimit());
        plan.setFeatures(null);
        plan.setSubscriptions(null);

        Plan save = planRepository.save(plan);


        return PlanResponse.builder()
                .id(save.getId())
                .name(save.getName())
                .price(save.getPrice())
                .days(save.getDays())
                .features(save.getFeatures())
                .memberLimit(save.getMemberLimit())
                .subscriptions(save.getSubscriptions())
                .build();

    }

    @Transactional
    public PlanResponse update(PlanRequest requestDto){
        Plan plan = new Plan();
        plan.setId(requestDto.getId());
        plan.setName(requestDto.getName());
        plan.setDays(requestDto.getDays());
        plan.setPrice(requestDto.getPrice());
        plan.setMemberLimit(requestDto.getMemberLimit());
        plan.setFeatures(null);
        plan.setSubscriptions(null);

        Plan save = planRepository.save(plan);


        return PlanResponse.builder()
                .id(save.getId())
                .name(save.getName())
                .price(save.getPrice())
                .days(save.getDays())
                .features(save.getFeatures())
                .memberLimit(save.getMemberLimit())
                .subscriptions(save.getSubscriptions())
                .build();

    }


    public PlanResponse findById(Integer id){
        if (!planRepository.existsById(id)){
            throw new RuntimeException("Id not exists");
        }

        Plan plan = planRepository.findById(id).get();

        return PlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .price(plan.getPrice())
                .days(plan.getDays())
                .features(plan.getFeatures())
                .memberLimit(plan.getMemberLimit())
                .subscriptions(plan.getSubscriptions())
                .build();
    }

    public List<PlanResponse> findAllPlans(){
        List<Plan> all = planRepository.findAll();
        List<PlanResponse> plans = new ArrayList<>();
        for (Plan p : all){
            PlanResponse build = PlanResponse.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .price(p.getPrice())
                    .days(p.getDays())
                    .features(p.getFeatures())
                    .memberLimit(p.getMemberLimit())
                    .subscriptions(p.getSubscriptions())
                    .build();

            plans.add(build);
        }

        return plans;
    }

    public String deleteById(Integer id){
        if (!planRepository.existsById(id)){
            throw new RuntimeException("Id not exists");
        }

        planRepository.deleteById(id);

        return "deleted successfully";
    }
}
