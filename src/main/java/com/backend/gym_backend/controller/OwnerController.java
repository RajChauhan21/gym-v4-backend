package com.backend.gym_backend.controller;

import com.backend.gym_backend.dto.AuthRequest;
import com.backend.gym_backend.dto.OwnerDetailsRequest;
import com.backend.gym_backend.dto.UserRequest;
import com.backend.gym_backend.entity.RefreshToken;
import com.backend.gym_backend.repo.RefreshTokenRepository;
import com.backend.gym_backend.security.CookieUtil;
import com.backend.gym_backend.service.JwtService;
import com.backend.gym_backend.service.OwnerService;
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

import java.time.Instant;
import java.time.LocalDate;

@CrossOrigin("*")
@RestController
@RequestMapping("/owner")
public class OwnerController {

    @Autowired
    private OwnerService ownerService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private JwtService jwtService;

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

    @PostMapping("/save")
    public ResponseEntity<?> saveOwner(@RequestBody OwnerDetailsRequest detailsRequestDto) {
        return new ResponseEntity<>(ownerService.save(detailsRequestDto), HttpStatus.ACCEPTED);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateOwner(@RequestBody OwnerDetailsRequest detailsRequestDto) {
        return new ResponseEntity<>(ownerService.update(detailsRequestDto), HttpStatus.ACCEPTED);
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
                                                  @RequestParam(value = "expiryTo",required = false) LocalDate expiryTo,Pageable pageable) {
        return new ResponseEntity<>(ownerService.getAllMembersOfOwner(ownerId,name,dueAmount,joinedFrom,joinedTo,expiryFrom,expiryTo,pageable), HttpStatus.ACCEPTED);
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
