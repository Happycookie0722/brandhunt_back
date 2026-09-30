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
    private static final String REFRESH_PREFIX = "refresh:";
    private static final String POPULAR_PREFIX = "popularKeyword:";
    private static final String LOGIN_EMAIL_PREFIX = "login:email:";
    private static final String LOGIN_IP_PREFIX = "login:ip:";
    private static final String VERIFY_ATTEMPT_PREFIX = "verify:attempt:";
    private static final String VERIFY_SEND_PREFIX = "verify:send:";

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public long incrementWithExpiry(String key, Duration expiry) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, expiry);
        }
        return count == null ? 0L : count;
    }

    public void resetLoginAttempts(String email, String ip) {
        redisTemplate.delete(LOGIN_EMAIL_PREFIX + email);
        redisTemplate.delete(LOGIN_IP_PREFIX + ip);
    }

    public long incrementLoginEmailAttempts(String email) {
        return incrementWithExpiry(LOGIN_EMAIL_PREFIX + email, Duration.ofMinutes(15));
    }

    public long incrementLoginIpAttempts(String ip) {
        return incrementWithExpiry(LOGIN_IP_PREFIX + ip, Duration.ofMinutes(15));
    }

    public long incrementVerificationAttempts(String email) {
        return incrementWithExpiry(VERIFY_ATTEMPT_PREFIX + email, Duration.ofMinutes(10));
    }

    public void resetVerificationAttempts(String email) {
        redisTemplate.delete(VERIFY_ATTEMPT_PREFIX + email);
    }

    public long incrementVerificationSendAttempts(String email) {
        return incrementWithExpiry(VERIFY_SEND_PREFIX + email, Duration.ofMinutes(10));
    }

    public void saveRefreshToken(String email, String refreshToken) {
        redisTemplate.opsForValue().set(REFRESH_PREFIX + email, refreshToken, Duration.ofDays(7));
    }

    public String getRefreshToken(String email) {
        return redisTemplate.opsForValue().get(REFRESH_PREFIX + email);
    }

    public void deleteRefreshToken(String email) {
        redisTemplate.delete(REFRESH_PREFIX + email);
    }

    public void setEmailVerification(String email, String value, boolean verified, long timeOut, TimeUnit timeUnit) {
        String key = VERIFY_PREFIX + email;
        redisTemplate.opsForHash().put(key, "code", value);
        redisTemplate.opsForHash().put(key, "verified", String.valueOf(verified));
        redisTemplate.expire(key, timeOut, timeUnit);
    }

    public String getEmailVerification(String email) {
        Object code = redisTemplate.opsForHash().get(VERIFY_PREFIX + email, "code");
        return Objects.toString(code, null);
    }

    public void setEmailVerified(String email) {
        redisTemplate.opsForHash().put(VERIFY_PREFIX + email, "verified", "true");
    }

    public Boolean isEmailVerified(String email) {
        Object verified = redisTemplate.opsForHash().get(VERIFY_PREFIX + email, "verified");
        return verified != null && "true".equalsIgnoreCase(verified.toString());
    }

    public void deleteEmailVerification(String email) {
        redisTemplate.delete(VERIFY_PREFIX + email);
    }

    public void setPopularKeyword(String keyword) {
        redisTemplate.opsForZSet().incrementScore(POPULAR_PREFIX, keyword, 1);
    }

    public List<String> getPopularKeywords() {
        return redisTemplate.opsForZSet()
                .reverseRange(POPULAR_PREFIX, 0, 9)
                .stream()
                .toList();
    }
}
