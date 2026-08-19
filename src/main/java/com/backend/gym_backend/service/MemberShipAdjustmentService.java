package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.MemberShipAdjustmentRequest;
import com.backend.gym_backend.dto.MemberShipAdjustmentResponse;
import com.backend.gym_backend.entity.Member;
import com.backend.gym_backend.entity.MemberShipAdjustment;
import com.backend.gym_backend.entity.Subscription;
import com.backend.gym_backend.repo.MemberRepository;
import com.backend.gym_backend.repo.MemberShipAdjustmentRepository;
import com.backend.gym_backend.repo.OwnerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MemberShipAdjustmentService {

    @Autowired
    private MemberShipAdjustmentRepository repository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CommonService commonService;

    @Value("${member.active}")
    private int memberActive;

    @Value("${member.inactive}")
    private int memberInActive;

    @Value("${member.extended}")
    private int memberExtended;

    @Value("${member.freeze}")
    private int memberFreeze;

    @Value("${is.current}")
    private int isCurrent;


    @Transactional
    public String saveUpdate(MemberShipAdjustmentRequest request) {

        Subscription subscription = commonService.checkSubscriptionOfOwner(request.getOwnerId());
        Optional<Member> member = memberRepository.findById(request.getMemberId());

        if (subscription == null) {
            throw new RuntimeException("100");
        }

        if (!ownerRepository.existsById(request.getOwnerId())) {
            throw new RuntimeException("Owner id not found");
        }

        if (member.isEmpty()) {
            throw new RuntimeException("member id not found");
        }

        if (request.getNewEndDate().isBefore(request.getOldEndDate())) {
            throw new RuntimeException("101");//New end date cannot be before old end date
        }

        MemberShipAdjustment memberShipAdjustment;

        if (request.getId() > 0) {
            memberShipAdjustment = repository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("Adjustment not found"));

            if (memberShipAdjustment.getMemberId() != request.getMemberId()) {
                throw new RuntimeException("Adjustment does not belong to member");
            }
        } else {
            memberShipAdjustment = new MemberShipAdjustment();
        }

        if (memberShipAdjustment.getMemberId() <= 0) {
            memberShipAdjustment.setMemberId(request.getMemberId());
        }

        if (memberShipAdjustment.getOwnerId() <= 0) {
            memberShipAdjustment.setOwnerId(request.getOwnerId());
        }

        if (memberShipAdjustment.getCreatedAt() == null) {
            memberShipAdjustment.setCreatedAt(LocalDateTime.now());
        }

        memberShipAdjustment.setUpdatedAt(LocalDateTime.now());
        if (request.getNotes() != null) memberShipAdjustment.setNotes(request.getNotes());
        if (request.getReason() != null) memberShipAdjustment.setReason(request.getReason());
        if (request.getStatus() > 0) {
            memberShipAdjustment.setStatus(request.getStatus());
            member.get().setIsActive(request.getStatus());
        }
        if (request.getDurationDays() > 0) memberShipAdjustment.setDurationDays(request.getDurationDays());
        if (request.getNewEndDate() != null) {
            memberShipAdjustment.setNewEndDate(request.getNewEndDate());
            member.get().setExpiry(request.getNewEndDate());
            member.get().setUpdatedAt(LocalDateTime.now());
        }
        if (request.getOldEndDate() != null) memberShipAdjustment.setOldEndDate(request.getOldEndDate());

        if (request.getFreezeStartDate() != null) {
            memberShipAdjustment.setFreezeStartDate(
                    request.getFreezeStartDate()
            );
        }

        if (request.getFreezeEndDate() != null) {
            memberShipAdjustment.setFreezeEndDate(
                    request.getFreezeEndDate()
            );
        }

        MemberShipAdjustment memberShipUpdateFreeze;

        //check and if exists, update freeze if editing extension
        if (request.getStatus() == memberExtended && request.getId() > 0) {
            Optional<MemberShipAdjustment> freezeAdjustment = repository.findByMemberIdAndStatus(request.getMemberId(), memberFreeze);

            if (freezeAdjustment.isPresent()) {
                memberShipUpdateFreeze = freezeAdjustment.get();

                memberShipUpdateFreeze.setOldEndDate(request.getNewEndDate());//extension's new end
                //date will be freeze's old date because freeze will be updated from that date

                LocalDate newEndDate = request.getNewEndDate().plusDays(memberShipUpdateFreeze.getDurationDays());
                memberShipUpdateFreeze.setNewEndDate(newEndDate);//take new end date and calculate updated new end date of freeze
                //update member expiry also
                member.get().setExpiry(newEndDate);

                if (request.getFreezeStartDate() != null) {
                    memberShipUpdateFreeze.setFreezeStartDate(
                            request.getFreezeStartDate()
                    );
                }

                if (request.getFreezeEndDate() != null) {
                    memberShipUpdateFreeze.setFreezeEndDate(
                            request.getFreezeEndDate()
                    );
                }

                memberShipUpdateFreeze.setUpdatedAt(LocalDateTime.now());

                repository.saveAndFlush(memberShipUpdateFreeze);
            }
        }
        memberShipAdjustment.setIsCurrent(isCurrent);
        repository.saveAndFlush(memberShipAdjustment);
        memberRepository.saveAndFlush(member.get());
        return "ok";
    }

    public List<MemberShipAdjustmentResponse> getMemberShipAdjustmentByMemberId(Integer memberId) {
        List<MemberShipAdjustmentResponse> responses = new ArrayList<>();

        if (memberId == null) {
            throw new RuntimeException("100");
        }
        Optional<List<MemberShipAdjustment>> members = repository.findByMemberIdAndIsCurrent(memberId,isCurrent);
        if (members.isEmpty()) {
            throw new RuntimeException("101");
        }
        for (MemberShipAdjustment member : members.get()) {
            MemberShipAdjustmentResponse m = MemberShipAdjustmentResponse.builder()
                    .memberId(member.getMemberId())
                    .id(member.getId())
                    .notes(member.getNotes())
                    .reason(member.getReason())
                    .durationDays(member.getDurationDays())
                    .oldEndDate(member.getOldEndDate())
                    .status(member.getStatus())
                    .freezeStartDate(member.getFreezeStartDate())
                    .freezeEndDate(member.getFreezeEndDate())
                    .newEndDate(member.getNewEndDate())
                    .build();

            responses.add(m);
        }

        return responses;
    }

    public String deleteMembershipAdjustment(Integer id) {
        if (id == null) {
            throw new RuntimeException("100");
        }
        Optional<MemberShipAdjustment> memberShipAdjustment = repository.findById(id);
        if (memberShipAdjustment.isEmpty()) {
            throw new RuntimeException("101"); //not found in db
        }

        Optional<Member> member = memberRepository.findById(memberShipAdjustment.get().getMemberId());
        if (member.isEmpty()) {
            throw new RuntimeException("102"); //member not found
        }

        member.get().setExpiry(memberShipAdjustment.get().getOldEndDate()); //set original expiry date
        member.get().setIsActive(member.get().getExpiry().isBefore(LocalDate.now()) ? memberInActive:memberActive);//make again member status to active/in-active based on expiry
        member.get().setUpdatedAt(LocalDateTime.now());
        memberRepository.saveAndFlush(member.get());
        memberShipAdjustment.get().setIsCurrent(0);
        repository.saveAndFlush(memberShipAdjustment.get());
//        repository.delete(memberShipAdjustment.get());

        return "ok";
    }

