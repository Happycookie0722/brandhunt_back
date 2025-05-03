package com.dev.BrandHunt.Service;

import com.dev.BrandHunt.Common.CustomException;
import com.dev.BrandHunt.Common.ErrorCode;
import com.dev.BrandHunt.Entity.User;
import com.dev.BrandHunt.Repository.UserRepository;
import com.dev.BrandHunt.Security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    // Spring Security용 사용자 정보 로드
    // Spring Security 내부에서 사용
    // SecurityContext를 통해 인증 처리 시 호출
    // 반환값은 UserDetails 인터페이스를 구현한 UserPrincipal
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return new UserPrincipal(user); // 여기서 UserPrincipal로 감싸서 반환
    }

    // JWT 로그인 API에서 직접 호출해 인증 수행.
    // 비밀번호를 직접 검증하고, 성공하면 실제 User 엔티티를 반환합니다.
    // 이후에 JwtUtil.generateToken(user.getEmail()) 같은 방식으로 토큰 발급
    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_PASSWORD);
        }

        return user;
    }
}
