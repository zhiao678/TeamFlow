package com.teamflow.backend.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserBlockMapper {
    @Select("""
            SELECT COUNT(*) FROM user_block
            WHERE (blocker_id = #{firstUserId} AND blocked_id = #{secondUserId})
               OR (blocker_id = #{secondUserId} AND blocked_id = #{firstUserId})
            """)
    long countBetween(@Param("firstUserId") Long firstUserId,
                      @Param("secondUserId") Long secondUserId);
}
