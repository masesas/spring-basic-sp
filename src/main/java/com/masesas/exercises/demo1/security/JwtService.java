package com.masesas.exercises.demo1.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration ttl;

    @Value("${app.security.jwt-secret}")
    private String secret;

    public JwtService(
            @Value("${app.security.jwt-secret}") String secret,
            @Value("${app.security.jwt-ttl-minutes}") long ttlMinutes
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public String issue(AppUser user, Instant issuedAt) {
        return builder(user, issuedAt)
                .expiration(Date.from(issuedAt.plus(ttl)))
                .compact();
    }

    public String issueWithoutExpiry(AppUser user, Instant issuedAt) {
        return builder(user, issuedAt).compact();
    }

    public long ttlSeconds() {
        return ttl.toSeconds();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private JwtBuilder builder(AppUser user, Instant issuedAt) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("roles", user.getRoles())
                .claim("idKaryawan", user.getIdKaryawan())
                .claim("tipe", user.getTipe())
                .issuedAt(Date.from(issuedAt))
                .signWith(key);
    }
}
