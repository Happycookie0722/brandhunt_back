package com.dev.BrandHunt.Controller;

import com.dev.BrandHunt.Common.JwtUtil;
import com.dev.BrandHunt.DTO.EmailVerifyDto;
import com.dev.BrandHunt.DTO.LoginRequestDto;
import com.dev.BrandHunt.DTO.TokenResponseDto;
import com.dev.BrandHunt.Entity.User;
import com.dev.BrandHunt.Service.RedisService;
import com.dev.BrandHunt.Service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RedisService redisService;

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        User user = userService.authenticate(request.getEmail(), request.getPassword());

        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        redisService.saveRefreshToken(user.getEmail(), refreshToken);

        return ResponseEntity.ok(new TokenResponseDto(accessToken, refreshToken));
    }

    // 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refresh(@RequestBody TokenResponseDto request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        String email = jwtUtil.extractEmail(refreshToken);
        String storedToken = redisService.getRefreshToken(email);

        if (!refreshToken.equals(storedToken)) {
            throw new IllegalArgumentException("일치하지 않는 Refresh Token입니다.");
        }

        String newAccessToken = jwtUtil.generateAccessToken(email);

        return ResponseEntity.ok(new TokenResponseDto(newAccessToken, refreshToken));
    }

    // 로그아웃 (Redis에서 Refresh Token 삭제)
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody TokenResponseDto request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.isTokenValid(refreshToken)) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰입니다.");
        }

        String email = jwtUtil.extractEmail(refreshToken);
        redisService.deleteRefreshToken(email);

        return ResponseEntity.ok("로그아웃 완료");
    }
    
    // 이메일 인증 메일 전송
    @PostMapping("/send-verification")
    public ResponseEntity<?> sendVerificationCode(@RequestBody EmailVerifyDto request) {
        userService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok("인증 메일을 전송했습니다.");
    }
    
    // 이메일 인증 완료
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody EmailVerifyDto request) {
        userService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok("이메일 인증 완료");
    }
}
