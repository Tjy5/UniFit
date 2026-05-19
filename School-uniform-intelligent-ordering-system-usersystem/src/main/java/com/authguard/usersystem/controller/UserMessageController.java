package com.authguard.usersystem.controller;

import com.authguard.usersystem.dto.ApiResponse;
import com.authguard.usersystem.entity.UserMessage;
import com.authguard.usersystem.exception.BizException;
import com.authguard.usersystem.exception.NotFoundException;
import com.authguard.usersystem.service.UserMessageService;
// ⭐ 1. Import
import com.authguard.usersystem.service.ISUserActivityLogService;
import jakarta.servlet.http.HttpServletRequest;
// End Import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-message")
public class UserMessageController {
    @Autowired
    private UserMessageService userMessageService;

    // ⭐ 2. Inject
    @Autowired
    private ISUserActivityLogService activityLogService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserMessage>> getUserMessage(
            @PathVariable("userId") Long userId,
            HttpServletRequest request // ⭐ 3.
    ) {
        // Assuming this might be called by an admin or the user themselves (if token implies userId)
        // For now, log based on path variable. If called by user, userId from token would be better.
        activityLogService.recordActivityAsync(userId, "VIEW_USER_MESSAGE", "USER_MESSAGE", userId.toString(), null, request);
        UserMessage userMessage = userMessageService.getUserMessageByUserId(userId);
        if (userMessage != null) {
            return ResponseEntity.ok(ApiResponse.success(userMessage));
        } else {
            activityLogService.recordActivityAsync(userId, "VIEW_USER_MESSAGE_FAILED_NOT_FOUND", "USER_MESSAGE", userId.toString(), "Not found", request);
            throw new NotFoundException("用户信息不存在");
        }
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<Void>> updateUserMessage(
            @RequestBody UserMessage userMessage,
            HttpServletRequest request // ⭐ 3.
    ) {
        if (userMessage == null || userMessage.getMessageUserId() == null) {
            activityLogService.recordActivityAsync(null, "UPDATE_USER_MESSAGE_FAILED_VALIDATION", "USER_MESSAGE_REQUEST", null, "messageUserId is null", request);
            throw new BizException("用户ID (messageUserId) 不能为空");
        }

        Long userIdForLog = userMessage.getMessageUserId(); // User ID being updated

        try {
            boolean result = userMessageService.updateOrInsertUserMessage(userMessage);
            if (result) {
                activityLogService.recordActivityAsync(userIdForLog, "UPDATE_USER_MESSAGE_SUCCESS", "USER_MESSAGE", userIdForLog.toString(), null, request);
                return ResponseEntity.ok(ApiResponse.success("更新成功", null));
            }
            activityLogService.recordActivityAsync(userIdForLog, "UPDATE_USER_MESSAGE_FAILED", "USER_MESSAGE", userIdForLog.toString(), null, request);
            throw new BizException("更新失败");
        } catch (IllegalArgumentException ex) {
            activityLogService.recordActivityAsync(userIdForLog, "UPDATE_USER_MESSAGE_FAILED_VALIDATION", "USER_MESSAGE", userIdForLog.toString(), ex.getMessage(), request);
            throw new BizException(ex.getMessage());
        }
    }
}
