package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.*;
import com.backend.gym_backend.service.InvoiceTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;
import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/invoice/template-preview")
public class InvoiceTemplatePreviewController {

    @Autowired
    private InvoiceTemplateService invoiceTemplateService;


    @GetMapping(value = "/{template}", produces = MediaType.TEXT_HTML_VALUE)
    public String previewInvoiceTemplate(@PathVariable String template) {
        Context context = new Context();
        GymDTO gymDTO = GymDTO.builder()
                .name("Shri Gym")
                .phone("+9136784783")
                .email("owner@gmail.com")
                .address("Mumbai")
                .website("www.mygym.com")
                .signatureName("Owner name")
                .terms("N/A")
                .thankYouMessage("Keep training. Stay consistent. Stay healthy.")
                .build();

        MemberDTO memberDTO = MemberDTO.builder()
                .name("Vicky Singh")
                .memberId("1")
                .phone("+9147847847")
                .email("vicky@gmail.com")
                .build();

        InvoiceDTO invoiceDTO = InvoiceDTO.builder()
                .invoiceDate(LocalDate.now())
                .paymentDate(LocalDate.now())
                .invoiceNumber("22")
                .build();

        PaymentDTO paymentDTO = PaymentDTO.builder()
                .paymentMethod("CASH")
                .gst(BigDecimal.valueOf(0))
                .total(5000)
                .dueAmount(2000)
                .paidAmount(3000)
                .discount(BigDecimal.valueOf(0))
                .subtotal(5000)
                .transactionId("N/A")
                .build();

        MembershipDTO membershipDTO = MembershipDTO.builder()
                .duration("3 Months")
                .expiryDate(LocalDate.now().plusDays(30))
                .startDate(LocalDate.now())
                .planName("Silver")
                .trainerName("Rahul")
                .build();
        context.setVariable("gym", gymDTO);
        context.setVariable("member", memberDTO);
        context.setVariable("invoice", invoiceDTO);
        context.setVariable("payment", paymentDTO);
        context.setVariable("membership", membershipDTO);
//        return templateEngine.process(template, context);
        return null;
    }

    @GetMapping(value = "/preview-all")
    public ResponseEntity<?> previewAllTemplatesAsList(@RequestParam("o") Integer ownerId) {
        return new ResponseEntity<>(invoiceTemplateService.getInvoiceTemplatesOfOwner(ownerId),HttpStatus.ACCEPTED);
    }

}