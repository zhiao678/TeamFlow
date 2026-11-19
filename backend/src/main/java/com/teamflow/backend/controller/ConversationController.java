package com.teamflow.backend.controller;

import com.teamflow.backend.common.result.Result;
import com.teamflow.backend.dto.CreatePrivateConversationRequest;
import com.teamflow.backend.dto.MarkConversationReadRequest;
import com.teamflow.backend.dto.SendMessageRequest;
import com.teamflow.backend.service.ConversationService;
import com.teamflow.backend.vo.ConversationReadVO;
import com.teamflow.backend.vo.ConversationVO;
import com.teamflow.backend.vo.MessagePageVO;
import com.teamflow.backend.vo.MessageVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {
    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public Result<List<ConversationVO>> list() {
        return Result.success(conversationService.list());
    }

    @PostMapping("/private")
    public ResponseEntity<Result<ConversationVO>> createPrivate(
            @Valid @RequestBody CreatePrivateConversationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success(conversationService.createPrivate(request.targetUserId())));
    }

    @GetMapping("/{id}/messages")
    public Result<MessagePageVO> listMessages(
            @PathVariable @Positive Long id,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) @Positive Long afterMessageId
    ) {
        return Result.success(conversationService.listMessages(id, page, pageSize, afterMessageId));
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<Result<MessageVO>> sendMessage(
            @PathVariable @Positive Long id,
            @Valid @RequestBody SendMessageRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success(conversationService.sendMessage(id, request)));
    }

    @PatchMapping("/{id}/read")
    public Result<ConversationReadVO> markRead(
            @PathVariable @Positive Long id,
            @Valid @RequestBody MarkConversationReadRequest request
    ) {
        return Result.success(conversationService.markRead(id, request));
    }
}
