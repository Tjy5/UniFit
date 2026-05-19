package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.UserAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserAccountMapper {
    // 插入用户（注册）
    int insertUser(UserAccount user);

    // 通过账号查询用户（用于登录、获取用户信息）
    UserAccount findByAccount(@Param("userAccount") String userAccount);

    // 更新用户密码
    int updatePassword(@Param("userAccount") String userAccount,
                       @Param("oldPassword") String oldPassword, // 注意：您之前的XML中没有使用oldPassword
                       @Param("newPassword") String newPassword);

    // ⭐ 新增方法: 通过用户ID查询用户信息 (不包含密码)
    UserAccount selectUserById(@Param("userId") Long userId);
}
