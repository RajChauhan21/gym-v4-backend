package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.*;
import com.backend.gym_backend.entity.*;
import com.backend.gym_backend.enums.SubscriptionStatus;
import com.backend.gym_backend.repo.MemberRepository;
import com.backend.gym_backend.repo.MemberShipRepository;
import com.backend.gym_backend.repo.OwnerRepository;
import com.backend.gym_backend.repo.SubscriptionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MemberService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberShipRepository memberShipRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Transactional
    public MemberResponse save(MemberRequest request) {
        if (!memberShipRepository.existsById(request.getMemberShipId())) {
            throw new RuntimeException("Member Ship id not found");
        }
        if (!ownerRepository.existsById(request.getOwnerId())) {
            throw new RuntimeException("Owner id not found");
        }
        MemberShip memberShip = memberShipRepository.findById(request.getMemberShipId()).get();
        Owner owner = ownerRepository.findById(request.getOwnerId()).get();

        Member member = new Member();
        member.setId(null);
        member.setName(request.getName());
        member.setAddress(request.getAddress());
        member.setJoined(LocalDate.now());
        member.setMemberShip(memberShip);
        member.setStartDate(request.getStartDate());
        member.setOwner(owner);
        member.setDueAmount(memberShip.getPrice());
        member.setExpiry(LocalDate.now().plusDays(memberShip.getValidity()));
        member.setEmail(request.getEmail());
        member.setPhone(request.getPhone());
        member.setIsActive(1);
        member.setSourceId(request.getSourceId());

        Member save = memberRepository.save(member);


        return MemberResponse.builder()
                .id(save.getId())
                .name(save.getName())
                .email(save.getEmail())
                .address(save.getAddress())
                .joined(save.getJoined())
                .expiry(save.getExpiry())
                .startDate(save.getStartDate())
                .phone(save.getPhone())
                .ownerId(save.getOwner().getId())
                .plan(save.getMemberShip().getName())
                .dueAmount(save.getDueAmount())
                .build();
    }

    @Transactional
    public MemberResponse update(MemberRequest request) {
        Subscription subscription = subscriptionRepository.findFirstByOwner_IdAndStatusInOrderByCreatedAtDesc(request.getOwnerId(), List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PARTIALLY_ACTIVE)).orElse(null);
        Integer currentCount = memberRepository.countAllMembersByOwnerIdAndIsActive(request.getOwnerId(),1);
        Plan plan = (subscription != null) ? subscription.getPlan() : null;

        if (subscription == null) {
            throw new RuntimeException("100");
        }
        if (request.getMemberId()==null && currentCount!=null && plan!=null && plan.getMemberLimit() <= currentCount){
            throw new RuntimeException("limit");
        }
        if (!memberShipRepository.existsById(request.getMemberShipId())) {
            throw new RuntimeException("Member Ship id not found");
        }
        if (!ownerRepository.existsById(request.getOwnerId())) {
            throw new RuntimeException("Owner id not found");
        }
        if (request.getMemberId() == null && memberRepository.existsByNameAndOwnerId(request.getName(), request.getOwnerId())) {
            throw new RuntimeException("112");
        }
        if (memberRepository.existsByNameAndOwnerIdAndIdNot(request.getName(), request.getOwnerId(), request.getMemberId())) {
            throw new RuntimeException("112");
        }
        MemberShip memberShip = memberShipRepository.findById(request.getMemberShipId()).get();
        Owner owner = ownerRepository.findById(request.getOwnerId()).get();

        Member newMember = new Member();
        newMember.setId(request.getMemberId() != null ? request.getMemberId() : null);
        newMember.setName(request.getName());
        newMember.setAddress(request.getAddress());
        newMember.setJoined(request.getJoined() != null ? request.getJoined() : LocalDate.now());
        newMember.setMemberShip(memberShip);
        newMember.setOwner(owner);
        newMember.setStartDate(request.getStartDate());
        newMember.setDueAmount(memberShip.getPrice());
        newMember.setExpiry(request.getExpiry() != null ? request.getExpiry() : LocalDate.now().plusDays(memberShip.getValidity()));
        newMember.setEmail(request.getEmail());
        newMember.setPhone(request.getPhone());
        newMember.setSourceId(request.getSourceId());
        newMember.setIsActive(1);

        Member save = memberRepository.save(newMember);


        return MemberResponse.builder()
                .id(save.getId())
                .name(save.getName())
                .email(save.getEmail())
                .address(save.getAddress())
                .joined(save.getJoined())
                .expiry(save.getExpiry())
                .startDate(save.getStartDate())
                .phone(save.getPhone())
                .ownerId(save.getOwner().getId())
                .plan(save.getMemberShip().getName())
                .dueAmount(save.getDueAmount())
                .build();
    }

    public Integer getActiveMembers(Integer ownerId, Integer isActive){
        return memberRepository.countAllMembersByOwnerIdAndIsActive(ownerId,isActive);
    }

    @Transactional
    public String makeMemberActiveOrInactive(String action, Integer memberId, Integer ownerId){
        Optional<Member> member = memberRepository.findById(memberId);
        if (member.isEmpty()){
            throw new RuntimeException("401"); //check if member exists or not
        }
        Subscription subscription = subscriptionRepository.findFirstByOwner_IdAndStatusInOrderByCreatedAtDesc(ownerId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PARTIALLY_ACTIVE)).orElse(null);

        //get active count of members
        Integer currentCount = memberRepository.countAllMembersByOwnerIdAndIsActive(ownerId,1);

        Plan plan = (subscription != null) ? subscription.getPlan() : null;

        //check if a user has enough limit/space to make member active
        if (currentCount!=null && plan!=null && plan.getMemberLimit() <= currentCount && action!=null && !action.isEmpty() && action.equals("active")){
            throw new RuntimeException("limit");
        }
        if (action!=null && !action.isEmpty() && action.equals("active")){
            member.get().setIsActive(1);
        }
        else if(action!=null && !action.isEmpty() && action.equals("inactive")){
            member.get().setIsActive(0);
        }

        return action;
    }

    public MemberResponse findById(Integer id) {
        if (!memberRepository.existsById(id)) {
            throw new RuntimeException("Member id not found");
        }

        Member save = memberRepository.findById(id).get();

        return MemberResponse.builder()
                .id(save.getId())
                .name(save.getName())
                .email(save.getEmail())
                .address(save.getAddress())
                .joined(save.getJoined())
                .expiry(save.getExpiry())
                .startDate(save.getStartDate())
                .phone(save.getPhone())
                .ownerId(save.getOwner().getId())
                .plan(save.getMemberShip().getName())
                .dueAmount(save.getDueAmount())
                .sourceId(save.getSourceId())
                .build();
    }

    public long getMembersCountOnMemberShipId(Integer memberShipId,Integer gymId,Integer ownerId) {
       return memberRepository.countByMembershipIdAndGymId(memberShipId,gymId,ownerId);
    }

    public Integer getAllMembersCount(Integer ownerId) {
        return memberRepository.countAllMembersByOwnerId(ownerId);
    }

    public List<MemberSearchProjection> searchMembers(Integer ownerId, String query) {
        if (subscriptionRepository.findFirstByOwner_IdAndStatusInOrderByCreatedAtDesc(ownerId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PARTIALLY_ACTIVE)).isEmpty()) {
            throw new RuntimeException("100");
        }
        return memberRepository.searchMembers(ownerId, query);
    }

    public Integer countActiveMembers(Integer ownerId) {
        if (subscriptionRepository.findFirstByOwner_IdAndStatusInOrderByCreatedAtDesc(ownerId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PARTIALLY_ACTIVE)).isEmpty()) {
            throw new RuntimeException("100");
        }
        return memberRepository.countActiveMembersByOwner(ownerId);
    }

    public Integer getMembersJoinedCurrentMonth(Integer ownerId) {
        if (subscriptionRepository.findFirstByOwner_IdAndStatusInOrderByCreatedAtDesc(ownerId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PARTIALLY_ACTIVE)).isEmpty()) {
            throw new RuntimeException("100");
        }
        return memberRepository.countNewMembersThisMonth(ownerId);
    }

    public Integer getMembersCountExpiringIn7Days(Integer ownerId) {
        if (subscriptionRepository.findFirstByOwner_IdAndStatusInOrderByCreatedAtDesc(ownerId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PARTIALLY_ACTIVE)).isEmpty()) {
            throw new RuntimeException("100");
        }
        return memberRepository.countMembersExpiringSoon(ownerId);
    }

    public RevenueProjection getAllStatsOfMembers(Integer ownerId) {
        if (subscriptionRepository.findFirstByOwner_IdAndStatusInOrderByCreatedAtDesc(ownerId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PARTIALLY_ACTIVE)).isEmpty()) {
            throw new RuntimeException("100");
        }
        return memberRepository.getFullStatsByOwner(ownerId);
    }

    public String renewMemberShip(RenewMemberShipRequest request){
        if (request.getMemberId()==null || request.getPlanId()==null){
            return "404";
        }
        if (!memberRepository.existsById(request.getMemberId())){
            return "404";
        }
        if (!memberShipRepository.existsById(request.getPlanId())){
            return "404";
        }
        MemberShip memberShip = memberShipRepository.findById(request.getPlanId()).get();
        Member member = memberRepository.findById(request.getMemberId()).get();
        member.setStartDate(request.getStartDate());
        member.setJoined(request.getJoiningDate());
        member.setExpiry(request.getExpiryDate());
        member.setDueAmount(request.getDueAmount());
        member.setMemberShip(memberShip);
        memberRepository.save(member);

        return "OK";
    }

    public List<MemberExpiryProjection> getLatestMemberExpiry(Integer ownerId) {
        if (subscriptionRepository.findFirstByOwner_IdAndStatusInOrderByCreatedAtDesc(ownerId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PARTIALLY_ACTIVE)).isEmpty()) {
            throw new RuntimeException("100");
        }
        return memberRepository.findExpiringMembers(ownerId);
    }

    @Transactional
    public String deleteById(Integer id) {
        if (!memberRepository.existsById(id)) {
            throw new RuntimeException("Member id not found");
        }
        memberRepository.deleteById(id);
        return "Member deleted successfully";
    }

}
