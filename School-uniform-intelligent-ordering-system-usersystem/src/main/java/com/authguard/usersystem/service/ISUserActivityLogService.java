package com.authguard.usersystem.service;

import com.authguard.usersystem.entity.SUserActivityLog;
import jakarta.servlet.http.HttpServletRequest; // 注意是 jakarta.servlet

public interface ISUserActivityLogService {

    /**
     * 异步记录用户行为日志
     * @param userId 用户ID (可为null)
     * @param actionType 行为类型
     * @param targetType 目标类型 (可为null)
     * @param targetId 目标ID (可为null)
     * @param logDetail 日志详情 (可为null)
     * @param request HttpServletRequest 对象，用于获取IP, User-Agent, URL, Method
     */
    void recordActivityAsync(Long userId, String actionType, String targetType, String targetId, String logDetail, HttpServletRequest request);

    /**
     * 直接记录 SUserActivityLog 对象 (如果外部已构建好)
     * @param log 日志对象
     */
    void recordActivityAsync(SUserActivityLog log);
}
