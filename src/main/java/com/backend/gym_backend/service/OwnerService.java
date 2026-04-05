package com.backend.gym_backend.service;

import com.backend.gym_backend.dto.*;
import com.backend.gym_backend.entity.Member;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.entity.RefreshToken;
import com.backend.gym_backend.enums.OAuthProvider;
import com.backend.gym_backend.repo.GymRepository;
import com.backend.gym_backend.repo.MemberRepository;
import com.backend.gym_backend.repo.OwnerRepository;
import com.backend.gym_backend.repo.RefreshTokenRepository;
import com.backend.gym_backend.security.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OwnerService {

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private GymRepository gymRepository;

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

            OwnerDetailsResponse responseDto = OwnerDetailsResponse.builder()
                    .website(owner.getGym() != null ? owner.getGym().getWebsite() : "")
                    .email(owner.getEmail())
                    .phone(owner.getPhone())
                    .googleMapUrl(owner.getGym() != null ? owner.getGym().getGoogleMapUrl() : "")
                    .ownerName(owner.getName())
                    .gymName(owner.getGym() != null ? owner.getGym().getName() : "")
                    .location(owner.getGym() != null ? owner.getGym().getLocation() : "")
                    .ownerId(owner.getId())
                    .gymId(owner.getGym() != null ? owner.getGym().getId() : null)
                    .gymImage(owner.getGym() !=null ? owner.getGym().getImage() : "")
                    .ownerImage(owner.getImage())
                    .build();

            return ResponseEntity.ok(responseDto);
        }

        throw new RuntimeException("please check your credentials");
    }

    @Transactional
    public String signUp(UserRequest userRequest) {
        Optional<Owner> ownerObj = ownerRepository.findByEmail(userRequest.getEmail());

        if (ownerObj.isPresent()) {
            throw new RuntimeException(userRequest.getEmail());
        }
        String password = bCryptPasswordEncoder.encode(userRequest.getPassword());
        Owner owner = new Owner();
        owner.setProvider(OAuthProvider.LOCAL);
        owner.setName(userRequest.getName());
        owner.setPassword(password);
        owner.setEmail(userRequest.getEmail());
        ownerRepository.save(owner);

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
    public OwnerDetailsRequest update(OwnerDetailsRequest ownerDetailsRequestDto) {
        if (!ownerRepository.existsById(ownerDetailsRequestDto.getOwnerId())) {
            throw new RuntimeException("User not found");
        }
        Owner owner = Owner.builder()
                .id(ownerDetailsRequestDto.getOwnerId())
                .phone(ownerDetailsRequestDto.getPhone())
                .email(ownerDetailsRequestDto.getEmail())
                .name(ownerDetailsRequestDto.getOwnerName())
                .build();

        ownerRepository.save(owner);
        return ownerDetailsRequestDto;
    }

    public OwnerDetailsResponse findById(int id) {
        if (!ownerRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        Owner owner = ownerRepository.findById(id).get();
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

    public List<MemberResponse> getAllMembersOfOwner(Integer ownerId) {
        List<MemberResponse> members = new ArrayList<>();
        List<Member> byOwnerId = memberRepository.findByOwnerId(ownerId);
        for (Member m : byOwnerId) {
            MemberResponse build = MemberResponse.builder()
                    .expiry(m.getExpiry())
                    .name(m.getName())
                    .joined(m.getJoined())
                    .id(m.getId())
                    .dueAmount(m.getDueAmount())
                    .email(m.getEmail())
                    .address(m.getAddress())
                    .phone(m.getPhone())
                    .ownerId(m.getOwner().getId())
                    .plan(m.getMemberShip()!=null ? m.getMemberShip().getName() : "N/A")
                    .build();

            members.add(build);
        }
        return members;
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


    @Transactional
    public String deleteById(int id) {
        if (!ownerRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        ownerRepository.deleteById(id);
        return "deleted";
    }

}
