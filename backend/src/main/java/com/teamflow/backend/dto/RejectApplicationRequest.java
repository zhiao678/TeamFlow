package com.teamflow.backend.dto;

import jakarta.validation.constraints.Size;

public record RejectApplicationRequest(@Size(max = 255) String reason) {
}
