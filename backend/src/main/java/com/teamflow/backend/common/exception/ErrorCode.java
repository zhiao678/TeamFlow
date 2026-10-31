package com.teamflow.backend.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INVALID_REQUEST(40000, "invalid request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(40100, "unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(40300, "forbidden", HttpStatus.FORBIDDEN),
    DATA_NOT_FOUND(40400, "data not found", HttpStatus.NOT_FOUND),
    CONFLICT(40900, "conflict", HttpStatus.CONFLICT),
    INTERNAL_SERVER_ERROR(50000, "internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
