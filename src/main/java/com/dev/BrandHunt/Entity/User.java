package com.dev.BrandHunt.Entity;

import com.dev.BrandHunt.Constant.UserStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.validator.constraints.Length;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter @Setter @Entity
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Favorite> favorites = new ArrayList<>();

    @Email
    @NotNull
    @Column(unique = true)
    private String email;

    @Length(min = 2, max = 20)
    @Column(unique = true)
    @NotNull
    private String nickName;

    @NotNull
    private String password;

    private LocalDateTime last_login_at;

    private LocalDateTime password_change_at;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    private String profile_img;
}
