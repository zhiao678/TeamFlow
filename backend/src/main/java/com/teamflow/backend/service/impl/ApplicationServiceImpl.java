package com.teamflow.backend.service.impl;

import com.teamflow.backend.common.auth.CurrentUserContext;
import com.teamflow.backend.common.exception.BusinessException;
import com.teamflow.backend.common.exception.ErrorCode;
import com.teamflow.backend.dto.CreateApplicationRequest;
import com.teamflow.backend.entity.Application;
import com.teamflow.backend.entity.Recruitment;
import com.teamflow.backend.entity.TeamMember;
import com.teamflow.backend.mapper.ApplicationMapper;
import com.teamflow.backend.mapper.LeadershipExitMapper;
import com.teamflow.backend.mapper.MemberExitRequestMapper;
import com.teamflow.backend.mapper.RecruitmentMapper;
import com.teamflow.backend.mapper.TeamMemberMapper;
import com.teamflow.backend.service.ApplicationService;
import com.teamflow.backend.service.NotificationService;
import com.teamflow.backend.service.RecruitmentService;
import com.teamflow.backend.vo.ApplicationVO;
import com.teamflow.backend.vo.TeamMemberVO;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationMapper applicationMapper;
    private final RecruitmentMapper recruitmentMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final RecruitmentService recruitmentService;
    private final NotificationService notificationService;
    private final MemberExitRequestMapper memberExitRequestMapper;
    private final LeadershipExitMapper leadershipExitMapper;

    public ApplicationServiceImpl(
            ApplicationMapper applicationMapper,
            RecruitmentMapper recruitmentMapper,
            TeamMemberMapper teamMemberMapper,
            RecruitmentService recruitmentService,
            NotificationService notificationService,
            MemberExitRequestMapper memberExitRequestMapper,
            LeadershipExitMapper leadershipExitMapper
    ) {
        this.applicationMapper = applicationMapper;
        this.recruitmentMapper = recruitmentMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.recruitmentService = recruitmentService;
        this.notificationService = notificationService;
        this.memberExitRequestMapper = memberExitRequestMapper;
        this.leadershipExitMapper = leadershipExitMapper;
    }

    @Override
    @Transactional
    public ApplicationVO submit(Long recruitmentId, CreateApplicationRequest request) {
        Long userId = CurrentUserContext.require();
        Recruitment recruitment = lockRecruitment(recruitmentId);
        if ("DRAFT".equals(recruitment.getStatus()) && !userId.equals(recruitment.getOwnerId())) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "recruitment not found");
        }
        requireRecruiting(recruitment);
        if (recruitment.getDeadlineAt() != null && !recruitment.getDeadlineAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.CONFLICT, "recruitment deadline has passed");
        }
        TeamMember member = teamMemberMapper.selectByRecruitmentAndUser(recruitmentId, userId);
        if (userId.equals(recruitment.getOwnerId()) || member != null && "ACTIVE".equals(member.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "current team members cannot apply");
        }
        requireAvailableSlot(recruitment);
        if (applicationMapper.countPending(recruitmentId, userId) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "a pending application already exists");
        }

        Application application = new Application();
        application.setRecruitmentId(recruitmentId);
        application.setApplicantId(userId);
        application.setIntroduction(request.introduction());
        application.setSkillsDescription(request.skillsDescription());
        application.setAvailableTime(request.availableTime());
        application.setStatus("PENDING");
        try {
            applicationMapper.insert(application);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ErrorCode.CONFLICT, "a pending application already exists");
        }
        notifyApplication(recruitment.getOwnerId(), userId, application.getId(), "SUBMITTED",
                "收到新的入队申请", "有学生申请加入你的招募「" + recruitment.getTitle() + "」");
        return applicationMapper.selectView(application.getId());
    }

    @Override
    public List<ApplicationVO> listForRecruitment(Long recruitmentId) {
        Long userId = CurrentUserContext.require();
        Recruitment recruitment = recruitmentMapper.selectById(recruitmentId);
        if (recruitment == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "recruitment not found");
        requireOwner(recruitment, userId);
        return applicationMapper.selectForRecruitment(recruitmentId);
    }

    @Override
    public List<ApplicationVO> listMine() {
        return applicationMapper.selectForApplicant(CurrentUserContext.require());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ApplicationVO withdraw(Long id) {
        Long userId = CurrentUserContext.require();
        Recruitment recruitment = lockRecruitmentForApplication(id);
        Application application = requireApplicationForUpdate(id);
        if (!userId.equals(application.getApplicantId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only the applicant can withdraw this application");
        }
        requirePending(application);
        applicationMapper.process(id, "WITHDRAWN", null);
        notifyApplication(recruitment.getOwnerId(), userId, id, "WITHDRAWN",
                "入队申请已撤回", "一名学生撤回了招募「" + recruitment.getTitle() + "」的入队申请");
        return applicationMapper.selectView(id);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ApplicationVO accept(Long id) {
        Long userId = CurrentUserContext.require();
        Recruitment recruitment = lockRecruitmentForApplication(id);
        Application application = requireApplicationForUpdate(id);
        requireOwner(recruitment, userId);
        requirePending(application);
        requireRecruiting(recruitment);
        Long applicantId = application.getApplicantId();
        TeamMember member = teamMemberMapper.selectByRecruitmentAndUser(recruitment.getId(), applicantId);
        if (applicantId.equals(recruitment.getOwnerId()) || member != null && "ACTIVE".equals(member.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "applicant is already an active team member");
        }
        requireAvailableSlot(recruitment);
        if (member == null) {
            member = new TeamMember();
            member.setRecruitmentId(recruitment.getId());
            member.setUserId(applicantId);
            member.setRole("MEMBER");
            member.setStatus("ACTIVE");
            teamMemberMapper.insert(member);
        } else {
            teamMemberMapper.reactivateMember(recruitment.getId(), applicantId);
        }
        applicationMapper.process(id, "ACCEPTED", null);
        notifyApplication(applicantId, userId, id, "ACCEPTED",
                "入队申请已通过", "你加入招募「" + recruitment.getTitle() + "」的申请已通过");
        return applicationMapper.selectView(id);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ApplicationVO reject(Long id, String reason) {
        Long userId = CurrentUserContext.require();
        Recruitment recruitment = lockRecruitmentForApplication(id);
        Application application = requireApplicationForUpdate(id);
        requireOwner(recruitment, userId);
        requirePending(application);
        requireRecruiting(recruitment);
        applicationMapper.process(id, "REJECTED", reason);
        notifyApplication(application.getApplicantId(), userId, id, "REJECTED",
                "入队申请未通过", "你加入招募「" + recruitment.getTitle() + "」的申请未通过");
        return applicationMapper.selectView(id);
    }

    @Override
    public List<TeamMemberVO> listMembers(Long recruitmentId) {
        recruitmentService.getRecruitment(recruitmentId);
        return teamMemberMapper.selectActiveViews(recruitmentId);
    }

    @Override
    @Transactional
    public void removeMember(Long recruitmentId, Long userId) {
        Long operatorId = CurrentUserContext.require();
        Recruitment recruitment = lockRecruitment(recruitmentId);
        if (operatorId.equals(userId)) {
            TeamMember member = teamMemberMapper.selectByRecruitmentAndUser(recruitmentId, userId);
            if (!operatorId.equals(recruitment.getOwnerId()) && (member == null || !"ACTIVE".equals(member.getStatus()))) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "only active members can request an exit");
            }
            throw new BusinessException(ErrorCode.CONFLICT,
                    "members must request an approved exit and the leader must first transfer responsibility");
        }
        requireOwner(recruitment, operatorId);
        if (userId.equals(recruitment.getOwnerId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "the leader must first transfer responsibility");
        }
        TeamMember member = teamMemberMapper.selectByRecruitmentAndUser(recruitmentId, userId);
        if (member == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "team member not found");
        if (!"ACTIVE".equals(member.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "team member is no longer active");
        }
        teamMemberMapper.removeMember(recruitmentId, userId);
        teamMemberMapper.removeConversationMember(recruitmentId, userId);
        memberExitRequestMapper.expirePendingForMember(recruitmentId, userId);
        leadershipExitMapper.expirePendingForMember(recruitmentId, userId);
    }

    private Recruitment lockRecruitmentForApplication(Long id) {
        Long recruitmentId = applicationMapper.selectRecruitmentId(id);
        if (recruitmentId == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "application not found");
        return lockRecruitment(recruitmentId);
    }

    private Recruitment lockRecruitment(Long id) {
        Recruitment recruitment = recruitmentMapper.selectForUpdate(id);
        if (recruitment == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "recruitment not found");
        return recruitment;
    }

    private Application requireApplicationForUpdate(Long id) {
        Application application = applicationMapper.selectForUpdate(id);
        if (application == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "application not found");
        return application;
    }

    private void requireOwner(Recruitment recruitment, Long userId) {
        if (!userId.equals(recruitment.getOwnerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only the owner can manage this recruitment");
        }
    }

    private void requireRecruiting(Recruitment recruitment) {
        if (!"RECRUITING".equals(recruitment.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "recruitment is not recruiting");
        }
    }

    private void requirePending(Application application) {
        if (!"PENDING".equals(application.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "application has already been processed");
        }
    }

    private void requireAvailableSlot(Recruitment recruitment) {
        if (teamMemberMapper.countActive(recruitment.getId()) >= recruitment.getMaxMembers()) {
            throw new BusinessException(ErrorCode.CONFLICT, "recruitment has no remaining slots");
        }
    }

    private void notifyApplication(Long recipientId, Long actorId, Long applicationId, String action,
                                   String title, String content) {
        notificationService.create(recipientId, actorId, "APPLICATION_" + action,
                "application:" + applicationId + ":" + action.toLowerCase(java.util.Locale.ROOT),
                "APPLICATION", null, applicationId, title, content);
    }
}
