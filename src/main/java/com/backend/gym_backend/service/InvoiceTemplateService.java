package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.*;
import com.backend.gym_backend.entity.*;
import com.backend.gym_backend.enums.SubscriptionStatus;
import com.backend.gym_backend.repo.PaymentRepository;
//import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.backend.gym_backend.repo.SubscriptionRepository;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class InvoiceTemplateService {
//    OpenHTMLToPDF requires valid XHTML.
    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    public byte[] generateInvoice(Integer paymentId) throws Exception {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("payment not found"));

        InvoicePdfDTO dto = buildInvoicePdfDTO(payment);

        Context context = new Context();
        System.out.println(dto.getGym().getLogoUrl());

        context.setVariable("gym", dto.getGym());

        context.setVariable("member", dto.getMember());

        context.setVariable("membership", dto.getMembership());

        context.setVariable("payment", dto.getPayment());

        context.setVariable("invoice", dto.getInvoice());

        String html = templateEngine.process(
                "invoice-corporate-business",
                context
//                invoice-modern-card, invoice-apple-minimal, invoice-corporate-business
                //invoice-fitness-green, invoice-luxury-executive, invoice-material-light
                // invoice-soft-blue-saas,
        );

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        PdfRendererBuilder builder = new PdfRendererBuilder();

        builder.withHtmlContent(html, null);

        builder.toStream(output);

        builder.run();
        System.out.println("html " + html);

        return output.toByteArray();
