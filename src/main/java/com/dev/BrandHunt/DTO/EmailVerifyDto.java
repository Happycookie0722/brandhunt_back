package com.dev.BrandHunt.DTO;

import lombok.Getter;

@Getter
public class EmailVerifyDto {
    // 이메일 인증 관련 DTO
    private String email;
    private String code;
}
