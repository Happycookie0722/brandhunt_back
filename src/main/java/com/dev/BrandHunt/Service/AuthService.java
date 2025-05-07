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

import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisService redisService;
    private final MailService mailService;

    // JWT 로그인 API에서 직접 호출해 인증 수행
    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String aaa = passwordEncoder.encode(password);
        log.info("4444" + aaa);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new CustomException(ErrorCode.USER_INACTIVE);
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.USER_SUSPENDED);
        }

        return user;
    }

    public void sendVerificationCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 이미 발급한 이후에 재전송 눌렀을 경우
        if(redisService.hasKey(email)) {
            redisService.delete(email);
//            throw new CustomException(ErrorCode.AUTH_EMAIL_DUPLICATED);
        }

        String code = createRandomCode();
        boolean verified = false;
        redisService.setEmailVerification(email, code, verified, 10L, TimeUnit.MINUTES);
        mailService.sendVerificationEmail(email, code);
    }

    // 인증 코드 확인
    public void verifyCode(String email, String inputCode) {
        String code = redisService.getEmailVerification(email);

        if (code == null) {
            throw new CustomException(ErrorCode.VERIFY_NOT_FOUND);
        }

        if (!code.equals(inputCode)) {
            throw new CustomException(ErrorCode.INVALID_VERIFY_CODE);
        }

        // 인증 완료 후 verified 값 true 로 변경
        redisService.setEmailVerified(email);
    }

    // 인증 코드 생성 (6자리 숫자)
    private String createRandomCode() {
        return String.valueOf((int) ((Math.random() * 900_000) + 100_000)); // 100000 ~ 999999
    }
}
