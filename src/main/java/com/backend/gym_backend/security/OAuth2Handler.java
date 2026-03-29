package com.backend.gym_backend.security;

import com.backend.gym_backend.dto.OAuth2UserInfo;
import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.enums.OAuthProvider;
import com.backend.gym_backend.repo.OwnerRepository;
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
import java.util.Map;
import java.util.Objects;

@Component
public class OAuth2Handler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OwnerRepository ownerRepository;

    private final ObjectMapper mapper = new ObjectMapper();

    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        //1.find provider
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String provider = token.getAuthorizedClientRegistrationId();
        System.out.println("provider " + provider);

        //2.get details and attributes from provider
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        System.out.println(attributes);

        // 3. Map to your POJO based on Provider
        OAuth2UserInfo userInfo = mapToUserInfo(attributes, provider);
        System.out.println(userInfo);

        // 4. Find or Create Owner in DB
        Owner owner = ownerRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> createNewOwner(userInfo, provider));

        // 5. Generate JWT and Redirect
        String jwt = jwtService.generateToken(owner);
        CookieUtil.createJwtCookie(response,jwt);
        response.sendRedirect("http://localhost:5173/dashboard");
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
        owner.setProvider(Objects.equals(provider, "google") ?OAuthProvider.GOOGLE: Objects.equals(provider, "facebook") ?OAuthProvider.FACEBOOK:OAuthProvider.INSTAGRAM);
        owner.setName(userInfo.getName());
        owner.setEmail(userInfo.getEmail());
        owner.setPassword(null);

        return ownerRepository.save(owner);
    }
}
