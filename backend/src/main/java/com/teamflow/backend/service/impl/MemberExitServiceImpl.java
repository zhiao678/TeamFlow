package com.teamflow.backend.service.impl;

import com.teamflow.backend.common.auth.CurrentUserContext;
import com.teamflow.backend.common.exception.BusinessException;
import com.teamflow.backend.common.exception.ErrorCode;
import com.teamflow.backend.entity.MemberExitRequest;
import com.teamflow.backend.entity.Recruitment;
import com.teamflow.backend.entity.TeamMember;
import com.teamflow.backend.mapper.MemberExitRequestMapper;
import com.teamflow.backend.mapper.LeadershipExitMapper;
import com.teamflow.backend.mapper.RecruitmentMapper;
import com.teamflow.backend.service.MemberExitService;
import com.teamflow.backend.service.NotificationService;
import com.teamflow.backend.vo.MemberExitRequestVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MemberExitServiceImpl implements MemberExitService {
    private final MemberExitRequestMapper exitRequestMapper;
    private final RecruitmentMapper recruitmentMapper;
    private final LeadershipExitMapper leadershipExitMapper;
    private final NotificationService notificationService;

    public MemberExitServiceImpl(MemberExitRequestMapper exitRequestMapper,
                                 RecruitmentMapper recruitmentMapper,
                                 LeadershipExitMapper leadershipExitMapper,
                                 NotificationService notificationService) {
        this.exitRequestMapper = exitRequestMapper;
        this.recruitmentMapper = recruitmentMapper;
        this.leadershipExitMapper = leadershipExitMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MemberExitRequestVO create(Long recruitmentId, String reason) {
        Long userId = CurrentUserContext.require();
        Recruitment recruitment = requireRecruitment(recruitmentId, true);
        TeamMember member = exitRequestMapper.selectMember(recruitmentId, userId);
        if (member == null || !"ACTIVE".equals(member.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "only active members can request to leave");
        }
        if (userId.equals(recruitment.getOwnerId()) || "LEADER".equals(member.getRole())) {
            throw new BusinessException(ErrorCode.CONFLICT, "the leader must choose a successor before leaving");
        }
        if (exitRequestMapper.selectPendingForMember(recruitmentId, userId) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "a pending exit request already exists");
        }

        MemberExitRequest request = new MemberExitRequest();
        request.setRecruitmentId(recruitmentId);
        request.setUserId(userId);
        request.setStatus("PENDING");
        request.setReason(reason);
        exitRequestMapper.insert(request);
        notify(recruitment.getOwnerId(), userId, request, "REQUESTED",
                "收到新的退出申请", "有成员申请退出队伍，请审核并沟通当前分工。");
        return exitRequestMapper.selectView(request.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberExitRequestVO> listForRecruitment(Long recruitmentId) {
        Long userId = CurrentUserContext.require();
        Recruitment recruitment = requireRecruitment(recruitmentId, false);
        requireOwner(recruitment, userId);
        return exitRequestMapper.selectForRecruitment(recruitmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemberExitRequestVO> listMine() {
        return exitRequestMapper.selectForUser(CurrentUserContext.require());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MemberExitRequestVO approve(Long id) {
        Long ownerId = CurrentUserContext.require();
        MemberExitRequest request = requireOwnedPendingRequest(id, ownerId);
        TeamMember member = exitRequestMapper.selectMember(request.getRecruitmentId(), request.getUserId());
        if (member == null || !"ACTIVE".equals(member.getStatus())) {
            exitRequestMapper.process(id, "EXPIRED", null, LocalDateTime.now());
            return exitRequestMapper.selectView(id);
        }
        if (ownerId.equals(request.getUserId()) || "LEADER".equals(member.getRole())) {
            throw new BusinessException(ErrorCode.CONFLICT, "the leader cannot approve their own exit");
        }

        LocalDateTime now = LocalDateTime.now();
        if (exitRequestMapper.leaveMember(request.getRecruitmentId(), request.getUserId(), now) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "member state changed");
        }
        exitRequestMapper.revokeConversationAccess(request.getRecruitmentId(), request.getUserId(), now);
        leadershipExitMapper.expirePendingForMember(request.getRecruitmentId(), request.getUserId());
        exitRequestMapper.process(id, "APPROVED", null, now);
        notify(request.getUserId(), ownerId, request, "APPROVED",
                "退出申请已批准", "负责人已批准你的退出申请，请与负责人沟通当前分工和待办事项。");
        return exitRequestMapper.selectView(id);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public MemberExitRequestVO reject(Long id, String reason) {
        Long ownerId = CurrentUserContext.require();
        MemberExitRequest request = requireOwnedPendingRequest(id, ownerId);
        TeamMember member = exitRequestMapper.selectMember(request.getRecruitmentId(), request.getUserId());
        if (member == null || !"ACTIVE".equals(member.getStatus())) {
            exitRequestMapper.process(id, "EXPIRED", null, LocalDateTime.now());
            return exitRequestMapper.selectView(id);
        }
        if (ownerId.equals(request.getUserId()) || "LEADER".equals(member.getRole())) {
            throw new BusinessException(ErrorCode.CONFLICT, "the leader cannot review their own exit");
        }
        exitRequestMapper.process(id, "REJECTED", reason, LocalDateTime.now());
        notify(request.getUserId(), ownerId, request, "REJECTED",
                "退出申请未通过", "负责人未批准你的退出申请，可联系负责人沟通。");
        return exitRequestMapper.selectView(id);
    }

    private MemberExitRequest requireOwnedPendingRequest(Long id, Long ownerId) {
        Long recruitmentId = exitRequestMapper.selectRecruitmentId(id);
        if (recruitmentId == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "exit request not found");
        }
        Recruitment recruitment = requireRecruitment(recruitmentId, true);
        requireOwner(recruitment, ownerId);
        MemberExitRequest request = exitRequestMapper.selectForUpdate(id);
        if (request == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "exit request not found");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "only pending exit requests can be reviewed");
        }
        return request;
    }

    private Recruitment requireRecruitment(Long id, boolean forUpdate) {
        Recruitment recruitment = forUpdate
                ? recruitmentMapper.selectForUpdate(id) : recruitmentMapper.selectById(id);
        if (recruitment == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "recruitment not found");
        }
        return recruitment;
    }

    private void requireOwner(Recruitment recruitment, Long userId) {
        if (!userId.equals(recruitment.getOwnerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only the owner can review exit requests");
        }
    }

    private void notify(Long recipientId, Long actorId, MemberExitRequest request,
                        String event, String title, String content) {
        notificationService.create(recipientId, actorId, "MEMBER_EXIT_" + event,
                "member-exit:" + request.getId() + ":" + event.toLowerCase(java.util.Locale.ROOT),
                "RECRUITMENT", request.getRecruitmentId(), null, title, content);
    }
}
