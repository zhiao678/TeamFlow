package com.teamflow.backend.service.impl;

import com.teamflow.backend.common.auth.CurrentUserContext;
import com.teamflow.backend.common.exception.BusinessException;
import com.teamflow.backend.common.exception.ErrorCode;
import com.teamflow.backend.entity.LeaderTransfer;
import com.teamflow.backend.entity.Recruitment;
import com.teamflow.backend.entity.TeamMember;
import com.teamflow.backend.mapper.LeadershipExitMapper;
import com.teamflow.backend.mapper.MemberExitRequestMapper;
import com.teamflow.backend.mapper.RecruitmentMapper;
import com.teamflow.backend.service.LeadershipExitService;
import com.teamflow.backend.service.NotificationService;
import com.teamflow.backend.vo.LeaderTransferVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeadershipExitServiceImpl implements LeadershipExitService {
    private final LeadershipExitMapper leadershipExitMapper;
    private final MemberExitRequestMapper memberExitRequestMapper;
    private final RecruitmentMapper recruitmentMapper;
    private final NotificationService notificationService;

    public LeadershipExitServiceImpl(LeadershipExitMapper leadershipExitMapper,
                                     MemberExitRequestMapper memberExitRequestMapper,
                                     RecruitmentMapper recruitmentMapper,
                                     NotificationService notificationService) {
        this.leadershipExitMapper = leadershipExitMapper;
        this.memberExitRequestMapper = memberExitRequestMapper;
        this.recruitmentMapper = recruitmentMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LeaderTransferVO create(Long recruitmentId, Long successorId) {
        Long userId = CurrentUserContext.require();
        Recruitment recruitment = requireRecruitment(recruitmentId);
        if (!userId.equals(recruitment.getOwnerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only the owner can choose a successor");
        }
        if (userId.equals(successorId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "the successor must be another active member");
        }
        TeamMember owner = memberExitRequestMapper.selectMember(recruitmentId, userId);
        TeamMember successor = memberExitRequestMapper.selectMember(recruitmentId, successorId);
        if (!isActiveMember(owner) || !"LEADER".equals(owner.getRole())) {
            throw new BusinessException(ErrorCode.CONFLICT, "the owner must be an active leader");
        }
        if (!isActiveMember(successor) || !"MEMBER".equals(successor.getRole())) {
            throw new BusinessException(ErrorCode.CONFLICT, "the successor must be an active member");
        }
        LeaderTransfer pending = leadershipExitMapper.selectPending(recruitmentId);
        if (pending != null) {
            if (participantsAreCurrent(pending, recruitment)) {
                throw new BusinessException(ErrorCode.CONFLICT, "a pending leader transfer already exists");
            }
            leadershipExitMapper.process(pending.getId(), "EXPIRED", LocalDateTime.now());
        }

        LeaderTransfer transfer = new LeaderTransfer();
        transfer.setRecruitmentId(recruitmentId);
        transfer.setFromUserId(userId);
        transfer.setToUserId(successorId);
        transfer.setStatus("PENDING");
        transfer.setExitAfterTransfer(true);
        leadershipExitMapper.insert(transfer);
        notify(successorId, userId, transfer, "REQUESTED",
                "收到负责人继任邀请", "负责人申请退出并邀请你继任，请确认并沟通当前职责和工作内容。");
        return leadershipExitMapper.selectView(transfer.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderTransferVO> listMine() {
        return leadershipExitMapper.selectForUser(CurrentUserContext.require());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LeaderTransferVO accept(Long id) {
        Long successorId = CurrentUserContext.require();
        LeaderTransfer transfer = requireSuccessorPendingTransfer(id, successorId);
        Recruitment recruitment = requireRecruitment(transfer.getRecruitmentId());
        if (!participantsAreCurrent(transfer, recruitment)) {
            leadershipExitMapper.process(id, "EXPIRED", LocalDateTime.now());
            return leadershipExitMapper.selectView(id);
        }

        LocalDateTime now = LocalDateTime.now();
        Long recruitmentId = transfer.getRecruitmentId();
        memberExitRequestMapper.expirePendingForMember(recruitmentId, successorId);
        if (leadershipExitMapper.changeRole(recruitmentId, transfer.getFromUserId(), "MEMBER") != 1
                || leadershipExitMapper.changeRole(recruitmentId, successorId, "LEADER") != 1
                || leadershipExitMapper.changeOwner(recruitmentId, transfer.getFromUserId(), successorId, now) != 1) {
            throw new BusinessException(ErrorCode.CONFLICT, "leader or member state changed");
        }
        if (Boolean.TRUE.equals(transfer.getExitAfterTransfer())) {
            if (memberExitRequestMapper.leaveMember(recruitmentId, transfer.getFromUserId(), now) != 1) {
                throw new BusinessException(ErrorCode.CONFLICT, "outgoing leader state changed");
            }
            memberExitRequestMapper.revokeConversationAccess(recruitmentId, transfer.getFromUserId(), now);
            memberExitRequestMapper.expirePendingForMember(recruitmentId, transfer.getFromUserId());
        }
        leadershipExitMapper.process(id, "ACCEPTED", now);
        notify(transfer.getFromUserId(), successorId, transfer, "ACCEPTED",
                "负责人继任已确认", "继任者已确认接任，请与对方沟通当前职责和工作内容。");
        return leadershipExitMapper.selectView(id);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public LeaderTransferVO reject(Long id) {
        Long successorId = CurrentUserContext.require();
        LeaderTransfer transfer = requireSuccessorPendingTransfer(id, successorId);
        Recruitment recruitment = requireRecruitment(transfer.getRecruitmentId());
        if (!participantsAreCurrent(transfer, recruitment)) {
            leadershipExitMapper.process(id, "EXPIRED", LocalDateTime.now());
            return leadershipExitMapper.selectView(id);
        }
        leadershipExitMapper.process(id, "REJECTED", LocalDateTime.now());
        notify(transfer.getFromUserId(), successorId, transfer, "REJECTED",
                "负责人继任未接受", "对方未接受继任邀请，当前负责人和成员关系保持有效。");
        return leadershipExitMapper.selectView(id);
    }

    private LeaderTransfer requireSuccessorPendingTransfer(Long id, Long successorId) {
        Long recruitmentId = leadershipExitMapper.selectRecruitmentId(id);
        if (recruitmentId == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "leader transfer not found");
        }
        requireRecruitment(recruitmentId);
        LeaderTransfer transfer = leadershipExitMapper.selectForUpdate(id);
        if (transfer == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "leader transfer not found");
        }
        if (!successorId.equals(transfer.getToUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only the selected successor can respond");
        }
        if (!"PENDING".equals(transfer.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "only pending leader transfers can be processed");
        }
        return transfer;
    }

    private Recruitment requireRecruitment(Long id) {
        Recruitment recruitment = recruitmentMapper.selectForUpdate(id);
        if (recruitment == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "recruitment not found");
        }
        return recruitment;
    }

    private boolean participantsAreCurrent(LeaderTransfer transfer, Recruitment recruitment) {
        if (!transfer.getFromUserId().equals(recruitment.getOwnerId())) {
            return false;
        }
        TeamMember owner = memberExitRequestMapper.selectMember(transfer.getRecruitmentId(), transfer.getFromUserId());
        TeamMember successor = memberExitRequestMapper.selectMember(transfer.getRecruitmentId(), transfer.getToUserId());
        return isActiveMember(owner) && "LEADER".equals(owner.getRole())
                && isActiveMember(successor) && "MEMBER".equals(successor.getRole());
    }

    private boolean isActiveMember(TeamMember member) {
        return member != null && "ACTIVE".equals(member.getStatus());
    }

    private void notify(Long recipientId, Long actorId, LeaderTransfer transfer,
                        String event, String title, String content) {
        notificationService.create(recipientId, actorId, "LEADER_TRANSFER_" + event,
                "leader-transfer:" + transfer.getId() + ":" + event.toLowerCase(java.util.Locale.ROOT),
                "LEADER_TRANSFER", null, null, transfer.getId(), title, content);
    }
}
