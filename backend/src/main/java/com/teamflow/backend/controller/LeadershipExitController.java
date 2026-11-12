package com.teamflow.backend.controller;

import com.teamflow.backend.common.result.Result;
import com.teamflow.backend.dto.CreateLeadershipExitRequest;
import com.teamflow.backend.service.LeadershipExitService;
import com.teamflow.backend.vo.LeaderTransferVO;
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
public class LeadershipExitController {
    private final LeadershipExitService leadershipExitService;

    public LeadershipExitController(LeadershipExitService leadershipExitService) {
        this.leadershipExitService = leadershipExitService;
    }

    @PostMapping("/recruitments/{id}/leader-exit-requests")
    public ResponseEntity<Result<LeaderTransferVO>> create(
            @PathVariable @Positive Long id,
            @Valid @RequestBody CreateLeadershipExitRequest body
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Result.success(leadershipExitService.create(id, body.successorId())));
    }

    @GetMapping("/leader-transfers/me")
    public Result<List<LeaderTransferVO>> mine() {
        return Result.success(leadershipExitService.listMine());
    }

    @PatchMapping("/leader-transfers/{id}/accept")
    public Result<LeaderTransferVO> accept(@PathVariable @Positive Long id) {
        return Result.success(leadershipExitService.accept(id));
    }

    @PatchMapping("/leader-transfers/{id}/reject")
    public Result<LeaderTransferVO> reject(@PathVariable @Positive Long id) {
        return Result.success(leadershipExitService.reject(id));
    }
}
