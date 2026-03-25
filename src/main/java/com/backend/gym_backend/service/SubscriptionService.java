package com.backend.gym_backend.service;

import com.backend.gym_backend.Status;
import com.backend.gym_backend.dto.SubscriptionResponseDto;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.entity.Plan;
import com.backend.gym_backend.entity.Subscription;
import com.backend.gym_backend.repo.OwnerRepository;
import com.backend.gym_backend.repo.PlanRepository;
import com.backend.gym_backend.repo.SubscriptionRespository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class SubscriptionService {

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionRespository subscriptionRespository;


    public SubscriptionResponseDto ownerSubscribesToPlan(Integer oId, Integer pId){
        if (!ownerRepository.existsById(oId)){
            throw new RuntimeException("Owner Id not found");
        }

        if (!planRepository.existsById(pId)){
            throw new RuntimeException("Plan Id not found");
        }

        Plan plan = planRepository.findById(pId).get();
        Owner owner = ownerRepository.findById(oId).get();

        Subscription subscription = new Subscription();
        subscription.setPlan(plan);
        subscription.setOwner(owner);
        subscription.setPrice(plan.getPrice());
        subscription.setStatus(Status.ACTIVE);
        subscription.setId(null);
        subscription.setStartDate(LocalDate.now());
        long daysToAdd = (plan.getDays() != null && !plan.getDays().isEmpty()) ? Long.parseLong(plan.getDays()) : 0L;
        subscription.setEndDate(LocalDate.now().plusDays(daysToAdd));

        Subscription save = subscriptionRespository.save(subscription);

        return SubscriptionResponseDto.builder()
                .owner(save.getOwner())
                .plan(save.getPlan())
                .id(save.getId())
                .endDate(save.getEndDate())
                .startDate(save.getStartDate())
                .name(save.getName())
                .price(save.getPrice())
                .status(save.getStatus())
                .build();
    }
}
