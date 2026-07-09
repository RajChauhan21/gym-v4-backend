package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.RazorpayWebhookEvent;
import com.backend.gym_backend.dto.SubscriptionActivatedEvent;
import com.backend.gym_backend.dto.SubscriptionLinkedEvent;
import com.backend.gym_backend.dto.SubscriptionResponse;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.entity.Plan;
import com.backend.gym_backend.entity.Subscription;
import com.backend.gym_backend.entity.SubscriptionCancelRetry;
import com.backend.gym_backend.enums.Retry;
import com.backend.gym_backend.enums.SubscriptionStatus;
import com.backend.gym_backend.repo.*;
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
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @Autowired
    private SubscriptionCancelRetryRepository retryRepository;

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
        subscription.setStatus(status.equals("ACTIVE") ? SubscriptionStatus.ACTIVE : SubscriptionStatus.CREATED);
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
                .subscriptionStatus(save.getStatus())
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
                .subscriptionStatus(save.getStatus())
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
                    .subscriptionStatus(s.getStatus())
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
                    .subscriptionStatus(s.getStatus())
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
                    .subscriptionStatus(s.getStatus())
                    .build();

            subscriptions.add(build);
        }
        return subscriptions;
    }

    private SubscriptionStatus mapSubscriptionStatus(String event) {
        return switch (event) {
            case "subscription.authenticated" -> SubscriptionStatus.AUTHENTICATED;
            case "subscription.activated", "subscription.charged" -> SubscriptionStatus.ACTIVE;
            case "subscription.cancelled" -> SubscriptionStatus.PARTIALLY_ACTIVE;
            default -> null;
        };
    }

    private int getSubscriptionStatusPriority(SubscriptionStatus status) {
        return switch (status) {
            case CREATED -> 0;
            case AUTHENTICATED -> 1;
            case PARTIALLY_ACTIVE, ACTIVE -> 3;
            case COMPLETED -> 4;
            case CANCELLED -> 5;
            case EXPIRED -> 6;
        };
    }

    @Retryable(retryFor = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2, random = true))
    @Transactional
    public void handleSubscriptionLogic(RazorpayWebhookEvent.SubscriptionEntity entity, String event) throws RazorpayException {
        log.info("Webhook Subscription Thread: {}", Thread.currentThread().getName());


        Plan plan;
        Owner owner;

        if(!entity.getNotes().isEmpty()) {
            if (!entity.getNotes().get("planId").isEmpty()) {
                plan = planRepository.findById(Integer.valueOf(entity.getNotes().get("planId"))).get();
            } else {
                plan = null;
            }
            if (!entity.getNotes().get("ownerId").isEmpty()) {
                owner = ownerRepository.findById(Integer.valueOf(entity.getNotes().get("ownerId"))).get();

            } else {
                owner = null;
            }
        } else {
            plan = null;
            owner = null;
        }

        if (owner==null){
            owner = ownerRepository.findByEmail(entity.getCustomer_email()).orElse(null);
        }

        if (plan==null){
            plan = planRepository.findByRazorPayPlanId(entity.getPlan_id()).orElse(null);
        }


        Owner finalOwner = owner;
        Plan finalPlan = plan;
        com.backend.gym_backend.entity.Subscription subscription = subscriptionRepository.findByRazorpaySubscriptionId(entity.getId())
                .orElseGet(() -> {
                    try {
                        com.backend.gym_backend.entity.Subscription newSub = new com.backend.gym_backend.entity.Subscription();
                        newSub.setRazorpaySubscriptionId(entity.getId());
                        newSub.setOwner(finalOwner);
                        newSub.setPlan(finalPlan);
                        newSub.setName(finalPlan.getName());
                        newSub.setPrice(finalPlan.getPrice());
                        newSub.setStartDate(LocalDate.now());
                        long daysToAdd = (finalPlan.getDays() != null && !finalPlan.getDays().isEmpty()) ? Long.parseLong(finalPlan.getDays()) : 0L;
                        newSub.setEndDate(LocalDate.now().plusDays(daysToAdd));
                        newSub.setCreatedAt(LocalDateTime.now());
                        log.info("New subscription added with id {} and event {}", entity.getId(),event);
                        return newSub;
                    } catch (DataIntegrityViolationException e) {
                        log.info("Another thread inserting same row with id {}", entity.getId());
                        return subscriptionRepository.findByRazorpaySubscriptionId(entity.getId()).orElseThrow();
                    }
                });

        SubscriptionStatus oldStatus = subscription.getStatus();

        if (subscription.getOwner() == null) {
            subscription.setOwner(finalOwner);
        }
        if (subscription.getPlan() == null) {
            subscription.setPlan(finalPlan);
        }
        if (subscription.getName() == null) {
            subscription.setName(finalPlan.getName());
        }
        if (subscription.getPrice() == null) {
            subscription.setPrice(finalPlan.getPrice());
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

        SubscriptionStatus newStatus = mapSubscriptionStatus(event);
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
            log.info("subscription record saved successfully of event {}", event);
            log.info(
                    "Event={}, SubscriptionId={}, CurrentStatus={}, NewStatus={}",
                    event,
                    entity.getId(),
                    subscription.getStatus(),
                    newStatus
            );
            savedSubscription = subscriptionRepository.saveAndFlush(subscription);
            log.info(
                    "Saved Event={}, FinalStatus={}",
                    event,
                    savedSubscription.getStatus()
            );

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
                SubscriptionStatus.ACTIVE.equals(newStatus)
                        && !SubscriptionStatus.ACTIVE.equals(oldStatus);

        //Always use savedSubscription (never original)
        applicationEventPublisher.publishEvent(new SubscriptionLinkedEvent(subscription.getRazorpaySubscriptionId()));

        if (isBecomingActive) {
            applicationEventPublisher.publishEvent(
                    new SubscriptionActivatedEvent(
                            savedSubscription.getId())
            );
        }
    }

    @Recover
    public void recoverOptimisticLock(Exception ex, RazorpayWebhookEvent.SubscriptionEntity entity, String event) {

        log.warn("Final failure after retries for payment {}. Reason: {}",
                entity.getId(), ex.getClass().getSimpleName());
    }

    @jakarta.transaction.Transactional
    public void processSingleRetry(SubscriptionCancelRetry retry) {
        try {
            razorpayClient.subscriptions.cancel(retry.getRazorpaySubscriptionId());

            retry.setStatus(Retry.SUCCESS);

            // ✅ update subscription
            Subscription subscription = subscriptionRepository
                    .findByRazorpaySubscriptionId(retry.getRazorpaySubscriptionId())
                    .orElse(null);

            if (subscription != null) {
                subscription.setStatus(SubscriptionStatus.CANCELLED);
                subscription.setUpdatedAt(LocalDateTime.now());
                subscriptionRepository.save(subscription);
            }

            log.info("subscription retry succeeded and cancelled");

        } catch (Exception ex) {

            int count = retry.getRetryCount() + 1;
            retry.setRetryCount(count);

            if (count >= 5) {
                retry.setStatus(Retry.FAILED);
            } else {
                retry.setNextRetryAt(LocalDateTime.now().plusMinutes(5 * count));
            }

            log.info("subscription failed again for id {}", retry.getRazorpaySubscriptionId());
        } finally {
            retryRepository.save(retry);
            log.info("Retry saved with status {}", retry.getStatus());
        }
    }
}
