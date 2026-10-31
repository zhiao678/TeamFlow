package com.teamflow.backend.mapper;

import com.teamflow.backend.entity.RecruitmentTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;

public interface RecruitmentTagMapper {

    @Insert("INSERT INTO recruitment_tag (recruitment_id, tag_id) VALUES (#{recruitmentId}, #{tagId})")
    int insert(RecruitmentTag relation);

    @Delete("DELETE FROM recruitment_tag WHERE recruitment_id = #{recruitmentId}")
    int deleteByRecruitmentId(@Param("recruitmentId") Long recruitmentId);
}
