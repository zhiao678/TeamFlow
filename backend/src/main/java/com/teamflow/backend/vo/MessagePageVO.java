package com.teamflow.backend.vo;

import java.util.List;

public record MessagePageVO(
        List<MessageVO> records,
        int page,
        int pageSize,
        long total,
        boolean hasMore,
        long unreadCount
) {
}
