package com.teamflow.backend.dto;

import jakarta.validation.constraints.Size;

public record RejectMemberExitRequest(@Size(max = 255) String reason) {
}
