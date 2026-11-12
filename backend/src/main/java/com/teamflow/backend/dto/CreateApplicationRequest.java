package com.teamflow.backend.dto;

import jakarta.validation.constraints.Size;

public record CreateApplicationRequest(
        @Size(max = 1000) String introduction,
        @Size(max = 1000) String skillsDescription,
        @Size(max = 255) String availableTime
) {
}
