package com.teamflow.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateTaskStatusRequest {

    @NotBlank(message = "must not be blank")
    @Pattern(regexp = "TODO|IN_PROGRESS|DONE", message = "must be TODO, IN_PROGRESS or DONE")
    @Schema(description = "目标状态", example = "IN_PROGRESS", allowableValues = {"TODO", "IN_PROGRESS", "DONE"})
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
