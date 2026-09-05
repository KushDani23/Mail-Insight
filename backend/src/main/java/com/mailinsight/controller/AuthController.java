package com.mailinsight.controller;

import com.mailinsight.dto.response.UserResponse;
import com.mailinsight.entity.User;
import com.mailinsight.mapper.EmailMapper;
import com.mailinsight.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final EmailMapper emailMapper;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        User user = userService.resolveUser(authentication);
        return ResponseEntity.ok(emailMapper.toUserResponse(user));
    }

    @GetMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }
}
