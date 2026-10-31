package com.teamflow.backend.vo;

import java.util.List;

public record UserProfileVO(
        Long id,
        String username,
        String displayName,
        String avatarUrl,
        String department,
        String grade,
        String bio,
        List<String> skills
) {
}
