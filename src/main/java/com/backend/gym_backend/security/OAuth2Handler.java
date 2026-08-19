package com.backend.gym_backend.security;

import com.backend.gym_backend.dto.OAuth2UserInfo;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.entity.RefreshToken;
import com.backend.gym_backend.enums.OAuthProvider;
import com.backend.gym_backend.repo.OwnerRepository;
import com.backend.gym_backend.repo.RefreshTokenRepository;
import com.backend.gym_backend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;

@Component
public class OAuth2Handler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OwnerRepository ownerRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;


    private final ObjectMapper mapper = new ObjectMapper();

    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        //1.find provider
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String provider = token.getAuthorizedClientRegistrationId();
        System.out.println("provider " + provider);

        //2.get details and attributes from provider
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 3. Map to your POJO based on Provider
        OAuth2UserInfo userInfo = mapToUserInfo(attributes, provider);

        // 4. Find or Create Owner in DB
        Owner owner = ownerRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> createNewOwner(userInfo, provider));

        // 5. Generate JWT and Redirect
        String jwt = jwtService.generateToken(owner);
        String refreshToken = jwtService.generateRefreshToken();

        RefreshToken rt = refreshTokenRepository.findByOwner(owner)
                .orElse(new RefreshToken());

        rt.setOwner(owner);
        rt.setToken(refreshToken);
        rt.setUpdatedAt(LocalDateTime.now());
        if (rt.getCreatedAt()==null){
            rt.setCreatedAt(LocalDateTime.now());
        }
        rt.setExpiryTime(Instant.now().plus(7, ChronoUnit.DAYS));

        refreshTokenRepository.save(rt);

        CookieUtil.createJwtCookie(response, jwt, refreshToken);
        response.sendRedirect("http://localhost:5173/dashboard?login=success");
    }

    private OAuth2UserInfo mapToUserInfo(Map<String, Object> attributes, String provider) {
        OAuth2UserInfo userInfo = mapper.convertValue(attributes, OAuth2UserInfo.class);
        userInfo.setProvider(provider);

        // Fix ID mapping (Google uses 'sub', others use 'id')
        if ("google".equals(provider)) {
            userInfo.setId((String) attributes.get("sub"));
        } else {
            userInfo.setId((String) attributes.get("id"));
        }
        return userInfo;
    }

    private Owner createNewOwner(OAuth2UserInfo userInfo, String provider) {
        Owner owner = new Owner();

        owner.setProviderId(userInfo.getId());
        owner.setProvider(Objects.equals(provider, "google") ? OAuthProvider.GOOGLE : Objects.equals(provider, "facebook") ? OAuthProvider.FACEBOOK : OAuthProvider.INSTAGRAM);
        owner.setName(userInfo.getName());
        owner.setEmail(userInfo.getEmail());
        owner.setImage(userInfo.getPicture());
        owner.setCreatedAt(LocalDateTime.now());
        owner.setUpdatedAt(LocalDateTime.now());
        owner.setPassword(null);

        return ownerRepository.save(owner);
    }
}
