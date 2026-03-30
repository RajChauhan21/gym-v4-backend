package com.backend.gym_backend.service;

import com.backend.gym_backend.entity.Owner;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${jwt.secret.key}")
    public String jwtSecretKey;

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public SecretKey secretKey(){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey)); //first decode in BASE64, then encode using hmac algorithm
    }

    public String generateToken(Owner owner){ //generate jwt token
        return Jwts.builder()
                .claim("name",owner.getName())
                .claim("owner","CXR Technologies")
                .subject(owner.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000*60*10)) //10 mins expiry
                .signWith(secretKey())
                .compact();
    }

    public Claims getClaims(String token){ //extract claims from token
//        System.out.println("TOKEN RECEIVED = " + token);
        return Jwts.parser()
                .verifyWith(secretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsername(String token){ //get username from token
        Claims claims = getClaims(token);
        return claims.getSubject();
    }

    public Boolean isTokenExpired(String token){ //check token expiry
        return getClaims(token).getExpiration().after(new Date());
    }

    public Boolean isTokenValid(String token, UserDetails userDetails){ //check if token is valid
        String username = getUsername(token);
        return username.equals(userDetails.getUsername()) && isTokenExpired(token);
    }
}
