package com.teamflow.backend.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.teamflow.backend.common.auth.CurrentUserContext;
import com.teamflow.backend.common.exception.BusinessException;
import com.teamflow.backend.common.exception.ErrorCode;
import com.teamflow.backend.dto.CreateRecruitmentRequest;
import com.teamflow.backend.dto.RecruitmentQueryRequest;
import com.teamflow.backend.dto.UpdateRecruitmentRequest;
import com.teamflow.backend.entity.Application;
import com.teamflow.backend.entity.Category;
import com.teamflow.backend.entity.Recruitment;
import com.teamflow.backend.entity.RecruitmentTag;
import com.teamflow.backend.entity.Tag;
import com.teamflow.backend.entity.TeamMember;
import com.teamflow.backend.mapper.ApplicationMapper;
import com.teamflow.backend.mapper.CategoryMapper;
import com.teamflow.backend.mapper.RecruitmentMapper;
import com.teamflow.backend.mapper.RecruitmentFavoriteMapper;
import com.teamflow.backend.mapper.RecruitmentTagMapper;
import com.teamflow.backend.mapper.TagMapper;
import com.teamflow.backend.mapper.TeamMemberMapper;
import com.teamflow.backend.service.NotificationService;
import com.teamflow.backend.service.RecruitmentService;
import com.teamflow.backend.vo.CategoryVO;
import com.teamflow.backend.vo.RecruitmentPageVO;
import com.teamflow.backend.vo.RecruitmentVO;
import com.teamflow.backend.vo.TagVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecruitmentServiceImpl implements RecruitmentService {

    private final RecruitmentMapper recruitmentMapper;
    private final RecruitmentTagMapper recruitmentTagMapper;
    private final TeamMemberMapper teamMemberMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final RecruitmentFavoriteMapper recruitmentFavoriteMapper;
    private final ApplicationMapper applicationMapper;
    private final NotificationService notificationService;

    public RecruitmentServiceImpl(
            RecruitmentMapper recruitmentMapper,
            RecruitmentTagMapper recruitmentTagMapper,
            TeamMemberMapper teamMemberMapper,
            CategoryMapper categoryMapper,
            TagMapper tagMapper,
            RecruitmentFavoriteMapper recruitmentFavoriteMapper,
            ApplicationMapper applicationMapper,
            NotificationService notificationService
    ) {
        this.recruitmentMapper = recruitmentMapper;
        this.recruitmentTagMapper = recruitmentTagMapper;
        this.teamMemberMapper = teamMemberMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.recruitmentFavoriteMapper = recruitmentFavoriteMapper;
        this.applicationMapper = applicationMapper;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public RecruitmentPageVO listRecruitments(RecruitmentQueryRequest request) {
        Long userId = CurrentUserContext.get();
        long total = recruitmentMapper.countPage(request, userId);
        long offset = ((long) request.page() - 1) * request.pageSize();
        List<RecruitmentVO> records = recruitmentMapper.selectPage(request, userId, offset).stream()
                .map(this::fillTags)
                .map(this::fillFavorite)
                .toList();
        return new RecruitmentPageVO(records, request.page(), request.pageSize(), total);
    }

    @Override
    public List<RecruitmentVO> listMyRecruitments() {
        return recruitmentMapper.selectMine(CurrentUserContext.require()).stream()
                .map(this::fillTags)
                .toList();
    }

    @Override
    public List<CategoryVO> listCategories() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                        .eq(Category::getEnabled, 1)
                        .orderByAsc(Category::getSortOrder, Category::getId)).stream()
                .map(category -> new CategoryVO(category.getId(), category.getName(),
                        category.getParentId(), category.getSortOrder()))
                .toList();
    }

    @Override
    public List<TagVO> listTags() {
        return tagMapper.selectList(Wrappers.<Tag>lambdaQuery()
                        .eq(Tag::getEnabled, 1)
                        .orderByAsc(Tag::getName, Tag::getId)).stream()
                .map(tag -> new TagVO(tag.getId(), tag.getName()))
                .toList();
    }

    @Override
    public RecruitmentVO getRecruitment(Long id) {
        RecruitmentVO recruitment = recruitmentMapper.selectDetail(id);
        if (recruitment == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "recruitment not found");
        if ("DRAFT".equals(recruitment.getStatus())
                && !recruitment.getOwnerId().equals(CurrentUserContext.get())) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "recruitment not found");
        }
        fillTags(recruitment);
        fillFavorite(recruitment);
        return recruitment;
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
        if (!"DRAFT".equals(recruitment.getStatus()) && !"RECRUITING".equals(recruitment.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "recruitment cannot be edited in its current state");
        }
        if ("RECRUITING".equals(recruitment.getStatus())
                && (request.categoryId() != null || request.title() != null
                || request.courseName() != null || request.teacherName() != null || request.purpose() != null)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "only description, requirements, tags, deadline and maxMembers can be edited while recruiting");
        }
        if (request.categoryId() != null) {
            requireCategory(request.categoryId());
            recruitment.setCategoryId(request.categoryId());
        }
        if (request.title() != null) {
            if (request.title().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "title cannot be blank");
            }
            recruitment.setTitle(request.title().trim());
        }
        if (request.courseName() != null) recruitment.setCourseName(request.courseName());
        if (request.teacherName() != null) recruitment.setTeacherName(request.teacherName());
        if (request.purpose() != null) {
            if (request.purpose().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "purpose cannot be blank");
            }
            recruitment.setPurpose(request.purpose().trim());
        }
        if (request.description() != null) recruitment.setDescription(request.description());
        if (request.requirements() != null) recruitment.setRequirements(request.requirements());
        if (request.maxMembers() != null) {
            validateCapacity(id, request.maxMembers());
            recruitment.setMaxMembers(request.maxMembers());
        }
        if (request.deadlineAt() != null) recruitment.setDeadlineAt(request.deadlineAt());
        validateTags(request.tagIds());
        recruitment.setUpdatedAt(LocalDateTime.now());
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
        if (recruitment.getTitle() == null || recruitment.getTitle().isBlank()
                || recruitment.getPurpose() == null || recruitment.getPurpose().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "title and purpose are required");
        }
        requireCategory(recruitment.getCategoryId());
        validateTags(recruitmentTagMapper.selectTagIds(id));
        if (recruitment.getMaxMembers() == null || recruitment.getMaxMembers() < 2) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "maxMembers must be at least 2");
        }
        validateCapacity(id, recruitment.getMaxMembers());
        recruitment.setStatus("RECRUITING");
        recruitment.setUpdatedAt(LocalDateTime.now());
        recruitmentMapper.updateById(recruitment);
        return getRecruitment(id);
    }

    @Override
    @Transactional
    public RecruitmentVO closeRecruitment(Long id, String reason) {
        Recruitment recruitment = requireOwnedRecruitment(id);
        if (!"RECRUITING".equals(recruitment.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "only recruiting recruitment can be closed");
        }
        recruitment.setStatus("CLOSED");
        recruitment.setCloseReason(reason);
        recruitment.setUpdatedAt(LocalDateTime.now());
        recruitmentMapper.updateById(recruitment);
        for (Application application : applicationMapper.selectPendingForUpdate(id)) {
            applicationMapper.process(application.getId(), "EXPIRED", null);
            notificationService.create(application.getApplicantId(), recruitment.getOwnerId(),
                    "APPLICATION_EXPIRED", "application:" + application.getId() + ":expired",
                    "APPLICATION", null, application.getId(), "入队申请已失效",
                    "招募「" + recruitment.getTitle() + "」已关闭，你的待处理申请已失效");
        }
        return getRecruitment(id);
    }

    @Override
    @Transactional
    public void deleteDraft(Long id) {
        Recruitment recruitment = requireOwnedRecruitment(id);
        if (!"DRAFT".equals(recruitment.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "only draft recruitment can be deleted");
        }
        recruitmentTagMapper.deleteByRecruitmentId(id);
        teamMemberMapper.delete(Wrappers.<TeamMember>lambdaQuery().eq(TeamMember::getRecruitmentId, id));
        recruitmentMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void favorite(Long id) {
        Long userId = CurrentUserContext.require();
        Recruitment recruitment = recruitmentMapper.selectById(id);
        if (recruitment == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "recruitment not found");
        if ("DRAFT".equals(recruitment.getStatus())) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "recruitment not found");
        }
        recruitmentFavoriteMapper.insertFavoriteIfAbsent(userId, id);
    }

    @Override
    @Transactional
    public void unfavorite(Long id) {
        recruitmentFavoriteMapper.deleteFavorite(CurrentUserContext.require(), id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecruitmentVO> listMyFavorites() {
        Long userId = CurrentUserContext.require();
        return recruitmentFavoriteMapper.selectRecruitmentIds(userId).stream()
                .map(recruitmentMapper::selectDetail)
                .filter(java.util.Objects::nonNull)
                .filter(item -> !"DRAFT".equals(item.getStatus()) || userId.equals(item.getOwnerId()))
                .map(this::fillTags)
                .map(item -> {
                    item.setFavorited(true);
                    return item;
                })
                .toList();
    }

    private RecruitmentVO fillFavorite(RecruitmentVO recruitment) {
        Long userId = CurrentUserContext.get();
        recruitment.setFavorited(userId != null && recruitmentFavoriteMapper.exists(userId, recruitment.getId()));
        return recruitment;
    }

    private Recruitment requireOwnedRecruitment(Long id) {
        Long userId = CurrentUserContext.require();
        Recruitment recruitment = recruitmentMapper.selectForUpdate(id);
        if (recruitment == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "recruitment not found");
        if (!recruitment.getOwnerId().equals(userId)) {
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

    private void validateCapacity(Long recruitmentId, int maxMembers) {
        long memberCount = teamMemberMapper.selectCount(Wrappers.<TeamMember>lambdaQuery()
                .eq(TeamMember::getRecruitmentId, recruitmentId)
                .eq(TeamMember::getStatus, "ACTIVE"));
        if (maxMembers < memberCount) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "maxMembers cannot be less than current members");
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
