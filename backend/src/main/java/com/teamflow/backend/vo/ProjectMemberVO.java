package com.teamflow.backend.vo;

import java.time.LocalDateTime;

public record ProjectMemberVO(
        Long id,
        String displayName,
        String avatarUrl,
        String projectRole,
        String teamRole,
        LocalDateTime joinedAt
) {
}
