package com.backend.gym_backend.security;


import com.backend.gym_backend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain
            filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization"); //get Authorization header

        if (header == null || !header.startsWith("Bearer")) { //validate if user passed the Authorization header and token
            filterChain.doFilter(request, response); //call the next filter
            return;
        }

        String token = header.substring(7); //extract token
        String userName = jwtService.getUsername(token); //extract username from token

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); //retrieve security context

        if (authentication == null && userName != null) { //user is not authenticated yet
            UserDetails userDetails = userDetailsService.loadUserByUsername(userName); //get username from db
            if (jwtService.isTokenValid(token, userDetails)) { //check if token is valid ie - matching username and expiry

                //prepare the next filter
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); //add user's request details
                SecurityContextHolder.getContext().setAuthentication(authenticationToken); //update security context, so that next filter can recognize the request
            }
        }

        filterChain.doFilter(request, response); //call the next filter
    }
}