package com.teamflow.backend.service;

import com.teamflow.backend.dto.LoginRequest;
import com.teamflow.backend.dto.RegisterRequest;
import com.teamflow.backend.dto.UpdateProfileRequest;
import com.teamflow.backend.vo.AuthVO;
import com.teamflow.backend.vo.UserProfileVO;

public interface AuthService {
    AuthVO register(RegisterRequest request);
    AuthVO login(LoginRequest request);
    UserProfileVO currentUser();
    UserProfileVO updateProfile(UpdateProfileRequest request);
}
