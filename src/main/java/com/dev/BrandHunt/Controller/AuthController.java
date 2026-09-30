package com.dev.BrandHunt.Controller;

import com.dev.BrandHunt.Common.JwtUtil;
import com.dev.BrandHunt.DTO.EmailVerifyDto;
import com.dev.BrandHunt.DTO.LoginRequestDto;
import com.dev.BrandHunt.DTO.SignUpDto;
import com.dev.BrandHunt.DTO.TokenResponseDto;
import com.dev.BrandHunt.Entity.User;
import com.dev.BrandHunt.Security.UserPrincipal;
import com.dev.BrandHunt.Service.AuthService;
import com.dev.BrandHunt.Service.RedisService;
import com.dev.BrandHunt.Service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RedisService redisService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest) {

        User user = authService.authenticate(
                request.getEmail(),
                request.getPassword(),
                httpRequest.getRemoteAddr()
        );

        String accessToken = jwtUtil.generateAccessToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        redisService.saveRefreshToken(user.getEmail(), refreshToken);

        return ResponseEntity.ok(new TokenResponseDto(accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponseDto> refresh(@RequestBody TokenResponseDto request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new com.dev.BrandHunt.Common.CustomException(
                    com.dev.BrandHunt.Constant.ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String email = jwtUtil.extractEmail(refreshToken);
        String storedToken = redisService.getRefreshToken(email);

        if (!refreshToken.equals(storedToken)) {
            throw new com.dev.BrandHunt.Common.CustomException(
                    com.dev.BrandHunt.Constant.ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String newAccessToken = jwtUtil.generateAccessToken(email);
        String newRefreshToken = jwtUtil.generateRefreshToken(email);
        redisService.saveRefreshToken(email, newRefreshToken);

        return ResponseEntity.ok(new TokenResponseDto(newAccessToken, newRefreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        if (authorization != null && authorization.startsWith("Bearer ")) {
            jwtUtil.revokeAccessToken(authorization.substring(7));
        }

        redisService.deleteRefreshToken(userPrincipal.getUser().getEmail());
        return ResponseEntity.ok("로그아웃 완료");
    }

    @PostMapping("/send-verification")
    public ResponseEntity<?> sendVerificationCode(@Valid @RequestBody EmailVerifyDto request) {
        authService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok("인증 메일을 전송했습니다.");
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@Valid @RequestBody EmailVerifyDto request) {
        authService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok("이메일 인증 완료");
    }

    @PostMapping("/signup")
    public ResponseEntity<?> userSignUp(@Valid @RequestBody SignUpDto request) {
        userService.userSignUp(request);
        return ResponseEntity.ok("회원가입 완료");
    }
}
