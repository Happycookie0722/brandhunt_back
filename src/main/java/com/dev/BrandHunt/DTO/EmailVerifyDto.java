package com.dev.BrandHunt.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class EmailVerifyDto {
    @NotBlank(message = "MISSING_REQUIRED_FIELDS")
    @Email(message = "INVALID_EMAIL")
    private String email;

    @Pattern(regexp = "^\\d{6}$", message = "INVALID_VERIFY_CODE")
    private String code;
}
