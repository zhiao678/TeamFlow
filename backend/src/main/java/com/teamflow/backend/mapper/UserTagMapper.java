package com.teamflow.backend.mapper;

import com.teamflow.backend.entity.UserTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface UserTagMapper {

    @Select("SELECT user_id, tag_id FROM user_tag WHERE user_id = #{userId}")
    List<UserTag> selectByUserId(@Param("userId") Long userId);

    @Insert("INSERT INTO user_tag (user_id, tag_id) VALUES (#{userId}, #{tagId})")
    int insert(UserTag userTag);

    @Delete("DELETE FROM user_tag WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
