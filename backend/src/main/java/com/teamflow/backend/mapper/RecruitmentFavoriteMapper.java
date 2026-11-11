package com.teamflow.backend.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface RecruitmentFavoriteMapper {
    @Insert("INSERT INTO recruitment_favorite (user_id, recruitment_id) VALUES (#{userId}, #{recruitmentId}) ON DUPLICATE KEY UPDATE created_at = created_at")
    int insertFavoriteIfAbsent(@Param("userId") Long userId, @Param("recruitmentId") Long recruitmentId);

    @Delete("DELETE FROM recruitment_favorite WHERE user_id = #{userId} AND recruitment_id = #{recruitmentId}")
    int deleteFavorite(@Param("userId") Long userId, @Param("recruitmentId") Long recruitmentId);

    @Select("SELECT COUNT(*) > 0 FROM recruitment_favorite WHERE user_id = #{userId} AND recruitment_id = #{recruitmentId}")
    boolean exists(@Param("userId") Long userId, @Param("recruitmentId") Long recruitmentId);

    @Select("SELECT recruitment_id FROM recruitment_favorite WHERE user_id = #{userId} ORDER BY created_at DESC, recruitment_id DESC")
    List<Long> selectRecruitmentIds(@Param("userId") Long userId);
}
