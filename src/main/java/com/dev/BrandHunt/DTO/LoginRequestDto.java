package com.dev.BrandHunt.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequestDto {
    @NotBlank(message = "MISSING_REQUIRED_FIELDS")
    @Email(message = "INVALID_EMAIL")
    private String email;

    @NotBlank(message = "MISSING_REQUIRED_FIELDS")
    private String password;
}