package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.MemberSourceRequest;
import com.backend.gym_backend.dto.MemberSourceResponse;
import com.backend.gym_backend.dto.SourceAnalyticsProjection;
import com.backend.gym_backend.entity.MemberSource;
import com.backend.gym_backend.entity.Subscription;
import com.backend.gym_backend.enums.SubscriptionStatus;
import com.backend.gym_backend.repo.MemberSourceRepository;
import com.backend.gym_backend.repo.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MemberSourceService {

    @Autowired
    private MemberSourceRepository memberSourceRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Value("${source.max.limit}")
    private Integer sourcesLimit;

    public String save(MemberSourceRequest request) {
        Optional<Subscription> ss = subscriptionRepository.findFirstByOwner_IdAndStatusInOrderByCreatedAtDesc(request.getOwnerId(), List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PARTIALLY_ACTIVE));

        if (ss.isPresent()) {
            if (!ss.get().getName().equals("Max Pro")) {
                throw new RuntimeException("300");
            }
        } else {
            throw new RuntimeException("100");
        }

        if (sourcesLimit <= memberSourceRepository.countSourcesByOwnerId(request.getOwnerId())) {
            throw new RuntimeException("limit");
        }

        if (memberSourceRepository.existsByNameAndOwnerId(request.getName().strip(), request.getOwnerId())) {
            throw new RuntimeException("duplicate");
        }

        MemberSource memberSource = MemberSource.builder()
                .id(request.getId())
                .name(request.getName())
                .ownerId(request.getOwnerId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        memberSourceRepository.save(memberSource);

        return "202";
    }

    public String update(MemberSourceRequest request) {
        if (!memberSourceRepository.existsById(request.getId())) {
            throw new RuntimeException("Source not found");
        }

        if (memberSourceRepository
                .existsByNameIgnoreCaseAndOwnerIdAndIdNot(
                        request.getName().strip(),
                        request.getOwnerId(),
                        request.getId())) {

            throw new RuntimeException(
                    "duplicate"
            );
        }

        MemberSource memberSource = memberSourceRepository.findById(request.getId()).get();
        memberSource.setName(request.getName());
        memberSource.setUpdatedAt(LocalDateTime.now());
        memberSourceRepository.save(memberSource);

        return "202";
    }

    public List<MemberSourceResponse> getAllSourcesOfOwner(Integer ownerId) {
        List<MemberSourceResponse> responses = new ArrayList<>();
        List<MemberSource> sources = memberSourceRepository.findAllByOwnerId(ownerId);
        if (sources.isEmpty()) {
            return new ArrayList<>();
        }
        for (MemberSource m : sources) {
            MemberSourceResponse source = MemberSourceResponse.builder()
                    .ownerId(m.getOwnerId())
                    .name(m.getName())
                    .id(m.getId())
                    .build();
            responses.add(source);
        }
        return responses;
    }

    public List<SourceAnalyticsProjection> getSourceAnalytics(Integer ownerId) {
        return memberSourceRepository.findSourceAnalytics(ownerId);
    }

    public Integer countMembersBySourceId(Integer sourceId){
        return memberSourceRepository.countMembersBySourceId(sourceId);
    }

    public String deleteSource(Integer sourceId) {
        if (memberSourceRepository.existsById(sourceId)) {
            memberSourceRepository.deleteById(sourceId);
        } else {
            return "400"; //id not found
        }
        return "202";
    }

}
