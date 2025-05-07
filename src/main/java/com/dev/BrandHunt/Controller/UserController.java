package com.dev.BrandHunt.Controller;

import com.dev.BrandHunt.DTO.SignUpDto;
import com.dev.BrandHunt.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/check-nickname")
    public ResponseEntity<Boolean> checkNickName(@RequestParam SignUpDto request) {;
        return ResponseEntity.ok(userService.isNickNameAvailable(request.getNickName()));
    }
}
