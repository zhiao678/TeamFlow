package com.teamflow.backend.dto;

import jakarta.validation.constraints.Size;

public record CloseRecruitmentRequest(@Size(max = 255) String reason) {
}
