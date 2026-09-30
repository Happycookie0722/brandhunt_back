package com.dev.BrandHunt.Common;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String REVOKED_PREFIX = "revoked:access:";

    @Value("${jwt.secret}")
    private String secret;

    private Key key;

    @Value("${jwt.access.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh.expiration}")
    private long refreshTokenExpiration;

    private final RedisTemplate<String, String> redisTemplate;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(String email) {
        return generateToken(email, accessTokenExpiration, ACCESS_TOKEN_TYPE);
    }

    public String generateRefreshToken(String email) {
        return generateToken(email, refreshTokenExpiration, REFRESH_TOKEN_TYPE);
    }

    private String generateToken(String email, long expirationTime, String tokenType) {
        return Jwts.builder()
                .setSubject(email)
                .setId(UUID.randomUUID().toString())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return parseToken(token).getBody().getSubject();
    }

    public String extractTokenType(String token) {
        return parseToken(token).getBody().get(TOKEN_TYPE_CLAIM, String.class);
    }

    public String extractJti(String token) {
        return parseToken(token).getBody().getId();
    }

    public long getRemainingExpirationMillis(String token) {
        Date expiration = parseToken(token).getBody().getExpiration();
        return Math.max(0L, expiration.getTime() - System.currentTimeMillis());
    }

    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        return isTokenValid(token) && ACCESS_TOKEN_TYPE.equals(extractTokenType(token)) && !isRevoked(token);
    }

    public boolean isRefreshToken(String token) {
        return isTokenValid(token) && REFRESH_TOKEN_TYPE.equals(extractTokenType(token));
    }

    public void revokeAccessToken(String token) {
        if (!isAccessToken(token)) {
            return;
        }

        String jti = extractJti(token);
        long ttl = getRemainingExpirationMillis(token);
        if (jti != null && ttl > 0) {
            redisTemplate.opsForValue().set(REVOKED_PREFIX + jti, "1", ttl, TimeUnit.MILLISECONDS);
        }
    }

    private boolean isRevoked(String token) {
        String jti = extractJti(token);
        return jti != null && Boolean.TRUE.equals(redisTemplate.hasKey(REVOKED_PREFIX + jti));
    }

    private Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }
}
