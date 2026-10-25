package com.teamflow.backend.common.result;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTests {

    @Test
    void shouldCreateSuccessfulResultWithData() {
        Result<String> result = Result.success("project");

        assertThat(result.getCode()).isZero();
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).isEqualTo("project");
    }

    @Test
    void shouldCreateFailureResultWithoutData() {
        Result<Void> result = Result.failure(40400, "data not found");

        assertThat(result.getCode()).isEqualTo(40400);
        assertThat(result.getMessage()).isEqualTo("data not found");
        assertThat(result.getData()).isNull();
    }
}
