package com.teamflow.backend.vo;

import java.time.LocalDateTime;

public record TeamMemberVO(Long id, String displayName, String avatarUrl, String role, LocalDateTime joinedAt) {
}
