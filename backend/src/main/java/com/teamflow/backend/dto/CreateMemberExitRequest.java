package com.teamflow.backend.dto;

import jakarta.validation.constraints.Size;

public record CreateMemberExitRequest(@Size(max = 255) String reason) {
}
