package com.backend.gym_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoicePdfDTO {

    private GymDTO gym;

    private MemberDTO member;

    private MembershipDTO membership;

    private PaymentDTO payment;

    private InvoiceDTO invoice;

}
