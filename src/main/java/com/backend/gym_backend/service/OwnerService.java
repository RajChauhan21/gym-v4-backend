package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.*;
import com.backend.gym_backend.entity.*;
import com.backend.gym_backend.enums.OAuthProvider;
import com.backend.gym_backend.enums.SubscriptionStatus;
import com.backend.gym_backend.repo.*;
import com.backend.gym_backend.security.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OwnerService {

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private OwnerPaymentRepository ownerPaymentRepository;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;


    public ResponseEntity<?> logIn(AuthRequest userRequest, HttpServletResponse response) throws Exception {
        if (ownerRepository.findByEmail(userRequest.getEmail()).isEmpty()) {
            throw new RuntimeException("Email not found");
        }
        Owner owner = ownerRepository.findByEmail(userRequest.getEmail()).get();
        if (owner.getProvider() != OAuthProvider.LOCAL) {
            throw new RuntimeException("Please consider login using google, facebook or instagram");
        }
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userRequest.getEmail(), userRequest.getPassword()));

        if (authenticate.isAuthenticated()) {
            String token = jwtService.generateToken(owner);
            String refreshToken = jwtService.generateRefreshToken();
            CookieUtil.createJwtCookie(response, token, refreshToken);

            RefreshToken rt = refreshTokenRepository.findByOwner(owner)
                    .orElse(new RefreshToken());

            rt.setOwner(owner);
            rt.setToken(refreshToken);
            rt.setExpiryTime(Instant.now().plus(7, ChronoUnit.DAYS));
            refreshTokenRepository.save(rt);
            Subscription subscription = subscriptionRepository.findFirstByOwner_IdAndStatusOrderByCreatedAtDesc(owner.getId(), SubscriptionStatus.ACTIVE).orElse(null);

// 2. Safely extract the plan from the subscription
            Plan plan = (subscription != null) ? subscription.getPlan() : null;

// 3. Build the response with null-safety for subscription fields
            OwnerDetailsResponse responseDto = OwnerDetailsResponse.builder()
                    .website(owner.getGym() != null ? owner.getGym().getWebsite() : "")
                    .email(owner.getEmail())
                    .ownerImage(owner.getImage())
                    .gymImage(owner.getGym() != null ? owner.getGym().getImage() : "")
                    .phone(owner.getPhone())
                    .googleMapUrl(owner.getGym() != null ? owner.getGym().getGoogleMapUrl() : "")
                    .ownerName(owner.getName())
                    .gymName(owner.getGym() != null ? owner.getGym().getName() : "")
                    .location(owner.getGym() != null ? owner.getGym().getLocation() : "")
                    .ownerId(owner.getId())
                    .gymId(owner.getGym() != null ? owner.getGym().getId() : null)
                    // Subscription fields (null-safe)
                    .price(subscription != null ? subscription.getPrice() : 0)
                    .endDate(subscription != null ? subscription.getEndDate() : null)
                    .startDate(subscription != null ? subscription.getStartDate() : null)
                    .planName(subscription != null ? subscription.getName() : "No Active Plan")
                    .subscriptionStatus(subscription != null ? subscription.getStatus() : null)
                    // Plan fields (null-safe)
                    .memberLimitCount(plan != null ? plan.getMemberLimit() : 0)
                    .currentMemberCount(memberRepository.countAllMembersByOwnerId(owner.getId()))
                    .build();

            return ResponseEntity.ok(responseDto);
        }

        throw new RuntimeException("please check your credentials");
    }

    @Transactional
    public String signUp(UserRequest userRequest) {
        Optional<Owner> ownerObj = ownerRepository.findByEmail(userRequest.getEmail());

        if (ownerObj.isPresent()) {
            throw new RuntimeException("Email already present " + userRequest.getEmail());
        }
        String password = bCryptPasswordEncoder.encode(userRequest.getPassword());
        Owner owner = new Owner();
        Plan trial = planRepository.findByName("Trial").get();
        owner.setProvider(OAuthProvider.LOCAL);
        owner.setName(userRequest.getName());
        owner.setPassword(password);
        owner.setEmail(userRequest.getEmail());
        Owner save = ownerRepository.save(owner);

//        subscriptionService.ownerSubscribesToPlan(save.getId(), trial.getId(), password, password);

        return "Account created successfully";
    }


    @Transactional
    public OwnerDetailsResponse save(OwnerDetailsRequest ownerDetailsRequestDto) {
        Owner owner = Owner.builder()
                .id(null)
                .phone(ownerDetailsRequestDto.getPhone())
                .email(ownerDetailsRequestDto.getEmail())
                .name(ownerDetailsRequestDto.getOwnerName())
                .build();

        Owner save = ownerRepository.save(owner);

        return OwnerDetailsResponse.builder()
                .ownerId(save.getId())
                .ownerName(save.getName())
                .email(save.getEmail())
                .phone(save.getPhone())
                .build();
    }

    @Transactional
    public OwnerDetailsResponse update(OwnerDetailsRequest ownerDetailsRequestDto) {
        if (subscriptionRepository.findFirstByOwner_IdAndStatusOrderByCreatedAtDesc(ownerDetailsRequestDto.getOwnerId(), SubscriptionStatus.ACTIVE).isEmpty()){
            throw new RuntimeException("100");
        }
        Owner owner = ownerRepository.findById(ownerDetailsRequestDto.getOwnerId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        owner = Owner.builder()
                .id(ownerDetailsRequestDto.getOwnerId())
                .phone(ownerDetailsRequestDto.getPhone())
                .email(ownerDetailsRequestDto.getEmail())
                .name(ownerDetailsRequestDto.getOwnerName())
                .build();

//        Owner save = ownerRepository.save(owner);
        return OwnerDetailsResponse.builder()
                .ownerId(owner.getId())
                .ownerName(owner.getName())
                .email(owner.getEmail())
                .phone(owner.getPhone())
                .build();
    }

    public OwnerDetailsResponse findById(int id) {
        if (!ownerRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        Owner owner = ownerRepository.findById(id).get();
        Subscription subscription = subscriptionRepository.findFirstByOwner_IdAndStatusOrderByCreatedAtDesc(owner.getId(), SubscriptionStatus.ACTIVE).orElse(null);
        Plan plan = (subscription != null) ? subscription.getPlan() : null;
        OwnerDetailsResponse responseDto = OwnerDetailsResponse.builder()
                .website(owner.getGym() != null ? owner.getGym().getWebsite() : "")
                .email(owner.getEmail())
                .ownerImage(owner.getImage())
                .gymImage(owner.getGym() != null ? owner.getGym().getImage() : "")
                .phone(owner.getPhone())
                .googleMapUrl(owner.getGym() != null ? owner.getGym().getGoogleMapUrl() : "")
                .ownerName(owner.getName())
                .gymName(owner.getGym() != null ? owner.getGym().getName() : "")
                .location(owner.getGym() != null ? owner.getGym().getLocation() : "")
                .ownerId(owner.getId())
                .gymId(owner.getGym() != null ? owner.getGym().getId() : null)
                // Subscription fields (null-safe)
                .price(subscription != null ? subscription.getPrice() : 0)
                .endDate(subscription != null ? subscription.getEndDate() : null)
                .startDate(subscription != null ? subscription.getStartDate() : null)
                .planName(subscription != null ? subscription.getName() : "No Active Plan")
                .subscriptionStatus(subscription != null ? subscription.getStatus() : null)
                // Plan fields (null-safe)
                .memberLimitCount(plan != null ? plan.getMemberLimit() : 0)
                .currentMemberCount(memberRepository.countAllMembersByOwnerId(owner.getId()))
                .build();

        return responseDto;
    }

    public List<OwnerDetailsResponse> findAllOwners() {
        List<Owner> all = ownerRepository.findAll();
        List<OwnerDetailsResponse> owners = new ArrayList<>();

        for (Owner o : all) {
            OwnerDetailsResponse responseDto = OwnerDetailsResponse.builder()
                    .website(o.getGym().getWebsite())
                    .email(o.getEmail())
                    .phone(o.getPhone())
                    .googleMapUrl(o.getGym().getGoogleMapUrl())
                    .ownerName(o.getName())
                    .gymName(o.getGym().getName())
                    .location(o.getGym().getLocation())
                    .ownerId(o.getId())
                    .gymId(o.getGym().getId())
                    .build();

            owners.add(responseDto);
        }

        return owners;
    }

    public PaymentDueResponse getTotalDueAmountAndCountOfMembersOfOwner(Integer ownerId) {
        List<MemberResponse> members = new ArrayList<>();
        List<Member> byOwnerId = memberRepository.findByOwnerId(ownerId);
        long sum = 0;
        long count = 0;
        for (Member m : byOwnerId) {
            sum += m.getDueAmount();
            if (m.getDueAmount() > 0) {
                count++;
            }
        }
        return new PaymentDueResponse(sum, count);
    }

    public Page<MemberProjection> getAllMembersOfOwner(Integer ownerId, String name, Integer dueAmount, LocalDate joinedFrom, LocalDate joinedTo, LocalDate expiryFrom, LocalDate expiryTo, String plan, Pageable pageable) {
        if (subscriptionRepository.findFirstByOwner_IdAndStatusOrderByCreatedAtDesc(ownerId, SubscriptionStatus.ACTIVE).isEmpty()){
            throw new RuntimeException("100");
        }
        return memberRepository.findAllMembersByOwnerId(Long.valueOf(ownerId), name, dueAmount, joinedFrom, joinedTo, expiryFrom, expiryTo, plan, pageable);
    }

    public OwnerDetailsResponse findByEmail(String email) {
        if (ownerRepository.findByEmail(email).isEmpty()) {
            throw new RuntimeException("Email not found");
        }
        Owner owner = ownerRepository.findByEmail(email).get();
        OwnerDetailsResponse responseDto = OwnerDetailsResponse.builder()
                .website(owner.getGym() != null ? owner.getGym().getWebsite() : "")
                .email(owner.getEmail())
                .ownerImage(owner.getImage())
                .gymImage(owner.getGym() != null ? owner.getGym().getImage() : "")
                .phone(owner.getPhone())
                .googleMapUrl(owner.getGym() != null ? owner.getGym().getGoogleMapUrl() : "")
                .ownerName(owner.getName())
                .gymName(owner.getGym() != null ? owner.getGym().getName() : "")
                .location(owner.getGym() != null ? owner.getGym().getLocation() : "")
                .ownerId(owner.getId())
                .gymId(owner.getGym() != null ? owner.getGym().getId() : null)
                .build();

        return responseDto;
    }

    public Page<OwnerPaymentProjection> getAllPaymentsOfOwner(Integer ownerId, BigDecimal amount, String status, String method, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        com.backend.gym_backend.enums.Payment payment = null;
        if (status != null && status.equals("SUCCESS")) {
            payment = com.backend.gym_backend.enums.Payment.CAPTURED;
        }
        if (status != null && status.equals("FAILED")) {
            payment = com.backend.gym_backend.enums.Payment.FAILED;
        }
        Page<OwnerPaymentProjection> byOwner = ownerPaymentRepository.findPaymentsByOwner(ownerId, amount, payment, method, startDate, endDate, pageable);

        return byOwner;
    }

    @Transactional
    public String resetPassword(Integer ownerId, String password) {
        Optional<Owner> owner = ownerRepository.findById(ownerId);
        if (owner.isEmpty()) {
            throw new RuntimeException("Owner not found");
        }
        otpRepository.deleteByOwnerEmail(owner.get().getEmail());
        otpRepository.flush();
        password = bCryptPasswordEncoder.encode(password);
        owner.get().setPassword(password);
        ownerRepository.save(owner.get());

        return "Password reset successfully";
    }

    @Transactional
    public String deleteById(int id) {
        if (!ownerRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        ownerRepository.deleteById(id);
        return "deleted";
    }


    public SubscriptionResponse getOwnerActiveSubscription(Integer ownerId){
        Optional<Subscription> subscription = subscriptionRepository.findFirstByOwner_IdAndStatusOrderByCreatedAtDesc(ownerId, SubscriptionStatus.ACTIVE);

        if (subscription.isEmpty()){
            return SubscriptionResponse.builder().build();
        }

        return SubscriptionResponse.builder()
                .id(subscription.get().getId())
                .endDate(subscription.get().getEndDate())
                .startDate(subscription.get().getStartDate())
                .subscriptionStatus(subscription.get().getStatus())
                .price(subscription.get().getPrice())
                .name(subscription.get().getName())
                .build();
    }

}
