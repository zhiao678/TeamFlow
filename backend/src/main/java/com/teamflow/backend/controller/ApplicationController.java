package com.teamflow.backend.controller;

import com.teamflow.backend.common.result.Result;
import com.teamflow.backend.dto.CreateApplicationRequest;
import com.teamflow.backend.dto.RejectApplicationRequest;
import com.teamflow.backend.service.ApplicationService;
import com.teamflow.backend.vo.ApplicationVO;
import com.teamflow.backend.vo.TeamMemberVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping("/recruitments/{id}/applications")
    public ResponseEntity<Result<ApplicationVO>> submit(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CreateApplicationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(applicationService.submit(id, request)));
    }

    @GetMapping("/recruitments/{id}/applications")
    public Result<List<ApplicationVO>> listForRecruitment(@PathVariable @Positive Long id) {
        return Result.success(applicationService.listForRecruitment(id));
    }

    @GetMapping("/applications/me")
    public Result<List<ApplicationVO>> mine() {
        return Result.success(applicationService.listMine());
    }

    @PatchMapping("/applications/{id}/withdraw")
    public Result<ApplicationVO> withdraw(@PathVariable @Positive Long id) {
        return Result.success(applicationService.withdraw(id));
    }

    @PatchMapping("/applications/{id}/accept")
    public Result<ApplicationVO> accept(@PathVariable @Positive Long id) {
        return Result.success(applicationService.accept(id));
    }

    @PatchMapping("/applications/{id}/reject")
    public Result<ApplicationVO> reject(
            @PathVariable @Positive Long id,
            @Valid @RequestBody(required = false) RejectApplicationRequest request
    ) {
        return Result.success(applicationService.reject(id, request == null ? null : request.reason()));
    }

    @GetMapping("/recruitments/{id}/members")
    public Result<List<TeamMemberVO>> members(@PathVariable @Positive Long id) {
        return Result.success(applicationService.listMembers(id));
    }

    @DeleteMapping("/recruitments/{id}/members/{userId}")
    public Result<Void> removeMember(@PathVariable @Positive Long id, @PathVariable @Positive Long userId) {
        applicationService.removeMember(id, userId);
        return Result.success();
    }
}
