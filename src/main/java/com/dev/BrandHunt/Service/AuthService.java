package com.dev.BrandHunt.Service;

import com.dev.BrandHunt.Common.CustomException;
import com.dev.BrandHunt.Constant.ErrorCode;
import com.dev.BrandHunt.Constant.UserStatus;
import com.dev.BrandHunt.Entity.User;
import com.dev.BrandHunt.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private static final int MAX_LOGIN_ATTEMPTS = 10;
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final int MAX_VERIFY_SEND_ATTEMPTS = 3;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;
    private final MailService mailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public User authenticate(String email, String password, String clientIp) {
        long emailAttempts = redisService.incrementLoginEmailAttempts(email);
        long ipAttempts = redisService.incrementLoginIpAttempts(clientIp);

        if (emailAttempts > MAX_LOGIN_ATTEMPTS || ipAttempts > MAX_LOGIN_ATTEMPTS * 3L) {
            throw new CustomException(ErrorCode.RATE_LIMITED);
        }

        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        redisService.resetLoginAttempts(email, clientIp);

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new CustomException(ErrorCode.USER_INACTIVE);
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.USER_SUSPENDED);
        }

        return user;
    }

    public void sendVerificationCode(String email) {
        long attempts = redisService.incrementVerificationSendAttempts(email);
        if (attempts > MAX_VERIFY_SEND_ATTEMPTS) {
            return;
        }

        // 가입 여부를 응답으로 노출하지 않기 위해 기존 회원에게도 동일한 성공 응답을 반환한다.
        if (userRepository.existsByEmail(email)) {
            return;
        }

        String code = createRandomCode();
        redisService.setEmailVerification(email, code, false, 10L, TimeUnit.MINUTES);
        mailService.sendVerificationEmail(email, code);
    }

    public void verifyCode(String email, String inputCode) {
        long attempts = redisService.incrementVerificationAttempts(email);
        if (attempts > MAX_VERIFY_ATTEMPTS) {
            throw new CustomException(ErrorCode.RATE_LIMITED);
        }

        String code = redisService.getEmailVerification(email);

        if (code == null) {
            throw new CustomException(ErrorCode.VERIFY_NOT_FOUND);
        }

        if (!code.equals(inputCode)) {
            throw new CustomException(ErrorCode.INVALID_VERIFY_CODE);
        }

        redisService.setEmailVerified(email);
        redisService.resetVerificationAttempts(email);
    }

    private String createRandomCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
