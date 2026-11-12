package com.teamflow.backend.service.impl;

import com.teamflow.backend.common.auth.CurrentUserContext;
import com.teamflow.backend.common.exception.BusinessException;
import com.teamflow.backend.common.exception.ErrorCode;
import com.teamflow.backend.entity.Notification;
import com.teamflow.backend.mapper.NotificationMapper;
import com.teamflow.backend.service.NotificationService;
import com.teamflow.backend.vo.NotificationPageVO;
import com.teamflow.backend.vo.NotificationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {
    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @Override
    public NotificationPageVO list(int page, int pageSize, boolean unreadOnly) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "invalid notification pagination");
        }
        Long userId = CurrentUserContext.require();
        long offset = (long) (page - 1) * pageSize;
        List<NotificationVO> records = notificationMapper.selectPageForUser(userId, unreadOnly, pageSize, offset)
                .stream().map(this::toView).toList();
        return new NotificationPageVO(records, page, pageSize,
                notificationMapper.countForUser(userId, unreadOnly), notificationMapper.countUnread(userId));
    }

    @Override
    @Transactional
    public void markRead(Long id) {
        Long userId = CurrentUserContext.require();
        if (notificationMapper.countOwned(id, userId) == 0) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "notification not found");
        }
        notificationMapper.markRead(id, userId);
    }

    @Override
    @Transactional
    public void markAllRead() {
        notificationMapper.markAllRead(CurrentUserContext.require());
    }

    @Override
    @Transactional
    public void create(Long recipientId, Long actorId, String eventType, String eventKey,
                       String targetType, Long recruitmentId, Long applicationId,
                       String title, String content) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setActorId(actorId);
        notification.setEventType(eventType);
        notification.setEventKey(eventKey);
        notification.setTargetType(targetType);
        notification.setRecruitmentId(recruitmentId);
        notification.setApplicationId(applicationId);
        notification.setTitle(title);
        notification.setContent(content);
        notificationMapper.insertIfAbsent(notification);
    }

    private NotificationVO toView(Notification item) {
        return new NotificationVO(item.getId(), item.getEventType(), item.getTargetType(),
                item.getRecruitmentId(), item.getApplicationId(), item.getTitle(), item.getContent(),
                item.getReadAt() != null, item.getCreatedAt());
    }
}
