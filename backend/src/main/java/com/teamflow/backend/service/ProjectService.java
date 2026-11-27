package com.teamflow.backend.service;

import com.teamflow.backend.entity.Project;
import com.teamflow.backend.entity.Recruitment;
import com.teamflow.backend.entity.TeamMember;
import com.teamflow.backend.vo.ProjectDetailVO;
import com.teamflow.backend.vo.ProjectMemberVO;

import java.util.List;

public interface ProjectService {

    List<ProjectDetailVO> listProjects();

    ProjectDetailVO getProjectById(Long id);

    List<ProjectMemberVO> listMembers(Long id);

    Project createForFormedRecruitment(Recruitment recruitment, List<TeamMember> activeMembers);

    void revokeMember(Long recruitmentId, Long userId, String status);

    void transferLeader(Long recruitmentId, Long formerLeaderId, Long newLeaderId);
}
