package com.teamflow.backend.service;

import com.teamflow.backend.dto.CreateRecruitmentRequest;
import com.teamflow.backend.dto.RecruitmentQueryRequest;
import com.teamflow.backend.dto.UpdateRecruitmentRequest;
import com.teamflow.backend.vo.CategoryVO;
import com.teamflow.backend.vo.RecruitmentPageVO;
import com.teamflow.backend.vo.RecruitmentVO;
import com.teamflow.backend.vo.TagVO;

import java.util.List;

public interface RecruitmentService {
    RecruitmentPageVO listRecruitments(RecruitmentQueryRequest request);
    List<RecruitmentVO> listMyRecruitments();
    List<CategoryVO> listCategories();
    List<TagVO> listTags();
    RecruitmentVO getRecruitment(Long id);
    RecruitmentVO createRecruitment(CreateRecruitmentRequest request);
    RecruitmentVO updateRecruitment(Long id, UpdateRecruitmentRequest request);
    RecruitmentVO publishRecruitment(Long id);
    RecruitmentVO closeRecruitment(Long id, String reason);
    void deleteDraft(Long id);
}
