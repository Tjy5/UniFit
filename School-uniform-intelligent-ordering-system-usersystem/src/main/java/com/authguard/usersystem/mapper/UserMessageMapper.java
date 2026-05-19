package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.UserMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMessageMapper {

    // 根据 messageUserId 查询用户信息
    UserMessage selectByMessageUserId(Long messageUserId);

    // 插入用户信息
    int insertUserMessage(UserMessage userMessage);

    // 更新用户信息
    int updateUserMessage(UserMessage userMessage);
}