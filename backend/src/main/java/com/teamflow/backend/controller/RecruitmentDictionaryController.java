package com.teamflow.backend.controller;

import com.teamflow.backend.common.result.Result;
import com.teamflow.backend.service.RecruitmentService;
import com.teamflow.backend.vo.CategoryVO;
import com.teamflow.backend.vo.TagVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RecruitmentDictionaryController {

    private final RecruitmentService recruitmentService;

    public RecruitmentDictionaryController(RecruitmentService recruitmentService) {
        this.recruitmentService = recruitmentService;
    }

    @GetMapping("/categories")
    public Result<List<CategoryVO>> categories() {
        return Result.success(recruitmentService.listCategories());
    }

    @GetMapping("/tags")
    public Result<List<TagVO>> tags() {
        return Result.success(recruitmentService.listTags());
    }
}
