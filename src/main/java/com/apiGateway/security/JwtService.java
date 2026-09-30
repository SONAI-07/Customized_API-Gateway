package com.apiGateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlMillis;

    public JwtService(@Value("${gateway.auth.secret}") String secret,
                      @Value("${gateway.auth.token-ttl-minutes:60}") long ttlMinutes) {
        // HS256 requires a secret of at least 32 characters
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMillis = ttlMinutes * 60_000;
    }

    public String generateToken(String subject, String role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMillis))
                .signWith(key)
                .compact();
    }

    public Optional<Claims> validate(String token) {
        try {
            return Optional.of(
                    Jwts.parser().verifyWith(key).build()
                            .parseSignedClaims(token).getPayload()
            );
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty(); // expired, tampered, wrong signature -> reject
        }
    }
}