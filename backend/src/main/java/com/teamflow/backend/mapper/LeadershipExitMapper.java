package com.teamflow.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamflow.backend.entity.LeaderTransfer;
import com.teamflow.backend.vo.LeaderTransferVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

public interface LeadershipExitMapper extends BaseMapper<LeaderTransfer> {
    String VIEW_QUERY = """
            SELECT t.id, t.recruitment_id, r.title AS recruitment_title,
                   t.from_user_id, old_owner.display_name AS from_display_name,
                   t.to_user_id, new_owner.display_name AS to_display_name,
                   t.status, t.exit_after_transfer, t.created_at, t.processed_at
            FROM leader_transfer t
            JOIN recruitment r ON r.id = t.recruitment_id
            JOIN `user` old_owner ON old_owner.id = t.from_user_id
            JOIN `user` new_owner ON new_owner.id = t.to_user_id
            """;

    @Select("SELECT recruitment_id FROM leader_transfer WHERE id = #{id}")
    Long selectRecruitmentId(@Param("id") Long id);

    @Select("SELECT * FROM leader_transfer WHERE id = #{id} FOR UPDATE")
    LeaderTransfer selectForUpdate(@Param("id") Long id);

    @Select("SELECT * FROM leader_transfer WHERE recruitment_id = #{recruitmentId} AND status = 'PENDING' FOR UPDATE")
    LeaderTransfer selectPending(@Param("recruitmentId") Long recruitmentId);

    @Select(VIEW_QUERY + " WHERE t.id = #{id}")
    LeaderTransferVO selectView(@Param("id") Long id);

    @Select(VIEW_QUERY + " WHERE t.from_user_id = #{userId} OR t.to_user_id = #{userId} ORDER BY t.created_at DESC, t.id DESC")
    List<LeaderTransferVO> selectForUser(@Param("userId") Long userId);

    @Update("""
            UPDATE leader_transfer SET status = #{status}, processed_at = #{processedAt}
            WHERE id = #{id} AND status = 'PENDING'
            """)
    int process(@Param("id") Long id, @Param("status") String status,
                @Param("processedAt") LocalDateTime processedAt);

    @Update("""
            UPDATE leader_transfer SET status = 'EXPIRED', processed_at = CURRENT_TIMESTAMP
            WHERE recruitment_id = #{recruitmentId} AND status = 'PENDING'
              AND (from_user_id = #{userId} OR to_user_id = #{userId})
            """)
    int expirePendingForMember(@Param("recruitmentId") Long recruitmentId, @Param("userId") Long userId);

    @Update("""
            UPDATE team_member SET role = #{role}
            WHERE recruitment_id = #{recruitmentId} AND user_id = #{userId} AND status = 'ACTIVE'
            """)
    int changeRole(@Param("recruitmentId") Long recruitmentId, @Param("userId") Long userId,
                   @Param("role") String role);

    @Update("""
            UPDATE recruitment SET owner_id = #{newOwnerId}, updated_at = #{updatedAt}
            WHERE id = #{recruitmentId} AND owner_id = #{oldOwnerId}
            """)
    int changeOwner(@Param("recruitmentId") Long recruitmentId, @Param("oldOwnerId") Long oldOwnerId,
                    @Param("newOwnerId") Long newOwnerId, @Param("updatedAt") LocalDateTime updatedAt);
}
