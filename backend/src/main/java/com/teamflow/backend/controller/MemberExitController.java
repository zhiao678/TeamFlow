package com.teamflow.backend.controller;

import com.teamflow.backend.common.result.Result;
import com.teamflow.backend.dto.CreateMemberExitRequest;
import com.teamflow.backend.dto.RejectMemberExitRequest;
import com.teamflow.backend.service.MemberExitService;
import com.teamflow.backend.vo.MemberExitRequestVO;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api")
public class MemberExitController {
    private final MemberExitService memberExitService;

    public MemberExitController(MemberExitService memberExitService) {
        this.memberExitService = memberExitService;
    }

    @PostMapping("/recruitments/{id}/exit-requests")
    public ResponseEntity<Result<MemberExitRequestVO>> create(
            @PathVariable @Positive Long id,
            @Valid @RequestBody(required = false) CreateMemberExitRequest body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success(memberExitService.create(id, body == null ? null : body.reason())));
    }

    @GetMapping("/recruitments/{id}/exit-requests")
    public Result<List<MemberExitRequestVO>> list(@PathVariable @Positive Long id) {
        return Result.success(memberExitService.listForRecruitment(id));
    }

    @GetMapping("/member-exit-requests/me")
    public Result<List<MemberExitRequestVO>> mine() {
        return Result.success(memberExitService.listMine());
    }

    @PatchMapping("/member-exit-requests/{id}/approve")
    public Result<MemberExitRequestVO> approve(@PathVariable @Positive Long id) {
        return Result.success(memberExitService.approve(id));
    }

    @PatchMapping("/member-exit-requests/{id}/reject")
    public Result<MemberExitRequestVO> reject(
            @PathVariable @Positive Long id,
            @Valid @RequestBody(required = false) RejectMemberExitRequest body
    ) {
        return Result.success(memberExitService.reject(id, body == null ? null : body.reason()));
    }
}
