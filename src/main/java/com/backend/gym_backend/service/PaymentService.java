package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.*;
import com.backend.gym_backend.entity.Member;
import com.backend.gym_backend.entity.Payment;
import com.backend.gym_backend.repo.MemberRepository;
import com.backend.gym_backend.repo.PaymentRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Transactional
    public PaymentResponse save(PaymentRequest request) {
        if (!memberRepository.existsById(request.getMemberId())) {
            throw new RuntimeException("member id not found");
        }
        Member member = memberRepository.findById(request.getMemberId()).get();
        Payment payment = new Payment();
        payment.setId(request.getPaymentId() != null ? request.getPaymentId() : null);
        payment.setDate(request.getDate());
        payment.setMethod(request.getMethod());
        payment.setAmountPaid(request.getAmountPaid());
        payment.setMember(member);
        member.setDueAmount(Math.abs(member.getDueAmount()) - request.getAmountPaid());

        Payment save = paymentRepository.save(payment);
        memberRepository.save(member);
        return PaymentResponse.builder()
                .paymentId(save.getId())
                .member(save.getMember())
                .date(save.getDate())
                .amountPaid(save.getAmountPaid())
                .method(save.getMethod())
                .build();
    }

    @Transactional
    public PaymentResponse update(PaymentRequest request) {
        if (!memberRepository.existsById(request.getMemberId())) {
            throw new RuntimeException("member id not found");
        }
        Member member = memberRepository.findById(request.getMemberId()).get();

        Payment payment = new Payment();
        payment.setId(request.getPaymentId());
        payment.setDate(request.getDate());
        payment.setMethod(request.getMethod());
        payment.setAmountPaid(request.getAmountPaid());
        member.setDueAmount(Math.max(Math.abs(member.getDueAmount()) - request.getAmountPaid(), 0));
        payment.setMember(member);
        Payment save = paymentRepository.save(payment);
        return PaymentResponse.builder()
                .paymentId(save.getId())
                .member(save.getMember())
                .date(save.getDate())
                .amountPaid(save.getAmountPaid())
                .method(save.getMethod())
                .build();
    }

    public String deleteById(Integer id) {
        if (!paymentRepository.existsById(id)) {
            throw new RuntimeException("payment id not found");
        }
        paymentRepository.deleteById(id);
        return "Deleted successfully";
    }

    public List<PaymentResponse> getAllPaymentsOfMember(Integer memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new RuntimeException("member id not found");
        }
        Member member = memberRepository.findById(memberId).get();

        List<PaymentResponse> payments = new ArrayList<>();

        for (Payment p : member.getPayments()) {
            PaymentResponse build = PaymentResponse.builder()
                    .date(p.getDate())
                    .amountPaid(p.getAmountPaid())
                    .paymentId(p.getId())
                    .method(p.getMethod())
                    .member(p.getMember())
                    .build();

            payments.add(build);
        }

        return payments;
    }

    public Long getTotalAmountPaid() {
        return paymentRepository.sumAllAmounts();
    }

    public List<RevenueChartProjection> getRevenueOverview(Integer ownerId, Integer days){
        return paymentRepository.getRevenueOverview(ownerId,days);
    }

    public Page<PaymentProjection> getAllPaymentsOfMemberByOwnerId(Long ownerId, String memberName, String membershipName, String method, Integer amount, LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        return paymentRepository.findPaymentsFiltered(ownerId, memberName, membershipName, method, amount, dateFrom, dateTo, pageable);
    }

    public Double getRevenueThisMonth(){
        return paymentRepository.calculateCurrentMonthRevenueNative();
    }

    public List<RecentPaymentProjection> getRecentPaymentByOwnerId(Integer ownerId){
        return paymentRepository.findRecentPaymentsByOwner(ownerId);
    }

    public RevenueProjection getRevenues(Integer ownerId){
        return paymentRepository.getRevenueByOwner(ownerId);
    }

}
