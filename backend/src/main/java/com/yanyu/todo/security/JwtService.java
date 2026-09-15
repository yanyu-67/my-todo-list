package com.yanyu.todo.security;

import com.yanyu.todo.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties){
        this.properties=properties;
        this.key= Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(properties.secret())
        );
    }

    public String generate(String username){
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.expirationMinutes(),ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public String username(String token){
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }
}
