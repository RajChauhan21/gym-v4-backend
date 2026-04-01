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

        Optional<Owner> owner;
        if (username.contains("@")) {   // login using email
            owner = Optional.ofNullable(repository.findByEmail(username).orElseThrow(() -> new RuntimeException("user not found")));

            return User
                    .builder()
                    .username(String.valueOf(owner.get().getEmail()))
                    .password(owner.get().getPassword() != null ? owner.get().getPassword() : "null")
                    .build();

        } else {   // JWT authentication using userId
            Long id = Long.valueOf(username);

            owner = Optional.ofNullable(repository
                    .findById(Math.toIntExact(id))
                    .orElseThrow(() -> new RuntimeException("User not found")));
        }
        return User
                .builder()
                .username(String.valueOf(owner.get().getId()))
                .password(owner.get().getPassword() != null ? owner.get().getPassword() : "null")
                .build();
    }
}
