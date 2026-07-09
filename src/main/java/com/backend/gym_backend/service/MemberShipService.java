package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.MemberShipRequest;
import com.backend.gym_backend.dto.MemberShipResponse;
import com.backend.gym_backend.entity.Member;
import com.backend.gym_backend.entity.MemberShip;
import com.backend.gym_backend.enums.SubscriptionStatus;
import com.backend.gym_backend.repo.MemberRepository;
import com.backend.gym_backend.repo.MemberShipRepository;
import com.backend.gym_backend.repo.OwnerRepository;
import com.backend.gym_backend.repo.SubscriptionRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MemberShipService {

    @Autowired
    private MemberShipRepository memberShipRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Transactional
    public MemberShipResponse save(MemberShipRequest request) {
        if (ownerRepository.findById(request.getOwnerId()).get().getGym()==null){
            throw new RuntimeException("404");
        }
        MemberShip memberShip = new MemberShip();
        memberShip.setId(null);
        memberShip.setName(request.getName());
        memberShip.setPrice(request.getPrice());
        memberShip.setValidity(request.getValidity());
        memberShip.setGym(ownerRepository.findById(request.getOwnerId()).get().getGym());
        MemberShip save = memberShipRepository.save(memberShip);

        return MemberShipResponse.builder()
                .name(save.getName())
                .id(save.getId())
                .price(save.getPrice())
                .validity(save.getValidity())
                .members(save.getMembers())
                .build();
    }

    @Transactional
    public MemberShipResponse update(MemberShipRequest request) {
        if (subscriptionRepository.findFirstByOwner_IdAndStatusInOrderByCreatedAtDesc(request.getOwnerId(), List.of(SubscriptionStatus.ACTIVE,SubscriptionStatus.PARTIALLY_ACTIVE)).isEmpty()){
            throw new RuntimeException("100");
        }
        if(ownerRepository.existsById(request.getOwnerId()) && ownerRepository.findById(request.getOwnerId()).get().getGym()==null){
            throw new RuntimeException("404");
        }
        if (request.getId()==null && memberShipRepository.existsByNameAndGymId(request.getName(),request.getGymId())){
            throw new RuntimeException("Plan already exists");
        }
        if (memberShipRepository.existsByNameAndGymIdAndIdNot(request.getName(),request.getGymId(), request.getId())){
            throw new RuntimeException("Plan already exists");
        }
        MemberShip memberShip = new MemberShip();
        memberShip.setId(request.getId());
        memberShip.setName(request.getName());
        memberShip.setPrice(request.getPrice());
        memberShip.setGym(ownerRepository.findById(request.getOwnerId()).get().getGym());
        memberShip.setValidity(request.getValidity()*30);//convert months in days
        MemberShip save = memberShipRepository.save(memberShip);

        return MemberShipResponse.builder()
                .name(save.getName())
                .id(save.getId())
                .price(save.getPrice())
                .validity(save.getValidity()/30)
                .build();
    }

    public MemberShipResponse findById(Integer id) {
        if (!memberShipRepository.existsById(id)) {
            throw new RuntimeException("Id not found");
        }

        MemberShip save = memberShipRepository.findById(id).get();

        return MemberShipResponse.builder()
                .name(save.getName())
                .id(save.getId())
                .price(save.getPrice())
                .validity(save.getValidity())
                .build();
    }

    public List<MemberShipResponse> getAllByGymId(Integer gymId){
        List<MemberShipResponse> responses = new ArrayList<>();
        for (MemberShip m : memberShipRepository.findAllByGymId(gymId)){
            MemberShipResponse build = MemberShipResponse.builder()
                    .name(m.getName())
                    .id(m.getId())
                    .price(m.getPrice())
                    .validity(m.getValidity() / 30)
                    .build();
            responses.add(build);
        }

        return responses;
    }

    @Transactional
    public String deleteById(Integer id){
        if (!memberShipRepository.existsById(id)) {
            throw new RuntimeException("Id not found");
        }

        MemberShip memberShip = memberShipRepository.findById(id).get();
        List<Member> members = memberShip.getMembers();

        for (Member m : members) {
            m.setMemberShip(null);
            memberRepository.save(m);
        }
        memberShipRepository.deleteById(id);
        return "deleted successfully";
    }
}

