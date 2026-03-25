package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.PlanRequestDto;
import com.backend.gym_backend.dto.PlanResponseDto;
import com.backend.gym_backend.entity.Plan;
import com.backend.gym_backend.repo.PlanRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlanService {

    @Autowired
    private PlanRepository planRepository;

    @Transactional
    public PlanResponseDto save(PlanRequestDto requestDto){
        Plan plan = new Plan();
        plan.setId(null);
        plan.setName(requestDto.getName());
        plan.setDays(requestDto.getDays());
        plan.setPrice(requestDto.getPrice());
        plan.setMemberLimit(requestDto.getMemberLimit());
        plan.setFeatures(null);
        plan.setSubscriptions(null);

        Plan save = planRepository.save(plan);


        return PlanResponseDto.builder()
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
    public PlanResponseDto update(PlanRequestDto requestDto){
        Plan plan = new Plan();
        plan.setId(requestDto.getId());
        plan.setName(requestDto.getName());
        plan.setDays(requestDto.getDays());
        plan.setPrice(requestDto.getPrice());
        plan.setMemberLimit(requestDto.getMemberLimit());
        plan.setFeatures(null);
        plan.setSubscriptions(null);

        Plan save = planRepository.save(plan);


        return PlanResponseDto.builder()
                .id(save.getId())
                .name(save.getName())
                .price(save.getPrice())
                .days(save.getDays())
                .features(save.getFeatures())
                .memberLimit(save.getMemberLimit())
                .subscriptions(save.getSubscriptions())
                .build();

    }


    public PlanResponseDto findById(Integer id){
        if (!planRepository.existsById(id)){
            throw new RuntimeException("Id not exists");
        }

        Plan plan = planRepository.findById(id).get();

        return PlanResponseDto.builder()
                .id(plan.getId())
                .name(plan.getName())
                .price(plan.getPrice())
                .days(plan.getDays())
                .features(plan.getFeatures())
                .memberLimit(plan.getMemberLimit())
                .subscriptions(plan.getSubscriptions())
                .build();
    }

    public String deleteById(Integer id){
        if (!planRepository.existsById(id)){
            throw new RuntimeException("Id not exists");
        }

        planRepository.deleteById(id);

        return "deleted successfully";
    }
}
