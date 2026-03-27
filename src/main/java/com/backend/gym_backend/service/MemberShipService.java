package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.MemberShipRequest;
import com.backend.gym_backend.dto.MemberShipResponse;
import com.backend.gym_backend.entity.Member;
import com.backend.gym_backend.entity.MemberShip;
import com.backend.gym_backend.repo.MemberRepository;
import com.backend.gym_backend.repo.MemberShipRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MemberShipService {

    @Autowired
    private MemberShipRepository memberShipRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Transactional
    public MemberShipResponse save(MemberShipRequest request) {
        MemberShip memberShip = new MemberShip();

        memberShip.setId(null);
        memberShip.setName(request.getName());
        memberShip.setPrice(request.getPrice());
        memberShip.setValidity(request.getValidity());
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
        if (!memberShipRepository.existsById(request.getId())) {
            throw new RuntimeException("Id not found");
        }
        MemberShip memberShip = new MemberShip();
        memberShip.setId(request.getId());
        memberShip.setName(request.getName());
        memberShip.setPrice(request.getPrice());
        memberShip.setValidity(request.getValidity());
        MemberShip save = memberShipRepository.save(memberShip);

        return MemberShipResponse.builder()
                .name(save.getName())
                .id(save.getId())
                .price(save.getPrice())
                .validity(save.getValidity())
                .members(save.getMembers())
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
                .members(save.getMembers())
                .build();
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

