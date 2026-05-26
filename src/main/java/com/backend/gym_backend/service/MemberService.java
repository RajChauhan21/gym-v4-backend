package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.*;
import com.backend.gym_backend.entity.Member;
import com.backend.gym_backend.entity.MemberShip;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.entity.Payment;
import com.backend.gym_backend.enums.SubscriptionStatus;
import com.backend.gym_backend.repo.MemberRepository;
import com.backend.gym_backend.repo.MemberShipRepository;
import com.backend.gym_backend.repo.OwnerRepository;
import com.backend.gym_backend.repo.SubscriptionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
        if (!memberShipRepository.existsById(request.getPackageId())) {
            throw new RuntimeException("Member Ship id not found");
        }
        if (!ownerRepository.existsById(request.getOwnerId())) {
            throw new RuntimeException("Owner id not found");
        }
        MemberShip memberShip = memberShipRepository.findById(request.getPackageId()).get();
        Owner owner = ownerRepository.findById(request.getOwnerId()).get();

        Member member = new Member();
        member.setId(null);
        member.setName(request.getName());
        member.setAddress(request.getAddress());
        member.setJoined(LocalDate.now());
        member.setMemberShip(memberShip);
        member.setOwner(owner);
        member.setDueAmount(memberShip.getPrice());
        member.setExpiry(LocalDate.now().plusDays(memberShip.getValidity()));
        member.setEmail(request.getEmail());
        member.setPhone(request.getPhone());

        Member save = memberRepository.save(member);


        return MemberResponse.builder()
                .id(save.getId())
                .name(save.getName())
                .email(save.getEmail())
                .address(save.getAddress())
                .joined(save.getJoined())
                .expiry(save.getExpiry())
                .phone(save.getPhone())
                .ownerId(save.getOwner().getId())
                .plan(save.getMemberShip().getName())
                .dueAmount(save.getDueAmount())
                .build();
    }

    @Transactional
    public MemberResponse update(MemberRequest request) {
        if (subscriptionRepository.findFirstByOwner_IdAndStatusOrderByCreatedAtDesc(request.getOwnerId(), SubscriptionStatus.ACTIVE).isEmpty()){
            throw new RuntimeException("100");
        }
        if (!memberShipRepository.existsById(request.getPackageId())) {
            throw new RuntimeException("Member Ship id not found");
        }
        if (!ownerRepository.existsById(request.getOwnerId())) {
            throw new RuntimeException("Owner id not found");
        }
        if(request.getMemberId()==null && memberRepository.existsByNameAndOwnerId(request.getName(),request.getOwnerId())){
            throw new RuntimeException("112");
        }
        if(memberRepository.existsByNameAndOwnerIdAndIdNot(request.getName(),request.getOwnerId(),request.getMemberId())){
            throw new RuntimeException("112");
        }
        MemberShip memberShip = memberShipRepository.findById(request.getPackageId()).get();
        Owner owner = ownerRepository.findById(request.getOwnerId()).get();

        Member newMember = new Member();
        newMember.setId(request.getMemberId()!=null ?request.getMemberId(): null);
        newMember.setName(request.getName());
        newMember.setAddress(request.getAddress());
        newMember.setJoined(request.getJoined()!=null?request.getJoined():LocalDate.now());
        newMember.setMemberShip(memberShip);
        newMember.setOwner(owner);
        newMember.setDueAmount(memberShip.getPrice());
        newMember.setExpiry(request.getExpiry()!=null?request.getExpiry():LocalDate.now().plusDays(memberShip.getValidity()));
        newMember.setEmail(request.getEmail());
        newMember.setPhone(request.getPhone());

        Member save = memberRepository.save(newMember);


        return MemberResponse.builder()
                .id(save.getId())
                .name(save.getName())
                .email(save.getEmail())
                .address(save.getAddress())
                .joined(save.getJoined())
                .expiry(save.getExpiry())
                .phone(save.getPhone())
                .ownerId(save.getOwner().getId())
                .plan(save.getMemberShip().getName())
                .dueAmount(save.getDueAmount())
                .build();
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
                .phone(save.getPhone())
                .ownerId(save.getOwner().getId())
                .plan(save.getMemberShip().getName())
                .dueAmount(save.getDueAmount())
                .build();
    }

    public List<MemberResponse> getMembersOnMemberShipId(Integer memberShipId) {
        List<Member> memberList = memberRepository.findByMemberShipId(memberShipId);
        List<MemberResponse> memberResponses = new ArrayList<>();

        for (Member m : memberList) {
            MemberResponse build = MemberResponse.builder()
                    .expiry(m.getExpiry())
                    .name(m.getName())
                    .joined(m.getJoined())
                    .id(m.getId())
                    .dueAmount(m.getDueAmount())
                    .email(m.getEmail())
                    .address(m.getAddress())
                    .phone(m.getPhone())
                    .build();

            memberResponses.add(build);
        }

        return memberResponses;
    }

    public Integer getAllMembersCount(Integer ownerId){
        return memberRepository.countAllMembersByOwnerId(ownerId);
    }

    public List<MemberSearchProjection> searchMembers(Integer ownerId,String query){
        if (subscriptionRepository.findFirstByOwner_IdAndStatusOrderByCreatedAtDesc(ownerId, SubscriptionStatus.ACTIVE).isEmpty()){
            throw new RuntimeException("100");
        }
        return memberRepository.searchMembers(ownerId,query);
    }

    public Integer countActiveMembers(Integer ownerId){
        if (subscriptionRepository.findFirstByOwner_IdAndStatusOrderByCreatedAtDesc(ownerId, SubscriptionStatus.ACTIVE).isEmpty()){
            throw new RuntimeException("100");
        }
        return memberRepository.countActiveMembersByOwner(ownerId);
    }

    public Integer getMembersJoinedCurrentMonth(Integer ownerId){
        if (subscriptionRepository.findFirstByOwner_IdAndStatusOrderByCreatedAtDesc(ownerId, SubscriptionStatus.ACTIVE).isEmpty()){
            throw new RuntimeException("100");
        }
        return memberRepository.countNewMembersThisMonth(ownerId);
    }

    public Integer getMembersCountExpiringIn7Days(Integer ownerId){
        if (subscriptionRepository.findFirstByOwner_IdAndStatusOrderByCreatedAtDesc(ownerId, SubscriptionStatus.ACTIVE).isEmpty()){
            throw new RuntimeException("100");
        }
        return memberRepository.countMembersExpiringSoon(ownerId);
    }

    public RevenueProjection getAllStatsOfMembers(Integer ownerId){
        if (subscriptionRepository.findFirstByOwner_IdAndStatusOrderByCreatedAtDesc(ownerId, SubscriptionStatus.ACTIVE).isEmpty()){
            throw new RuntimeException("100");
        }
        return memberRepository.getFullStatsByOwner(ownerId);
    }

    public List<MemberExpiryProjection> getLatestMemberExpiry(Integer ownerId){
        if (subscriptionRepository.findFirstByOwner_IdAndStatusOrderByCreatedAtDesc(ownerId, SubscriptionStatus.ACTIVE).isEmpty()){
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
