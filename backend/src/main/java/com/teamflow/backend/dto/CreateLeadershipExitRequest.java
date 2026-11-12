package com.teamflow.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateLeadershipExitRequest(@NotNull @Positive Long successorId) {
}
