package com.skillcircle.auth.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Service for JWT token generation, validation, and claim extraction.
 * Uses HMAC-SHA256 signing with a configurable secret key.
 */
@Slf4j
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secretKey;

    @Value("${app.jwt.access-token-expiry-ms}")
    private long accessTokenExpiryMs;

    @Value("${app.jwt.refresh-token-expiry-ms}")
    private long refreshTokenExpiryMs;

    /**
     * Generate an access token for the given user.
     */
    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(
                Map.of("type", "access"),
                userDetails.getUsername(),
                accessTokenExpiryMs
        );
    }

    /**
     * Generate a refresh token for the given user.
     * Refresh tokens use a unique JTI for one-time-use rotation.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(
                Map.of("type", "refresh", "jti", UUID.randomUUID().toString()),
                userDetails.getUsername(),
                refreshTokenExpiryMs
        );
    }

    /**
     * Extract the username (subject) from a token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract the token type ("access" or "refresh").
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("type", String.class));
    }

    /**
     * Extract the JTI (unique ID) from a refresh token.
     */
    public String extractJti(String token) {
        return extractClaim(token, claims -> claims.get("jti", String.class));
    }

    /**
     * Validate the token against the provided user details.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if a token is expired.
     */
    public boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Get access token expiry duration in seconds.
     */
    public long getAccessTokenExpirySeconds() {
        return accessTokenExpiryMs / 1000;
    }

    // ===================== Internal =====================

    private String buildToken(Map<String, Object> extraClaims, String subject, long expirationMs) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
