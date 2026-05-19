package com.authguard.usersystem.service;

import com.authguard.usersystem.entity.UserMessage;

public interface UserMessageService {
    // 🔍 根据用户ID查询用户信息
    UserMessage getUserMessageByUserId(Long messageUserId);

    // ✏️ 更新用户信息（如果没有数据，则新增）
    boolean updateOrInsertUserMessage(UserMessage userMessage);
}
