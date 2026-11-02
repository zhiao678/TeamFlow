package com.teamflow.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.teamflow.backend.common.auth.CurrentUserContext;
import com.teamflow.backend.common.auth.JwtTokenService;
import com.teamflow.backend.common.exception.BusinessException;
import com.teamflow.backend.common.exception.ErrorCode;
import com.teamflow.backend.dto.LoginRequest;
import com.teamflow.backend.dto.RegisterRequest;
import com.teamflow.backend.dto.UpdateProfileRequest;
import com.teamflow.backend.entity.User;
import com.teamflow.backend.entity.UserProfile;
import com.teamflow.backend.entity.UserTag;
import com.teamflow.backend.entity.Tag;
import com.teamflow.backend.mapper.TagMapper;
import com.teamflow.backend.mapper.UserMapper;
import com.teamflow.backend.mapper.UserProfileMapper;
import com.teamflow.backend.mapper.UserTagMapper;
import com.teamflow.backend.service.AuthService;
import com.teamflow.backend.vo.AuthVO;
import com.teamflow.backend.vo.UserProfileVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserTagMapper userTagMapper;
    private final TagMapper tagMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthServiceImpl(
            UserMapper userMapper,
            UserProfileMapper userProfileMapper,
            UserTagMapper userTagMapper,
            TagMapper tagMapper,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.userMapper = userMapper;
        this.userProfileMapper = userProfileMapper;
        this.userTagMapper = userTagMapper;
        this.tagMapper = tagMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    @Transactional
    public AuthVO register(RegisterRequest request) {
        User existing = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, request.username()));
        if (existing != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "username already exists");
        }

        User user = new User();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        userMapper.insert(user);

        UserProfile profile = new UserProfile();
        profile.setUserId(user.getId());
        profile.setContactAllowed(1);
        userProfileMapper.insert(profile);
        return new AuthVO(jwtTokenService.createToken(user.getId()), toView(user));
    }

    @Override
    public AuthVO login(LoginRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, request.username()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "username or password is invalid");
        }
        return new AuthVO(jwtTokenService.createToken(user.getId()), toView(user));
    }

    @Override
    public UserProfileVO currentUser() {
        return loadProfile(CurrentUserContext.require());
    }

    @Override
    @Transactional
    public UserProfileVO updateProfile(UpdateProfileRequest request) {
        Long userId = CurrentUserContext.require();
        User user = requireUser(userId);
        if (request.displayName() != null) user.setDisplayName(request.displayName().trim());
        if (request.avatarUrl() != null) user.setAvatarUrl(request.avatarUrl());
        userMapper.updateById(user);

        UserProfile profile = userProfileMapper.selectById(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setContactAllowed(1);
        }
        if (request.department() != null) profile.setDepartment(request.department());
        if (request.grade() != null) profile.setGrade(request.grade());
        if (request.bio() != null) profile.setBio(request.bio());
        if (profile.getCreatedAt() == null) userProfileMapper.insert(profile);
        else userProfileMapper.updateById(profile);
        return loadProfile(userId);
    }

    private UserProfileVO loadProfile(Long userId) {
        User user = requireUser(userId);
        UserProfile profile = userProfileMapper.selectById(userId);
        List<String> skills = userTagMapper.selectByUserId(userId).stream()
                .map(UserTag::getTagId)
                .map(tagMapper::selectById)
                .filter(java.util.Objects::nonNull)
                .map(Tag::getName)
                .toList();
        if (profile == null) {
            return new UserProfileVO(user.getId(), user.getUsername(), user.getDisplayName(),
                    user.getAvatarUrl(), null, null, null, skills);
        }
        return new UserProfileVO(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getAvatarUrl(), profile.getDepartment(), profile.getGrade(), profile.getBio(), skills);
    }

    private UserProfileVO toView(User user) {
        return new UserProfileVO(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getAvatarUrl(), null, null, null, Collections.emptyList());
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "user not found");
        return user;
    }
}
