package com.teamflow.backend.vo;

import java.util.List;

public record NotificationPageVO(
        List<NotificationVO> records,
        int page,
        int pageSize,
        long total,
        long unreadCount
) {
}
