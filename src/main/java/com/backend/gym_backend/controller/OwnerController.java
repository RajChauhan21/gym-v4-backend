package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.AuthRequest;
import com.backend.gym_backend.dto.OwnerDetailsRequest;
import com.backend.gym_backend.dto.UserRequest;
import com.backend.gym_backend.entity.RefreshToken;
import com.backend.gym_backend.repo.RefreshTokenRepository;
import com.backend.gym_backend.security.CookieUtil;
import com.backend.gym_backend.service.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/owner")
public class OwnerController {

    @Autowired
    private OwnerService ownerService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private OtpEmailService otpEmailService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication authentication) {
        String email = authentication.getName();
        return new ResponseEntity<>(ownerService.findById(Math.toIntExact(Long.parseLong(email))), HttpStatus.ACCEPTED);
    }

    @PostMapping("/signup")
    public ResponseEntity<String> signUp(@RequestBody UserRequest userRequest) {
        return new ResponseEntity<>(ownerService.signUp(userRequest), HttpStatus.ACCEPTED);
    }

    @PostMapping("/login")
    public ResponseEntity<?> logIn(@RequestBody AuthRequest userRequest, HttpServletResponse response) throws Exception {
        return new ResponseEntity<>(ownerService.logIn(userRequest, response), HttpStatus.ACCEPTED);
    }

    @GetMapping("/active-subscription")
    public ResponseEntity<?> activeSubscriptionOfOwner(@RequestParam("q") Integer ownerId){
        return new ResponseEntity<>(ownerService.getOwnerActiveSubscription(ownerId),HttpStatus.ACCEPTED);
    }

    @Transactional
    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpServletResponse response, HttpServletRequest request) {
        String refreshToken = CookieUtil.extractCookie(request, "refreshToken");
        if (refreshToken != null) {
            refreshTokenRepository.deleteByToken(refreshToken);
        }
        CookieUtil.clearCookies(response);
        System.out.println("Log out endpoint called");
        return ResponseEntity.ok("Logged out");
    }

    @GetMapping("/resetPassword")
    public ResponseEntity<?> resetPassword(@RequestParam("q") Integer ownerId, @RequestParam("p") String password){
        return new ResponseEntity<>(ownerService.resetPassword(ownerId,password),HttpStatus.ACCEPTED);
    }

    @GetMapping("/sendOtp")
    public ResponseEntity<?> resetPassword(@RequestParam("q") String email){
        return new ResponseEntity<>(otpEmailService.sendPasswordResetOtp(email),HttpStatus.ACCEPTED);
    }

    @GetMapping("/verifyOtp")
    public ResponseEntity<?> verifyOtp(@RequestParam("e") String email, @RequestParam("q") String otp){
        return new ResponseEntity<>(otpEmailService.verifyOtp(email,otp),HttpStatus.ACCEPTED);
    }

    @PostMapping("upload/image")
    public ResponseEntity<?> uploadImageForOwner(@RequestParam("q") int id,@RequestParam("t") String target, @RequestParam("file") MultipartFile file) throws IOException {
        return cloudinaryService.uploadImage(id,target,file);
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveOwner(@RequestBody OwnerDetailsRequest detailsRequestDto) {
        return new ResponseEntity<>(ownerService.save(detailsRequestDto), HttpStatus.ACCEPTED);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateOwner(@RequestBody OwnerDetailsRequest detailsRequestDto) {
        return new ResponseEntity<>(ownerService.update(detailsRequestDto), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getDuesOfMembers")
    public ResponseEntity<?> getDueAmountOfAllMembersOfOwner(@RequestParam("q") Integer ownerId){
        return new ResponseEntity<>(ownerService.getTotalDueAmountAndCountOfMembersOfOwner(ownerId),HttpStatus.ACCEPTED);
    }

    @GetMapping("/findById")
    public ResponseEntity<?> findOwnerById(@RequestParam("q") Integer id) {
        return new ResponseEntity<>(ownerService.findById(id), HttpStatus.ACCEPTED);
    }

    @GetMapping("/findAll")
    public ResponseEntity<?> findAllOwners() {
        return new ResponseEntity<>(ownerService.findAllOwners(), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getAllMembersOfOwner")
    public ResponseEntity<?> getAllMembersOfOwner(@RequestParam("q") Integer ownerId, @RequestParam(value = "name",required = false) String name,
                                                  @RequestParam(value = "dueAmount",required = false) Integer dueAmount,
                                                  @RequestParam(value = "joinedFrom",required = false) LocalDate joinedFrom,
                                                  @RequestParam(value = "joinedTo",required = false) LocalDate joinedTo,
                                                  @RequestParam(value = "expiryFrom",required = false) LocalDate expiryFrom,
                                                  @RequestParam(value = "plan",required = false) String plan,
                                                  @RequestParam(value = "expiryTo",required = false) LocalDate expiryTo,Pageable pageable) {
        return new ResponseEntity<>(ownerService.getAllMembersOfOwner(ownerId,name,dueAmount,joinedFrom,joinedTo,expiryFrom,expiryTo, plan,pageable), HttpStatus.ACCEPTED);
    }

    @GetMapping("/getAllMembersCount")
    public ResponseEntity<?> getAllMembersCount(@RequestParam("q") Integer ownerId){
        return new ResponseEntity<>(memberService.getAllMembersCount(ownerId),HttpStatus.ACCEPTED);
    }

    @GetMapping("/getAllPaymentsOwner")
    public ResponseEntity<?> getAllPaymentsOfOwner(@RequestParam("q") Integer ownerId,
                                                   @RequestParam(value = "amount",required = false) BigDecimal amount,
                                                   @RequestParam(value = "status",required = false) String status,
                                                   @RequestParam(value = "method",required = false) String method,
                                                   @RequestParam(value = "startDate",required = false) LocalDateTime createdAt,
                                                   @RequestParam(value = "endDate",required = false) LocalDateTime endAt,
                                                   Pageable pageable){
        return new ResponseEntity<>(ownerService.getAllPaymentsOfOwner(ownerId,amount,status,method,createdAt,endAt,pageable),HttpStatus.ACCEPTED);
    }

    @DeleteMapping("/deleteById")
    public ResponseEntity<?> deleteOwnerById(@RequestParam("q") Integer id) {
        return new ResponseEntity<>(ownerService.deleteById(id), HttpStatus.ACCEPTED);
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request,
                                          HttpServletResponse response) {

        String refreshToken = CookieUtil.extractCookie(request, "refreshToken");

        RefreshToken token = refreshTokenRepository
                .findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (token.getExpiryTime().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        String newAccessToken = jwtService.generateToken(token.getOwner());

        Cookie accessCookie = new Cookie("jwt", newAccessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(15 * 60);

        response.addCookie(accessCookie);

        return ResponseEntity.ok("Token refreshed");
    }
}
