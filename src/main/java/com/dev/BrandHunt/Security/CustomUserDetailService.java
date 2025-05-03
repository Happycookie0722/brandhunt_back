package com.dev.BrandHunt.Security;

import com.dev.BrandHunt.Common.CustomException;
import com.dev.BrandHunt.Entity.User;
import com.dev.BrandHunt.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static com.dev.BrandHunt.Common.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {
    private final UserRepository userRepository;

    // 이메일로 사용자 조회
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(USER_NOT_FOUND));

        return UserPrincipal.create(user); // UserDetails 구현체 반환
    }
}
