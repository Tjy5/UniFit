package com.suios.admin.activitylog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.activitylog.entity.SUserActivityLog;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SUserActivityLogMapper extends BaseMapper<SUserActivityLog> {

    @Select("""
            <script>
            SELECT l.*,
                   ua.user_account AS user_account
            FROM s_user_activity_logs l
            LEFT JOIN user_account ua ON l.user_id = ua.user_id
            <where>
                <if test="userId != null">
                    AND l.user_id = #{userId}
                </if>
                <if test="actionType != null and actionType != ''">
                    AND l.action_type LIKE CONCAT('%', #{actionType}, '%')
                </if>
                <if test="startTime != null">
                    AND l.log_time <![CDATA[>=]]> #{startTime}
                </if>
                <if test="endTime != null">
                    AND l.log_time <![CDATA[<=]]> #{endTime}
                </if>
            </where>
            ORDER BY l.log_time DESC, l.log_id DESC
            </script>
            """)
    IPage<SUserActivityLog> selectPageWithUser(Page<SUserActivityLog> page,
                                               @Param("userId") Long userId,
                                               @Param("actionType") String actionType,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);

    @Select("""
            SELECT l.*,
                   ua.user_account AS user_account
            FROM s_user_activity_logs l
            LEFT JOIN user_account ua ON l.user_id = ua.user_id
            WHERE l.log_id = #{logId}
            """)
    SUserActivityLog selectDetailById(@Param("logId") Long logId);
}
