package com.backend.gym_backend.repo;

import com.backend.gym_backend.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Integer> {

    List<Member> findByOwnerId(Integer ownerId);

    List<Member> findByMemberShipId(Integer memberShipId);
}
