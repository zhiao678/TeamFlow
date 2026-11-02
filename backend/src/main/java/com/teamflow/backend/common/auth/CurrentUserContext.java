package com.teamflow.backend.common.auth;

import com.teamflow.backend.common.exception.BusinessException;
import com.teamflow.backend.common.exception.ErrorCode;

public final class CurrentUserContext {

    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(Long userId) {
        CURRENT_USER.set(userId);
    }

    public static Long get() {
        return CURRENT_USER.get();
    }

    public static Long require() {
        Long userId = get();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "login required");
        }
        return userId;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
