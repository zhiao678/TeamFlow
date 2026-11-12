package com.teamflow.backend.service;

import com.teamflow.backend.dto.CreateApplicationRequest;
import com.teamflow.backend.vo.ApplicationVO;
import com.teamflow.backend.vo.TeamMemberVO;

import java.util.List;

public interface ApplicationService {
    ApplicationVO submit(Long recruitmentId, CreateApplicationRequest request);
    List<ApplicationVO> listForRecruitment(Long recruitmentId);
    List<ApplicationVO> listMine();
    ApplicationVO withdraw(Long id);
    ApplicationVO accept(Long id);
    ApplicationVO reject(Long id, String reason);
    List<TeamMemberVO> listMembers(Long recruitmentId);
    void removeMember(Long recruitmentId, Long userId);
}
