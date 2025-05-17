package com.dev.BrandHunt.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;
    public static final String NICKNAME_PREFIX = "nickname:";
    private static final String VERIFY_PREFIX = "verifyCode:";
    private static final String EMAIL_PREFIX = "email:";
    private static final String REFRESH_PREFIX = "refresh:";
    private static final String POPULAR_PREFIX = "popularKeyword:";


    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // Refresh Token 저장 (7일)
    public void saveRefreshToken(String email, String refreshToken) {
        redisTemplate.opsForValue().set(email, refreshToken, Duration.ofDays(7));
    }

    // Refresh Token 조회
    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get(email);
    }

    // Refresh Token 삭제
    public void deleteRefreshToken(String email) {
        redisTemplate.delete(email);
    }

    public void setEmailVerification(String key, String value, boolean verified, long timeOut, TimeUnit timeUnit) {
        redisTemplate.opsForHash().put(key, "code", value);
        redisTemplate.opsForHash().put(key, "verified", String.valueOf(verified));
        redisTemplate.expire(key, timeOut, timeUnit);
    }

    // 이메일로 전송한 코드 일치 확인
    public String getEmailVerification(String key) {
        Object code = redisTemplate.opsForHash().get(key, "code");
        return Objects.toString(code, null);
    }

    public void setEmailVerified(String key) {
        redisTemplate.opsForHash().put(key, "verified", true);
    }

    // 회원가입시 이메일 인증 여부 확인
    public Boolean isEmailVerified(String key) {
        Object verified = redisTemplate.opsForHash().get(key, "code");
        return "true".equalsIgnoreCase(verified.toString());
    }
    
    // 검색 키워드 score 증가
    public void setPopularKeyword(String keyword) {
        redisTemplate.opsForZSet().incrementScore(POPULAR_PREFIX, keyword, 1);
    }
    
    // score 높은 순으로 키워드 10개 리턴
    public List<String> getPopularKeywords() {
        return redisTemplate.opsForZSet()
                            .reverseRange("popular:keywords", 0, 9)
                            .stream()
                            .toList();
    }
}