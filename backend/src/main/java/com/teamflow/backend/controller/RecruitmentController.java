package com.teamflow.backend.controller;

import com.teamflow.backend.common.result.Result;
import com.teamflow.backend.dto.CloseRecruitmentRequest;
import com.teamflow.backend.dto.CreateRecruitmentRequest;
import com.teamflow.backend.dto.RecruitmentQueryRequest;
import com.teamflow.backend.dto.UpdateRecruitmentRequest;
import com.teamflow.backend.service.RecruitmentService;
import com.teamflow.backend.vo.RecruitmentPageVO;
import com.teamflow.backend.vo.RecruitmentVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/recruitments")
public class RecruitmentController {

    private final RecruitmentService recruitmentService;

    public RecruitmentController(RecruitmentService recruitmentService) {
        this.recruitmentService = recruitmentService;
    }

    @GetMapping
    public Result<RecruitmentPageVO> list(@Valid @ModelAttribute RecruitmentQueryRequest request) {
        return Result.success(recruitmentService.listRecruitments(request));
    }

    @GetMapping("/mine")
    public Result<List<RecruitmentVO>> mine() {
        return Result.success(recruitmentService.listMyRecruitments());
    }

    @GetMapping("/{id}")
    public Result<RecruitmentVO> detail(@PathVariable @Positive Long id) {
        return Result.success(recruitmentService.getRecruitment(id));
    }

    @PostMapping
    public ResponseEntity<Result<RecruitmentVO>> create(
            @Valid @RequestBody CreateRecruitmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success(recruitmentService.createRecruitment(request)));
    }

    @PatchMapping("/{id}")
    public Result<RecruitmentVO> update(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdateRecruitmentRequest request
    ) {
        return Result.success(recruitmentService.updateRecruitment(id, request));
    }

    @PostMapping("/{id}/publish")
    public Result<RecruitmentVO> publish(@PathVariable @Positive Long id) {
        return Result.success(recruitmentService.publishRecruitment(id));
    }

    @PostMapping("/{id}/close")
    public Result<RecruitmentVO> close(
            @PathVariable @Positive Long id,
            @Valid @RequestBody(required = false) CloseRecruitmentRequest body
    ) {
        return Result.success(recruitmentService.closeRecruitment(
                id,
                body == null ? null : body.reason()
        ));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable @Positive Long id) {
        recruitmentService.deleteDraft(id);
        return Result.success();
    }
}
