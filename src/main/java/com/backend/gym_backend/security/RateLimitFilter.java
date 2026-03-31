package com.backend.gym_backend.security;

import com.backend.gym_backend.service.RateLimitService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Autowired;
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

        String key; //get ip address from request

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication!=null && authentication.isAuthenticated()){
            key = authentication.getName();
        }
        else{
            key = httpRequest.getRemoteAddr();
        }
        System.out.println(key);
        Bucket bucket = rateLimitService.resolveBucket(key);

        if (bucket.tryConsume(1)) { //consume 1 token

            chain.doFilter(request, response);

        } else {

            httpResponse.setStatus(429);
            httpResponse.getWriter().write("Too many requests");
        }
    }
}