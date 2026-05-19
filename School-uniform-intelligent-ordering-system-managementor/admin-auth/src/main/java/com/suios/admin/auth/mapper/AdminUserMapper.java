package com.suios.admin.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.suios.admin.auth.entity.AdminUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface AdminUserMapper extends BaseMapper<AdminUser> {

    @Select("""
            SELECT user_id, username, password, nickname, avatar, status, create_time, update_time, last_login
            FROM admin_user
            WHERE username = #{username}
            LIMIT 1
            """)
    AdminUser selectByUsername(@Param("username") String username);

    @Update("""
            UPDATE admin_user
            SET last_login = NOW(), update_time = NOW()
            WHERE user_id = #{userId}
            """)
    int touchLastLogin(@Param("userId") Long userId);
}
