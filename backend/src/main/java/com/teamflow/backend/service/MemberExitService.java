package com.teamflow.backend.service;

import com.teamflow.backend.vo.MemberExitRequestVO;

import java.util.List;

public interface MemberExitService {
    MemberExitRequestVO create(Long recruitmentId, String reason);

    List<MemberExitRequestVO> listForRecruitment(Long recruitmentId);

    List<MemberExitRequestVO> listMine();

    MemberExitRequestVO approve(Long id);

    MemberExitRequestVO reject(Long id, String reason);
}
