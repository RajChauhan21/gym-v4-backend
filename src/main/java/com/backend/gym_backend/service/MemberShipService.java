package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.MemberShipRequest;
import com.backend.gym_backend.dto.MemberShipResponse;
import com.backend.gym_backend.entity.Member;
import com.backend.gym_backend.entity.MemberShip;
import com.backend.gym_backend.enums.SubscriptionStatus;
import com.backend.gym_backend.repo.MemberRepository;
import com.backend.gym_backend.repo.MemberShipRepository;
import com.backend.gym_backend.repo.OwnerRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class MemberShipService {

    @Autowired
    private MemberShipRepository memberShipRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private CommonService commonService;

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
        if (commonService.checkSubscriptionOfOwner(request.getOwnerId())==null){
            throw new RuntimeException("100");
        }
        if(ownerRepository.existsById(request.getOwnerId()) && ownerRepository.findById(request.getOwnerId()).get().getGym()==null){
            throw new RuntimeException("404");
        }
        if (request.getId()==null && memberShipRepository.existsByNameIgnoreCaseAndGymId(request.getName(),request.getGymId())){
            throw new RuntimeException("Plan already exists");
        }
        if (memberShipRepository.existsByNameAndGymIdAndIdNot(request.getName(),request.getGymId(), request.getId())){
            throw new RuntimeException("Plan already exists");
        }
        MemberShip memberShip;

        if (request.getId()!=null && request.getId() > 0){
           memberShip =  memberShipRepository.findById(request.getId())
                    .orElseThrow(()->new RuntimeException("plan not found"));
        }
        else{
            memberShip = new MemberShip();
        }
        memberShip.setId(request.getId());
        memberShip.setName(request.getName());
        memberShip.setPrice(request.getPrice());
        memberShip.setGym(ownerRepository.findById(request.getOwnerId()).get().getGym());
        memberShip.setValidity(request.getValidity()*30);//convert months in days
        memberShip.setUpdatedAt(LocalDateTime.now());
        if (memberShip.getCreatedAt()==null){
            memberShip.setCreatedAt(LocalDateTime.now());
        }
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
        List<Object[]> results =
                memberShipRepository.findAllPlansWithMemberCount(gymId);

        List<MemberShipResponse> responses = new ArrayList<>();

        for (Object[] row : results) {

            MemberShipResponse response = MemberShipResponse.builder()
                    .id((Integer) row[0])
                    .name((String) row[1])
                    .price((Integer) row[2])
                    .validity(((Number) row[3]).intValue() / 30)
                    .memberCount(((Number) row[4]).longValue())
                    .build();

            responses.add(response);
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

