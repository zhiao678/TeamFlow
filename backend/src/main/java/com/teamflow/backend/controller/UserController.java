package com.teamflow.backend.controller;

import com.teamflow.backend.common.result.Result;
import com.teamflow.backend.dto.UpdateProfileRequest;
import com.teamflow.backend.service.AuthService;
import com.teamflow.backend.vo.UserProfileVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public Result<UserProfileVO> currentUser() {
        return Result.success(authService.currentUser());
    }

    @PatchMapping("/me")
    public Result<UserProfileVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.success(authService.updateProfile(request));
    }
}