//        return html.getBytes();
    }

    private InvoicePdfDTO buildInvoicePdfDTO(Payment payment) {

        Member member = null;
        MemberShip membership = null;
        Gym gym = null;
        if (payment != null && payment.getMember() != null) {
            member = payment.getMember();
        }

        if (member != null && member.getMemberShip() != null) {
            membership = member.getMemberShip();
        }

        if (membership != null && membership.getGym() != null) {
            gym = membership.getGym();
        }

        InvoicePdfDTO dto = new InvoicePdfDTO();

        dto.setGym(buildGymDTO(gym));

        dto.setMember(buildMemberDTO(member));

        dto.setMembership(buildMembershipDTO(membership,member));

        dto.setPayment(buildPaymentDTO(payment, member, membership));

        dto.setInvoice(buildInvoiceDTO(payment));

        return dto;
    }

    private GymDTO buildGymDTO(Gym gym) {

        GymDTO dto = new GymDTO();
        Owner owner = null;
        if (gym!=null && gym.getOwner()!=null){
            owner = gym.getOwner();
        }

        dto.setName(gym.getName());

        dto.setAddress(gym.getLocation());

        dto.setPhone(owner!=null && owner.getPhone()!=null?owner.getPhone():"N/A");

        dto.setEmail(owner!=null && owner.getEmail()!=null?owner.getEmail():"N/A");

        dto.setWebsite(gym.getWebsite());

        dto.setGstNumber("N/A");

        dto.setLogoUrl(gym.getImage()!=null ? gym.getImage():"N/A");

        dto.setThankYouMessage("Thank you!!!!");

        dto.setTerms("N/A");

        dto.setSignatureName(owner!=null? owner.getName():"N/A");

        return dto;
    }

    private MemberDTO buildMemberDTO(Member member) {

        MemberDTO dto = new MemberDTO();

        dto.setMemberId(String.valueOf(member.getId()));

        dto.setName(member.getName());

        dto.setPhone(member.getPhone());

        dto.setEmail(member.getEmail());

        return dto;
    }

    private MembershipDTO buildMembershipDTO(MemberShip membership,Member member) {

        MembershipDTO dto = new MembershipDTO();

        dto.setPlanName(membership.getName());

        dto.setDuration(membership.getValidity()/30 + " Months");

        dto.setStartDate(member.getStartDate());

        dto.setExpiryDate(member.getExpiry());

        dto.setTrainerName(
                "N/A"
        );
        return dto;
    }

    private PaymentDTO buildPaymentDTO(Payment payment, Member member, MemberShip memberShip) {

        PaymentDTO dto = new PaymentDTO();

        dto.setSubtotal(memberShip.getPrice());

        dto.setDiscount(BigDecimal.ZERO);

        dto.setGst(BigDecimal.ZERO);

        dto.setTotal(memberShip.getPrice());

        dto.setPaidAmount(payment.getAmountPaid());

        dto.setDueAmount(member.getDueAmount());

        dto.setPaymentMethod(payment.getMethod());

        dto.setTransactionId("N/A");

        return dto;
    }

    private InvoiceDTO buildInvoiceDTO(Payment payment) {

        InvoiceDTO dto = new InvoiceDTO();

        dto.setInvoiceNumber(String.valueOf(payment.getId()));

        dto.setInvoiceDate(LocalDate.now());

        dto.setPaymentDate(payment.getDate());

        return dto;
    }

    public List<InvoiceTemplateResponse> getInvoiceTemplatesOfOwner(Integer ownerId){

        // 1. Get the assigned subscription tier folder (e.g., "basic", "premium")
        Optional<Subscription> subscription = subscriptionRepository.findFirstByOwner_IdAndStatusInOrderByCreatedAtDesc(ownerId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.PARTIALLY_ACTIVE));

        String folderName = "";

        if (subscription.isEmpty()) {
            throw new RuntimeException("403");
        }
        if (subscription.get().getName().equals("Max Pro")){
            folderName = "max-pro";
        }
        else{
            folderName = "basic";
        }

        // 2. Scan the directory to find all available template names
        List<String> templateNames = new ArrayList<>();
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            URL resource = classLoader.getResource("templates/" + folderName);
            if (resource != null) {
                File folder = new File(resource.toURI());
                File[] listOfFiles = folder.listFiles();
                if (listOfFiles != null) {
                    for (File file : listOfFiles) {
                        if (file.isFile() && file.getName().endsWith(".html")) {
                            // Strip the ".html" extension for Thymeleaf
                            String nameWithoutExt = file.getName().substring(0, file.getName().length() - 5);
                            templateNames.add(nameWithoutExt);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("404");
        }

        if (templateNames.isEmpty()) {
            throw new RuntimeException("400");
        }

        // 3. Prepare your common contextual dynamic variables
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


        //  Loop through each template file, process it, and append it to the list
        List<InvoiceTemplateResponse> responses = new ArrayList<>();
        int count = 1;
        for (String currentTemplate : templateNames) {
            String fullTemplatePath = folderName + "/" + currentTemplate;

            // Process the template into raw HTML text
            String renderedHtml = templateEngine.process(fullTemplatePath, context);

            InvoiceTemplateResponse build = InvoiceTemplateResponse.builder()
                    .id(count++)
                    .name(getTemplateName(currentTemplate))
                    .description(getTemplateDescription(currentTemplate))
                    .previewUrl(renderedHtml)
                    .featured(getIsTemplateFeatured(currentTemplate))
                    .category(getTemplateCategory(currentTemplate))
                    .build();

            responses.add(build);
        }

        // 6. Return the clean list. Spring Boot automatically converts this to a JSON array.
        return responses;
    }

    private String getTemplateDescription(String templateName) {
        return switch (templateName) {
            case "invoice-chalk-light" -> "High quality multi column template";
            case "invoice-corporate-business" -> "Best template for businesses.";
            case "invoice-luxury-executive" -> "Professional corporate invoice.";
            case "invoice-premium" -> "Premium template";
            case "invoice-apple-minimal" -> "Simple and sober one";
            case "invoice-fitness-green" -> "Our large header green template.";
            case "invoice-material-light" -> "Inspired by Stripe & Razorpay.";
            case "invoice-modern-card" -> "Minimal receipt with elegant cards.";
            case "invoice-soft-blue-saas" -> "Our flagship premium template.";
            case "invoice-ticket-light" -> "High quality multi column template";
            default -> throw new IllegalStateException("Unexpected value: " + templateName);
        };
    }


    private String getTemplateName(String templateName) {
        return switch (templateName) {
            case "invoice-chalk-light" -> "Paper light template";
            case "invoice-corporate-business" -> "Wild Template";
            case "invoice-luxury-executive" -> "Executive Luxury Blue";
            case "invoice-premium" -> "Premium template";
            case "invoice-apple-minimal" -> "Simple and sober one";
            case "invoice-fitness-green" -> "Elite Premium";
            case "invoice-material-light" -> "Lightweight Minimal";
            case "invoice-modern-card" -> "Modern Card";
            case "invoice-soft-blue-saas" -> "Soft Blue SaaS";
            case "invoice-ticket-light" -> "Light Magic";
            default -> throw new IllegalStateException("Unexpected value: " + templateName);
        };
    }

    private boolean getIsTemplateFeatured(String templateName) {
        return switch (templateName) {
            case "invoice-chalk-light", "invoice-soft-blue-saas", "invoice-material-light", "invoice-apple-minimal",
                 "invoice-luxury-executive" -> true;
            case "invoice-corporate-business", "invoice-fitness-green", "invoice-modern-card", "invoice-ticket-light",
                 "invoice-premium" -> false;
            default -> throw new IllegalStateException("Unexpected value: " + templateName);
        };
    }

    private String getTemplateCategory(String templateName) {
        return switch (templateName) {
            case "invoice-chalk-light", "invoice-ticket-light", "invoice-fitness-green", "invoice-premium",
                 "invoice-luxury-executive" -> "Premium";
            case "invoice-corporate-business", "invoice-soft-blue-saas", "invoice-material-light" -> "Corporate";
            case "invoice-apple-minimal", "invoice-modern-card" -> "Modern";
            default -> throw new IllegalStateException("Unexpected value: " + templateName);
        };
    }

}
