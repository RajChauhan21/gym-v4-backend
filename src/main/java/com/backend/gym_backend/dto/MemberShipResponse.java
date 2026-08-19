package com.backend.gym_backend.dto;

import com.backend.gym_backend.entity.Member;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MemberShipResponse {

    private Integer id;

    private String name;

    private Integer validity;

    private Integer price;

    private List<Member> members;

    private Long memberCount;

}
