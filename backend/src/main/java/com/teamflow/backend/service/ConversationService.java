package com.teamflow.backend.service;

import com.teamflow.backend.dto.MarkConversationReadRequest;
import com.teamflow.backend.dto.SendMessageRequest;
import com.teamflow.backend.vo.ConversationReadVO;
import com.teamflow.backend.vo.ConversationVO;
import com.teamflow.backend.vo.MessagePageVO;
import com.teamflow.backend.vo.MessageVO;

import java.util.List;

public interface ConversationService {
    List<ConversationVO> list();

    ConversationVO createPrivate(Long targetUserId);

    MessagePageVO listMessages(Long conversationId, int page, int pageSize, Long afterMessageId);

    MessageVO sendMessage(Long conversationId, SendMessageRequest request);

    ConversationReadVO markRead(Long conversationId, MarkConversationReadRequest request);
}
