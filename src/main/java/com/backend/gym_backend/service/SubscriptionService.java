package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.RazorpayWebhookEvent;
import com.backend.gym_backend.dto.SubscriptionActivatedEvent;
import com.backend.gym_backend.dto.SubscriptionLinkedEvent;
import com.backend.gym_backend.dto.SubscriptionResponse;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.entity.Plan;
import com.backend.gym_backend.enums.Subscription;
import com.backend.gym_backend.repo.OwnerRepository;
import com.backend.gym_backend.repo.PlanFeatureRepository;
import com.backend.gym_backend.repo.PlanRepository;
import com.backend.gym_backend.repo.SubscriptionRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
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

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public static LocalDate convertEpochToLocalDate(Long epochSeconds) {
        if (epochSeconds == null) {
            return null;
        }
        return Instant.ofEpochSecond(epochSeconds)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    @Transactional
    public SubscriptionResponse ownerSubscribesToPlan(Integer oId, Integer pId, String razorPaySubsId, String status) {
        if (!ownerRepository.existsById(oId)) {
            throw new RuntimeException("Owner Id not found");
        }

        if (!planRepository.existsById(pId)) {
            throw new RuntimeException("Plan Id not found");
        }

        Plan plan = planRepository.findById(pId).get();
        Owner owner = ownerRepository.findById(oId).get();

        com.backend.gym_backend.entity.Subscription subscription = new com.backend.gym_backend.entity.Subscription();
        subscription.setPlan(plan);
        subscription.setOwner(owner);
        subscription.setName(plan.getName());
        subscription.setPrice(plan.getPrice());
        subscription.setStatus(status.equals("ACTIVE") ? Subscription.ACTIVE : Subscription.CREATED);
        subscription.setRazorpaySubscriptionId(razorPaySubsId);
        subscription.setId(null);
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setStartDate(LocalDate.now());
        long daysToAdd = (plan.getDays() != null && !plan.getDays().isEmpty()) ? Long.parseLong(plan.getDays()) : 0L;
        subscription.setEndDate(LocalDate.now().plusDays(daysToAdd));

        com.backend.gym_backend.entity.Subscription save = subscriptionRepository.save(subscription);

        return SubscriptionResponse.builder()
                .owner(save.getOwner())
                .plan(save.getPlan())
                .id(save.getId())
                .endDate(save.getEndDate())
                .startDate(save.getStartDate())
                .name(save.getName())
                .price(save.getPrice())
                .subscription(save.getStatus())
                .build();
    }

    public SubscriptionResponse findById(Integer id) {
        if (!planFeatureRepository.existsById(id)) {
            throw new RuntimeException("Subscription id not found");
        }

        com.backend.gym_backend.entity.Subscription save = subscriptionRepository.findById(id).get();

        return SubscriptionResponse.builder()
                .owner(save.getOwner())
                .plan(save.getPlan())
                .id(save.getId())
                .endDate(save.getEndDate())
                .startDate(save.getStartDate())
                .name(save.getName())
                .price(save.getPrice())
                .subscription(save.getStatus())
                .build();

    }

    public List<SubscriptionResponse> findSubscriptionsOfOwner(Integer ownerId) {
        List<SubscriptionResponse> subscriptionResponses = new ArrayList<>();
        List<com.backend.gym_backend.entity.Subscription> subscriptions = subscriptionRepository.findByOwnerId(ownerId);

        for (com.backend.gym_backend.entity.Subscription s : subscriptions) {
            SubscriptionResponse build = SubscriptionResponse.builder()
                    .id(s.getId())
                    .price(s.getPrice())
                    .name(s.getName())
                    .startDate(s.getStartDate())
                    .endDate(s.getEndDate())
                    .subscription(s.getStatus())
                    .build();

            subscriptionResponses.add(build);
        }

        return subscriptionResponses;
    }

    public List<SubscriptionResponse> findSubscriptionsOfPlan(Integer planId) {
        List<SubscriptionResponse> subscriptionResponses = new ArrayList<>();
        List<com.backend.gym_backend.entity.Subscription> subscriptions = subscriptionRepository.findByPlanId(planId);

        for (com.backend.gym_backend.entity.Subscription s : subscriptions) {
            SubscriptionResponse build = SubscriptionResponse.builder()
                    .id(s.getId())
                    .price(s.getPrice())
                    .name(s.getName())
                    .startDate(s.getStartDate())
                    .endDate(s.getEndDate())
                    .subscription(s.getStatus())
                    .build();

            subscriptionResponses.add(build);
        }

        return subscriptionResponses;
    }

    @Transactional
    public String deleteById(Integer id) {
        if (!planFeatureRepository.existsById(id)) {
            throw new RuntimeException("Subscription id not found");
        }

        subscriptionRepository.deleteById(id);

        return "Subscription deleted successfully";
    }


    public List<SubscriptionResponse> findAllSubscriptions() {
        List<com.backend.gym_backend.entity.Subscription> all = subscriptionRepository.findAll();
        List<SubscriptionResponse> subscriptions = new ArrayList<>();

        for (com.backend.gym_backend.entity.Subscription s : all) {
            SubscriptionResponse build = SubscriptionResponse.builder()
                    .owner(s.getOwner())
                    .plan(s.getPlan())
                    .id(s.getId())
                    .endDate(s.getEndDate())
                    .startDate(s.getStartDate())
                    .name(s.getName())
                    .price(s.getPrice())
                    .subscription(s.getStatus())
                    .build();

            subscriptions.add(build);
        }
        return subscriptions;
    }

    private Subscription mapSubscriptionStatus(String event) {
        return switch (event) {
            case "subscription.authenticated" -> Subscription.AUTHENTICATED;
            case "subscription.activated", "subscription.charged" -> Subscription.ACTIVE;
            case "subscription.cancelled" -> Subscription.CANCELLED;
            default -> null;
        };
    }

    private int getSubscriptionStatusPriority(Subscription status) {
        return switch (status) {
            case CREATED -> 0;
            case AUTHENTICATED -> 1;
            case ACTIVE -> 2;
            case COMPLETED -> 3;
            case CANCELLED -> 4;
            case EXPIRED -> 5;
        };
    }

    @Retryable(value = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class}, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional
    public void handleSubscriptionLogic(RazorpayWebhookEvent.SubscriptionEntity entity, String event) throws RazorpayException {
        log.info("Webhook Subscription Thread: {}", Thread.currentThread().getName());
        try{
            Thread.sleep(10000); // 10 sec delay
        }
         catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Plan plan = planRepository.findById(Integer.valueOf(entity.getNotes().get("planId"))).get();
        Owner owner = ownerRepository.findById(Integer.valueOf(entity.getNotes().get("ownerId"))).get();

        com.backend.gym_backend.entity.Subscription subscription = subscriptionRepository.findByRazorpaySubscriptionId(entity.getId())
                .orElseGet(() -> {
                    try {
                        com.backend.gym_backend.entity.Subscription newSub = new com.backend.gym_backend.entity.Subscription();
                        newSub.setRazorpaySubscriptionId(entity.getId());
                        newSub.setOwner(owner);
                        newSub.setPlan(plan);
                        newSub.setName(plan.getName());
                        newSub.setPrice(plan.getPrice());
                        newSub.setStartDate(LocalDate.now());
                        long daysToAdd = (plan.getDays() != null && !plan.getDays().isEmpty()) ? Long.parseLong(plan.getDays()) : 0L;
                        newSub.setEndDate(LocalDate.now().plusDays(daysToAdd));
                        newSub.setCreatedAt(LocalDateTime.now());
                        log.info("New subscription added with id {}", entity.getId());
                        return newSub;
                    } catch (DataIntegrityViolationException e) {
                        log.info("Another thread inserting same row with id {}", entity.getId());
                        return subscriptionRepository.findByRazorpaySubscriptionId(entity.getId()).orElseThrow();
                    }
                });

        Subscription oldStatus = subscription.getStatus();

        if (subscription.getOwner() == null) {
            subscription.setOwner(owner);
        }
        if (subscription.getPlan() == null) {
            subscription.setPlan(plan);
        }
        if (subscription.getName() == null) {
            subscription.setName(plan.getName());
        }
        if (subscription.getPrice() == null) {
            subscription.setPrice(plan.getPrice());
        }
        if (subscription.getCreatedAt() == null) {
            subscription.setCreatedAt(LocalDateTime.now());
        }
        if (entity.getCharge_at() != null) {
            subscription.setNextBillingDate(convertEpochToLocalDate(entity.getCharge_at()));
        }
        if (entity.getCurrent_start() != null) {
            subscription.setStartDate(convertEpochToLocalDate(entity.getCurrent_start()));
        }
        if (entity.getCurrent_end() != null) {
            subscription.setEndDate(convertEpochToLocalDate(entity.getCurrent_end()));
        }
        if (entity.getStart_at() != null) {
            subscription.setSubscriptionStartDate(convertEpochToLocalDate(entity.getStart_at()));
        }
        if (entity.getEnd_at() != null) {
            subscription.setSubscriptionEndDate(convertEpochToLocalDate(entity.getEnd_at()));
        }
        if (entity.getCustomer_contact() != null) {
            subscription.setContact(entity.getCustomer_contact());
        }
        if (entity.getCustomer_email() != null) {
            subscription.setEmail(entity.getCustomer_email());
        }

        Subscription newStatus = mapSubscriptionStatus(event);
        if (newStatus != null && subscription.getStatus() == null
                || getSubscriptionStatusPriority(newStatus) >= getSubscriptionStatusPriority(subscription.getStatus())) {

            subscription.setStatus(newStatus);

        } else {
            log.info("Skipping subscription status downgrade from {} to {}",
                    subscription.getStatus(), newStatus);
        }
        subscription.setUpdatedAt(LocalDateTime.now());
//        subscription.setOwner(owner);
        com.backend.gym_backend.entity.Subscription savedSubscription;

        try {
            savedSubscription = subscriptionRepository.save(subscription);
            log.info("subscription record saved successfully of event {}", event);

        } catch (OptimisticLockException e) {
            log.warn("OptimisticLockException occurred: {}", e.toString());
            throw e; // 🔥 VERY IMPORTANT → let @Retryable handle it

        } catch (DataIntegrityViolationException e) {
            // 🔥 Another thread already inserted it
            savedSubscription = subscriptionRepository
                    .findByRazorpaySubscriptionId(entity.getId())
                    .orElseThrow();

            log.warn("Handled duplicate subscription insert for {}", entity.getId());
        }

        boolean isBecomingActive =
                Subscription.ACTIVE.equals(newStatus)
                        && !Subscription.ACTIVE.equals(oldStatus);

        //Always use savedSubscription (never original)
        applicationEventPublisher.publishEvent(new SubscriptionLinkedEvent(subscription.getRazorpaySubscriptionId()));

        if (isBecomingActive) {
            applicationEventPublisher.publishEvent(
                    new SubscriptionActivatedEvent(
                            savedSubscription.getId())
            );
        }
    }
}
