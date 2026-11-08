package com.teamflow.backend.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record UpdateProfileRequest(
        @Size(max = 50) String displayName,
        @Size(max = 255) String avatarUrl,
        @Size(max = 100) String department,
        @Size(max = 30) String grade,
        @Size(max = 500) String bio,
        @Size(max = 20) List<@Positive Long> tagIds
) {
}