//    @Scheduled(cron = "0 0 0 * * *") //12 AM
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void updateMembersStatus() {
        log.info("Scheduler triggered for updating member status");
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
       int inactiveCount =  memberRepository.updateExpiredMembersToInactive(today,memberInActive,now);
        log.info("updated {} member's status to inactive",inactiveCount);

        List<MemberShipAdjustment> expiredAdjustments = repository.findExpiredAdjustments(today);
        log.info("found {} expired records",expiredAdjustments.size());

        if (expiredAdjustments.isEmpty()) {
            return;
        }

        Set<Integer> memberIds = expiredAdjustments.stream()
                .map(MemberShipAdjustment::getMemberId)
                .collect(Collectors.toSet());

        List<Member> members = memberRepository.findAllById(memberIds);

        if (members.isEmpty()) {
            return;
        }

        Set<Integer> activeExtensionSet = repository.findActiveExtensionMemberIds(memberIds, memberExtended, today);

        Map<Integer, Member> memberMap = members.stream()
                .collect(Collectors.toMap(
                        Member::getId,
                        Function.identity()
                ));

        for (MemberShipAdjustment adjustment : expiredAdjustments) {

            Member member = memberMap.get(adjustment.getMemberId());
            if (member == null) {
                continue;
            }

            // Freeze expired
            if (adjustment.getStatus() == memberFreeze) {

                if (member.getExpiry() != null
                        && member.getExpiry().isBefore(today)) {
                    // Membership also expired
                    member.setIsActive(memberInActive);

                } else if(activeExtensionSet.contains(adjustment.getMemberId())) {
                    // Freeze expired but extension is active
                    member.setIsActive(memberExtended);
                }
                else{
                    //freeze expired, no extension
                    member.setIsActive(memberActive);
                }

            } else if (adjustment.getStatus() == memberExtended) {

                // Extension expired
                member.setIsActive(0);
            }

            member.setUpdatedAt(LocalDateTime.now());
            adjustment.setIsCurrent(0);
            adjustment.setUpdatedAt(LocalDateTime.now());
        }

//        memberRepository.saveAll(members);
        log.info("found {} expired records",expiredAdjustments.size());
    }

}
