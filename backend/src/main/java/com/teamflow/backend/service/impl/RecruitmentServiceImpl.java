package com.teamflow.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.teamflow.backend.common.auth.CurrentUserContext;
import com.teamflow.backend.common.exception.BusinessException;
import com.teamflow.backend.common.exception.ErrorCode;
import com.teamflow.backend.dto.CreateRecruitmentRequest;
import com.teamflow.backend.dto.UpdateRecruitmentRequest;
import com.teamflow.backend.entity.Category;
import com.teamflow.backend.entity.Recruitment;
import com.teamflow.backend.entity.RecruitmentTag;
import com.teamflow.backend.entity.Tag;
import com.teamflow.backend.entity.TeamMember;
import com.teamflow.backend.mapper.CategoryMapper;
import com.teamflow.backend.mapper.RecruitmentMapper;
import com.teamflow.backend.mapper.RecruitmentTagMapper;
import com.teamflow.backend.mapper.TagMapper;
import com.teamflow.backend.mapper.TeamMemberMapper;
import com.teamflow.backend.service.RecruitmentService;
import com.teamflow.backend.vo.RecruitmentVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecruitmentServiceImpl implements RecruitmentService {

    private final RecruitmentMapper recruitmentMapper;
    private final RecruitmentTagMapper recruitmentTagMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    public RecruitmentServiceImpl(
            RecruitmentMapper recruitmentMapper,
            RecruitmentTagMapper recruitmentTagMapper,
            TeamMemberMapper teamMemberMapper,
            CategoryMapper categoryMapper,
            TagMapper tagMapper
    ) {
        this.recruitmentMapper = recruitmentMapper;
        this.recruitmentTagMapper = recruitmentTagMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
    }

    @Override
    public List<RecruitmentVO> listRecruitments() {
        Long userId = CurrentUserContext.get();
        return recruitmentMapper.selectVisibleList(userId).stream()
                .map(this::fillTags)
                .toList();
    }

    @Override
    public RecruitmentVO getRecruitment(Long id) {
        RecruitmentVO recruitment = recruitmentMapper.selectDetail(id);
        if (recruitment == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "recruitment not found");
        if ("DRAFT".equals(recruitment.getStatus())
                && !Long.valueOf(recruitment.getOwnerId()).equals(CurrentUserContext.get())) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "recruitment not found");
        }
        return fillTags(recruitment);
    }

    @Override
    @Transactional
    public RecruitmentVO createRecruitment(CreateRecruitmentRequest request) {
        Long ownerId = CurrentUserContext.require();
        requireCategory(request.categoryId());
        validateTags(request.tagIds());

        Recruitment recruitment = new Recruitment();
        recruitment.setOwnerId(ownerId);
        recruitment.setCategoryId(request.categoryId());
        recruitment.setTitle(request.title().trim());
        recruitment.setCourseName(request.courseName());
        recruitment.setTeacherName(request.teacherName());
        recruitment.setPurpose(request.purpose().trim());
        recruitment.setDescription(request.description());
        recruitment.setRequirements(request.requirements());
        recruitment.setMaxMembers(request.maxMembers());
        recruitment.setDeadlineAt(request.deadlineAt());
        recruitment.setStatus(request.publish() ? "RECRUITING" : "DRAFT");
        recruitmentMapper.insert(recruitment);

        addOwnerAsLeader(recruitment.getId(), ownerId);
        replaceTags(recruitment.getId(), request.tagIds());
        return getRecruitment(recruitment.getId());
    }

    @Override
    @Transactional
    public RecruitmentVO updateRecruitment(Long id, UpdateRecruitmentRequest request) {
        Recruitment recruitment = requireOwnedRecruitment(id);
        if ("TEAMED".equals(recruitment.getStatus()) || "CLOSED".equals(recruitment.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "recruitment cannot be edited in its current state");
        }
        if (request.categoryId() != null) {
            requireCategory(request.categoryId());
            recruitment.setCategoryId(request.categoryId());
        }
        if (request.title() != null) recruitment.setTitle(request.title().trim());
        if (request.courseName() != null) recruitment.setCourseName(request.courseName());
        if (request.teacherName() != null) recruitment.setTeacherName(request.teacherName());
        if (request.purpose() != null) recruitment.setPurpose(request.purpose().trim());
        if (request.description() != null) recruitment.setDescription(request.description());
        if (request.requirements() != null) recruitment.setRequirements(request.requirements());
        if (request.maxMembers() != null) {
            long memberCount = teamMemberMapper.selectCount(Wrappers.<TeamMember>lambdaQuery()
                    .eq(TeamMember::getRecruitmentId, id)
                    .eq(TeamMember::getStatus, "ACTIVE"));
            if (request.maxMembers() < memberCount) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "maxMembers cannot be less than current members");
            }
            recruitment.setMaxMembers(request.maxMembers());
        }
        if (request.deadlineAt() != null) recruitment.setDeadlineAt(request.deadlineAt());
        validateTags(request.tagIds());
        recruitmentMapper.updateById(recruitment);
        if (request.tagIds() != null) replaceTags(id, request.tagIds());
        return getRecruitment(id);
    }

    @Override
    @Transactional
    public RecruitmentVO publishRecruitment(Long id) {
        Recruitment recruitment = requireOwnedRecruitment(id);
        if (!"DRAFT".equals(recruitment.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "only draft recruitment can be published");
        }
        if (recruitment.getTitle() == null || recruitment.getPurpose() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "title and purpose are required");
        }
        recruitment.setStatus("RECRUITING");
        recruitmentMapper.updateById(recruitment);
        return getRecruitment(id);
    }

    @Override
    @Transactional
    public RecruitmentVO closeRecruitment(Long id, String reason) {
        Recruitment recruitment = requireOwnedRecruitment(id);
        if ("TEAMED".equals(recruitment.getStatus()) || "CLOSED".equals(recruitment.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "recruitment is already finished");
        }
        recruitment.setStatus("CLOSED");
        recruitment.setCloseReason(reason);
        recruitmentMapper.updateById(recruitment);
        return getRecruitment(id);
    }

    private Recruitment requireOwnedRecruitment(Long id) {
        Recruitment recruitment = recruitmentMapper.selectById(id);
        if (recruitment == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "recruitment not found");
        if (!recruitment.getOwnerId().equals(CurrentUserContext.require())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only the owner can manage this recruitment");
        }
        return recruitment;
    }

    private void requireCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);
        if (category == null || !Integer.valueOf(1).equals(category.getEnabled())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "category is invalid");
        }
    }

    private void validateTags(List<Long> tagIds) {
        if (tagIds == null) return;
        for (Long tagId : tagIds) {
            Tag tag = tagMapper.selectById(tagId);
            if (tag == null || !Integer.valueOf(1).equals(tag.getEnabled())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "tag is invalid");
            }
        }
    }

    private void addOwnerAsLeader(Long recruitmentId, Long ownerId) {
        TeamMember member = new TeamMember();
        member.setRecruitmentId(recruitmentId);
        member.setUserId(ownerId);
        member.setRole("LEADER");
        member.setStatus("ACTIVE");
        teamMemberMapper.insert(member);
    }

    private void replaceTags(Long recruitmentId, List<Long> tagIds) {
        recruitmentTagMapper.deleteByRecruitmentId(recruitmentId);
        if (tagIds == null) return;
        for (Long tagId : tagIds.stream().distinct().toList()) {
            RecruitmentTag relation = new RecruitmentTag();
            relation.setRecruitmentId(recruitmentId);
            relation.setTagId(tagId);
            recruitmentTagMapper.insert(relation);
        }
    }

    private RecruitmentVO fillTags(RecruitmentVO recruitment) {
        recruitment.setTags(recruitmentMapper.selectTagNames(recruitment.getId()));
        return recruitment;
    }
}
