package com.dev.BrandHunt.Service;

import com.dev.BrandHunt.Common.CustomException;
import com.dev.BrandHunt.Constant.ErrorCode;
import com.dev.BrandHunt.DTO.SignUpDto;
import com.dev.BrandHunt.Entity.User;
import com.dev.BrandHunt.Repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
@RequiredArgsConstructor
public class UserService {

    private final RedisService redisService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void userSignUp(@RequestBody SignUpDto request) {
        // 이메일 인증 여부
        if (!redisService.isEmailVerified(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 이메일 중복 여부
        if (!userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // 닉네임 중복 여부
        isNickNameAvailable(request.getNickName());

        User user = User.createUser(request, passwordEncoder);
        userRepository.save(user);
//        return true;
    }

    public boolean isNickNameAvailable(String nickName) {
        if(userRepository.existsByNickName(nickName)) {
            throw new CustomException(ErrorCode.NICKNAME_ALREADY_EXISTS);
        }

        return true;
    }
}
