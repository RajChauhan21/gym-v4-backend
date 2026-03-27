package com.backend.gym_backend.security;

import com.backend.gym_backend.entity.Owner;
import com.backend.gym_backend.repo.OwnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CustomUserDetail implements UserDetailsService {

    @Autowired
    private OwnerRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<Owner> owner = repository.findByEmail(username);
        if (owner.isEmpty()){
            throw new UsernameNotFoundException("Email not found");
        }

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(owner.get().getEmail())
                .password(owner.get().getPassword())
                .build();
    }
}
