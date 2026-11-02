package com.teamflow.backend.controller;

import com.teamflow.backend.common.result.Result;
import com.teamflow.backend.dto.LoginRequest;
import com.teamflow.backend.dto.RegisterRequest;
import com.teamflow.backend.service.AuthService;
import com.teamflow.backend.vo.AuthVO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Result<AuthVO>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(authService.register(request)));
    }

    @PostMapping("/login")
    public Result<AuthVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }
}
