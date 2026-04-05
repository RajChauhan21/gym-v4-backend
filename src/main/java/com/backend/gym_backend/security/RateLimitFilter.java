package com.backend.gym_backend.security;

import com.backend.gym_backend.service.RateLimitService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RateLimitFilter implements Filter {

    private final RateLimitService rateLimitService;

    @Autowired
    public RateLimitFilter(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        String key;

        if (path.equals("/owner/login") || path.equals("/owner/signup")) {

            key = request.getRemoteAddr(); // prevent brute force

        } else {

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {

                key = auth.getName();

            } else {

                key = request.getRemoteAddr();
            }
        }
        System.out.println("key " + key);
        Bucket bucket = rateLimitService.resolveBucket(key);

        if (bucket.tryConsume(1)) { //consume 1 token

            chain.doFilter(request, response);

        } else {

//            httpResponse.setStatus(429);
//            httpResponse.getWriter().write("Too many requests");
            throw new RuntimeException("Too many requests");
        }
    }
}