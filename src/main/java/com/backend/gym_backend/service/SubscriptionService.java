package com.backend.gym_backend.service;

import com.backend.gym_backend.enums.Status;
import com.backend.gym_backend.dto.SubscriptionResponse;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.entity.Plan;
import com.backend.gym_backend.entity.Subscription;
import com.backend.gym_backend.repo.OwnerRepository;
import com.backend.gym_backend.repo.PlanFeatureRepository;
import com.backend.gym_backend.repo.PlanRepository;
import com.backend.gym_backend.repo.SubscriptionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SubscriptionService {

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlanFeatureRepository planFeatureRepository;

    @Transactional
    public SubscriptionResponse ownerSubscribesToPlan(Integer oId, Integer pId){
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
        subscription.setName(plan.getName());
        subscription.setPrice(plan.getPrice());
        subscription.setStatus(Status.ACTIVE);
        subscription.setId(null);
        subscription.setStartDate(LocalDate.now());
        long daysToAdd = (plan.getDays() != null && !plan.getDays().isEmpty()) ? Long.parseLong(plan.getDays()) : 0L;
        subscription.setEndDate(LocalDate.now().plusDays(daysToAdd));

        Subscription save = subscriptionRepository.save(subscription);

        return SubscriptionResponse.builder()
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

    public SubscriptionResponse findById(Integer id){
        if(!planFeatureRepository.existsById(id)){
            throw new RuntimeException("Subscription id not found");
        }

        Subscription save = subscriptionRepository.findById(id).get();

       return SubscriptionResponse.builder()
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

    public List<SubscriptionResponse> findSubscriptionsOfOwner(Integer ownerId){
        List<SubscriptionResponse> subscriptionResponses = new ArrayList<>();
        List<Subscription> subscriptions = subscriptionRepository.findByOwnerId(ownerId);

        for (Subscription s : subscriptions){
            SubscriptionResponse build = SubscriptionResponse.builder()
                    .id(s.getId())
                    .price(s.getPrice())
                    .name(s.getName())
                    .startDate(s.getStartDate())
                    .endDate(s.getEndDate())
                    .status(s.getStatus())
                    .build();

            subscriptionResponses.add(build);
        }

       return subscriptionResponses;
    }

    public List<SubscriptionResponse> findSubscriptionsOfPlan(Integer planId){
        List<SubscriptionResponse> subscriptionResponses = new ArrayList<>();
        List<Subscription> subscriptions = subscriptionRepository.findByPlanId(planId);

        for (Subscription s : subscriptions){
            SubscriptionResponse build = SubscriptionResponse.builder()
                    .id(s.getId())
                    .price(s.getPrice())
                    .name(s.getName())
                    .startDate(s.getStartDate())
                    .endDate(s.getEndDate())
                    .status(s.getStatus())
                    .build();

            subscriptionResponses.add(build);
        }

       return subscriptionResponses;
    }

    @Transactional
    public String deleteById(Integer id){
        if(!planFeatureRepository.existsById(id)){
            throw new RuntimeException("Subscription id not found");
        }

        subscriptionRepository.deleteById(id);

        return "Subscription deleted successfully";
    }


    public List<SubscriptionResponse> findAllSubscriptions(){
        List<Subscription> all = subscriptionRepository.findAll();
        List<SubscriptionResponse> subscriptions = new ArrayList<>();

        for (Subscription s : all) {
            SubscriptionResponse build = SubscriptionResponse.builder()
                    .owner(s.getOwner())
                    .plan(s.getPlan())
                    .id(s.getId())
                    .endDate(s.getEndDate())
                    .startDate(s.getStartDate())
                    .name(s.getName())
                    .price(s.getPrice())
                    .status(s.getStatus())
                    .build();

            subscriptions.add(build);
        }
        return subscriptions;
    }

}
