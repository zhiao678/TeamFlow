package com.teamflow.backend.service;

import com.teamflow.backend.dto.CreateRecruitmentRequest;
import com.teamflow.backend.dto.UpdateRecruitmentRequest;
import com.teamflow.backend.vo.RecruitmentVO;

import java.util.List;

public interface RecruitmentService {
    List<RecruitmentVO> listRecruitments();
    RecruitmentVO getRecruitment(Long id);
    RecruitmentVO createRecruitment(CreateRecruitmentRequest request);
    RecruitmentVO updateRecruitment(Long id, UpdateRecruitmentRequest request);
    RecruitmentVO publishRecruitment(Long id);
    RecruitmentVO closeRecruitment(Long id, String reason);
}
