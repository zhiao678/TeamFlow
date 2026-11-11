package com.teamflow.backend.service;

import com.teamflow.backend.vo.NotificationPageVO;

public interface NotificationService {
    NotificationPageVO list(int page, int pageSize, boolean unreadOnly);
    void markRead(Long id);
    void markAllRead();
    void create(Long recipientId, Long actorId, String eventType, String eventKey,
                String targetType, Long recruitmentId, Long applicationId,
                String title, String content);
}
