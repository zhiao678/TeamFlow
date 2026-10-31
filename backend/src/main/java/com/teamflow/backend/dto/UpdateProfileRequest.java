package com.teamflow.backend.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 50) String displayName,
        @Size(max = 255) String avatarUrl,
        @Size(max = 100) String department,
        @Size(max = 30) String grade,
        @Size(max = 500) String bio
) {
}
