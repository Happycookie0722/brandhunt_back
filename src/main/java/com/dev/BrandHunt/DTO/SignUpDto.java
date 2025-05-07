package com.dev.BrandHunt.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import org.hibernate.validator.constraints.Length;

@Getter
public class SignUpDto {
    @NotBlank(message = "MISSING_REQUIRED_FIELDS")
    @Email(message = "INVALID_EMAIL")
    private String email;

    @NotBlank(message = "MISSING_REQUIRED_FIELDS")
    private String password;

    @NotBlank(message = "MISSING_REQUIRED_FIELDS")
    @Length(min = 2, max = 20)
    private String nickName;
}
