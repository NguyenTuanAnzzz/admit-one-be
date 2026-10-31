package com.an.admit_one.service;

import com.an.admit_one.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(UserPrincipal userPrincipal) {
        return generateToken(
                userPrincipal,
                accessExpiration,
                "ACCESS"
        );
    }

    public String generateRefreshToken(UserPrincipal userPrincipal) {
        return generateToken(
                userPrincipal,
                refreshExpiration,
                "REFRESH"
        );
    }

    private String generateToken(
            UserPrincipal userPrincipal,
            long expiration,
            String type
    ) {
        Date now = new Date();

        return Jwts.builder()
                .subject(userPrincipal.getUsername())
                .claim("type", type)
                .issuedAt(now)
                .expiration(
                        new Date(now.getTime() + expiration)
                )
                .signWith(getSecretKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractTokenType(String token) {
        return extractAllClaims(token)
                .get("type", String.class);
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token)
                .getExpiration()
                .before(new Date());
    }

    public boolean isTokenValid(
            String token,
            UserPrincipal userPrincipal
    ) {
        String username = extractUsername(token);

        return username.equals(userPrincipal.getUsername())
                && !isTokenExpired(token);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}