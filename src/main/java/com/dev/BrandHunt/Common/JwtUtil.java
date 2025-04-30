package com.dev.BrandHunt.Common;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // SECRET_KEY, EXPIRATION_MS .env 파일이나 application.yml에 따로 저장
    private static final String SECRET_KEY = "BrandHunt123BrandHunt123BrandHunt123BrandHunt123";
    private static final long EXPIRATION_MS = 1000 * 60 * 60 * 24; // 24시간

    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    // 1. 토큰 생성
    // 이메일 기반으로 JWT 발급
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 2. 토큰에서 이메일(Subject) 추출
    public String extractEmail(String token) {
        return parseToken(token).getBody().getSubject();
    }

    // 3. 토큰 유효성 검사.
    // 서명, 만료일 검증
    public boolean isTokenValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 4. 내부적으로 공통 파싱 로직
    private Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
    }
}
