package com.teamflow.backend.service.impl;

import com.teamflow.backend.common.auth.CurrentUserContext;
import com.teamflow.backend.common.exception.BusinessException;
import com.teamflow.backend.common.exception.ErrorCode;
import com.teamflow.backend.dto.MarkConversationReadRequest;
import com.teamflow.backend.dto.SendMessageRequest;
import com.teamflow.backend.entity.Conversation;
import com.teamflow.backend.entity.ConversationMember;
import com.teamflow.backend.entity.Message;
import com.teamflow.backend.entity.Recruitment;
import com.teamflow.backend.entity.User;
import com.teamflow.backend.mapper.ConversationMapper;
import com.teamflow.backend.mapper.ConversationMemberMapper;
import com.teamflow.backend.mapper.MessageMapper;
import com.teamflow.backend.mapper.UserBlockMapper;
import com.teamflow.backend.mapper.UserMapper;
import com.teamflow.backend.mapper.RecruitmentMapper;
import com.teamflow.backend.service.ConversationService;
import com.teamflow.backend.vo.ConversationReadVO;
import com.teamflow.backend.vo.ConversationVO;
import com.teamflow.backend.vo.MessagePageVO;
import com.teamflow.backend.vo.MessageVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConversationServiceImpl implements ConversationService {
    private final ConversationMapper conversationMapper;
    private final ConversationMemberMapper conversationMemberMapper;
    private final MessageMapper messageMapper;
    private final UserBlockMapper userBlockMapper;
    private final UserMapper userMapper;
    private final RecruitmentMapper recruitmentMapper;

    public ConversationServiceImpl(ConversationMapper conversationMapper,
                                   ConversationMemberMapper conversationMemberMapper,
                                   MessageMapper messageMapper,
                                   UserBlockMapper userBlockMapper,
                                   UserMapper userMapper,
                                   RecruitmentMapper recruitmentMapper) {
        this.conversationMapper = conversationMapper;
        this.conversationMemberMapper = conversationMemberMapper;
        this.messageMapper = messageMapper;
        this.userBlockMapper = userBlockMapper;
        this.userMapper = userMapper;
        this.recruitmentMapper = recruitmentMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationVO> list() {
        return conversationMapper.selectForUser(CurrentUserContext.require());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ConversationVO createPrivate(Long targetUserId) {
        Long userId = CurrentUserContext.require();
        if (userId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "cannot create a private conversation with yourself");
        }
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "user not found");
        }
        if ("DISABLED".equals(target.getAccountStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "cannot contact a disabled user");
        }
        ensureNotBlocked(userId, targetUserId);

        long low = Math.min(userId, targetUserId);
        long high = Math.max(userId, targetUserId);
        Conversation conversation = conversationMapper.selectDirect(low, high);
        if (conversation == null) {
            conversation = new Conversation();
            conversation.setType("DIRECT");
            conversation.setDirectUserLowId(low);
            conversation.setDirectUserHighId(high);
            conversation.setStatus("ACTIVE");
            try {
                conversationMapper.insert(conversation);
                insertMember(conversation.getId(), userId);
                insertMember(conversation.getId(), targetUserId);
            } catch (DuplicateKeyException exception) {
                conversation = conversationMapper.selectDirect(low, high);
                if (conversation == null) {
                    throw new BusinessException(ErrorCode.CONFLICT, "private conversation could not be created");
                }
            }
        }

        reactivateIfNeeded(conversation.getId(), userId);
        reactivateIfNeeded(conversation.getId(), targetUserId);
        ConversationVO result = conversationMapper.selectView(conversation.getId(), userId);
        if (result == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "private conversation members are inconsistent");
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public MessagePageVO listMessages(Long conversationId, int page, int pageSize, Long afterMessageId) {
        validatePagination(page, pageSize);
        Long userId = CurrentUserContext.require();
        requireConversation(conversationId);
        requireActiveMember(conversationId, userId, false);
        if (afterMessageId != null && messageMapper.countInConversation(conversationId, afterMessageId) == 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "afterMessageId does not belong to this conversation");
        }
        long offset = (long) (page - 1) * pageSize;
        List<MessageVO> records = messageMapper.selectPage(conversationId, afterMessageId, pageSize, offset);
        long total = afterMessageId == null
                ? messageMapper.countVisible(conversationId)
                : messageMapper.countVisibleAfter(conversationId, afterMessageId);
        ConversationMember member = conversationMemberMapper.select(conversationId, userId);
        long unreadCount = messageMapper.countUnread(conversationId, member.getLastReadMessageId());
        return new MessagePageVO(records, page, pageSize, total, offset + records.size() < total, unreadCount);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MessageVO sendMessage(Long conversationId, SendMessageRequest request) {
        Long userId = CurrentUserContext.require();
        Conversation conversation = requireConversationForUpdate(conversationId, request.sourceRecruitmentId());
        ConversationMember member = requireActiveMember(conversationId, userId, true);
        if (!"ACTIVE".equals(conversation.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "conversation is read-only");
        }
        if ("DIRECT".equals(conversation.getType())) {
            Long otherUserId = conversation.getDirectUserLowId().equals(userId)
                    ? conversation.getDirectUserHighId() : conversation.getDirectUserLowId();
            ensureNotBlocked(userId, otherUserId);
        }

        String clientMessageId = request.clientMessageId().trim();
        String content = request.content();
        Message existing = messageMapper.selectByClientId(conversationId, userId, clientMessageId);
        if (existing != null) {
            ensureSameMessage(existing, content, request.sourceRecruitmentId());
            return messageMapper.selectView(conversationId, existing.getId());
        }

        if (request.sourceRecruitmentId() != null) {
            Recruitment source = recruitmentMapper.selectById(request.sourceRecruitmentId());
            if (source == null || "DRAFT".equals(source.getStatus())) {
                throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "source recruitment not found");
            }
        }
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(member.getUserId());
        message.setClientMessageId(clientMessageId);
        message.setSourceRecruitmentId(request.sourceRecruitmentId());
        message.setContent(content);
        message.setStatus("VISIBLE");
        try {
            messageMapper.insert(message);
        } catch (DuplicateKeyException exception) {
            existing = messageMapper.selectByClientId(conversationId, userId, clientMessageId);
            if (existing == null) {
                throw new BusinessException(ErrorCode.CONFLICT, "message could not be created");
            }
            ensureSameMessage(existing, content, request.sourceRecruitmentId());
            return messageMapper.selectView(conversationId, existing.getId());
        }
        return messageMapper.selectView(conversationId, message.getId());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ConversationReadVO markRead(Long conversationId, MarkConversationReadRequest request) {
        Long userId = CurrentUserContext.require();
        requireConversationForUpdate(conversationId);
        ConversationMember member = requireActiveMember(conversationId, userId, true);
        Long messageId = request.resolvedMessageId();
        if (messageId == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "lastReadMessageId is required");
        }
        if (messageMapper.countInConversation(conversationId, messageId) == 0) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "message not found in conversation");
        }
        conversationMemberMapper.advanceRead(conversationId, userId, messageId);
        Long currentRead = member.getLastReadMessageId() == null || messageId > member.getLastReadMessageId()
                ? messageId : member.getLastReadMessageId();
        return new ConversationReadVO(conversationId, currentRead,
                messageMapper.countUnread(conversationId, currentRead));
    }

    private Conversation requireConversationForUpdate(Long conversationId) {
        return requireConversationForUpdate(conversationId, null);
    }

    private Conversation requireConversationForUpdate(Long conversationId, Long sourceRecruitmentId) {
        Conversation current = requireConversation(conversationId);
        if ("TEAM".equals(current.getType())) {
            List<Long> recruitmentIds = java.util.stream.Stream.of(current.getRecruitmentId(), sourceRecruitmentId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .sorted()
                    .toList();
            for (Long recruitmentId : recruitmentIds) {
                if (recruitmentMapper.selectForUpdate(recruitmentId) == null) {
                    throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "recruitment not found");
                }
            }
        }
        Conversation conversation = conversationMapper.selectForUpdate(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "conversation not found");
        }
        return conversation;
    }

    private Conversation requireConversation(Long conversationId) {
        Conversation conversation = conversationMapper.selectConversation(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "conversation not found");
        }
        return conversation;
    }

    private ConversationMember requireActiveMember(Long conversationId, Long userId, boolean forUpdate) {
        ConversationMember member = forUpdate
                ? conversationMemberMapper.selectForUpdate(conversationId, userId)
                : conversationMemberMapper.select(conversationId, userId);
        if (member == null || !"ACTIVE".equals(member.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only active conversation members can access messages");
        }
        return member;
    }

    private void ensureNotBlocked(Long firstUserId, Long secondUserId) {
        if (userBlockMapper.countBetween(firstUserId, secondUserId) > 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "private conversation is blocked");
        }
    }

    private void insertMember(Long conversationId, Long userId) {
        ConversationMember member = new ConversationMember();
        member.setConversationId(conversationId);
        member.setUserId(userId);
        member.setStatus("ACTIVE");
        conversationMemberMapper.insert(member);
    }

    private void reactivateIfNeeded(Long conversationId, Long userId) {
        ConversationMember member = conversationMemberMapper.select(conversationId, userId);
        if (member != null && !"ACTIVE".equals(member.getStatus())) {
            conversationMemberMapper.reactivate(conversationId, userId);
        }
    }

    private void ensureSameMessage(Message existing, String content, Long sourceRecruitmentId) {
        if (!java.util.Objects.equals(existing.getContent(), content)
                || !java.util.Objects.equals(existing.getSourceRecruitmentId(), sourceRecruitmentId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "client message id was already used with different content");
        }
    }

    private void validatePagination(int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "invalid message pagination");
        }
    }
}
