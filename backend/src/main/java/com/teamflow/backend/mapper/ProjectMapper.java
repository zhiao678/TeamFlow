package com.teamflow.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.teamflow.backend.entity.Project;
import com.teamflow.backend.vo.ProjectDetailVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface ProjectMapper extends BaseMapper<Project> {

    @Select("""
            SELECT p.id,
                   p.name,
                   p.description,
                   p.owner_id,
                   u.display_name AS owner_display_name,
                   p.status,
                   p.created_at,
                   p.updated_at
            FROM `project` p
            JOIN `user` u ON u.id = p.owner_id
            ORDER BY p.created_at DESC, p.id DESC
            """)
    List<ProjectDetailVO> selectProjectList();

    @Select("""
            SELECT p.id,
                   p.name,
                   p.description,
                   p.owner_id,
                   u.display_name AS owner_display_name,
                   p.status,
                   p.created_at,
                   p.updated_at
            FROM `project` p
            JOIN `user` u ON u.id = p.owner_id
            WHERE p.id = #{id}
            """)
    ProjectDetailVO selectProjectDetailById(@Param("id") Long id);
}
