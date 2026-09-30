package com.dev.BrandHunt.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class SignUpDto {
    @NotBlank(message = "MISSING_REQUIRED_FIELDS")
    @Email(message = "INVALID_EMAIL")
    private String email;

    @NotBlank(message = "MISSING_REQUIRED_FIELDS")
    @Size(min = 8, max = 64, message = "PASSWORD_LENGTH_INVALID")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "PASSWORD_TOO_WEAK")
    private String password;

    @NotBlank(message = "MISSING_REQUIRED_FIELDS")
    @Size(min = 2, max = 20, message = "FIELD_TOO_LONG")
    @JsonAlias("nickname")
    private String nickName;
}
