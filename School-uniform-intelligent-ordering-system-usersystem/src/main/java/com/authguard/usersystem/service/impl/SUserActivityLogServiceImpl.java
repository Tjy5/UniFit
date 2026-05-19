package com.authguard.usersystem.service.impl;

import com.authguard.usersystem.entity.SUserActivityLog;
import com.authguard.usersystem.mapper.ISUserActivityLogMapper;
import com.authguard.usersystem.service.ISUserActivityLogService;
import com.authguard.usersystem.util.IpUtils; // 假设你有一个获取IP的工具类
import jakarta.servlet.http.HttpServletRequest; // 注意是 jakarta.servlet
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async; // 引入异步注解
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class SUserActivityLogServiceImpl implements ISUserActivityLogService {

    private static final Logger logger = LoggerFactory.getLogger(SUserActivityLogServiceImpl.class);

    @Autowired
    private ISUserActivityLogMapper activityLogMapper;

    @Override
    @Async // 标记为异步方法
    public void recordActivityAsync(Long userId, String actionType, String targetType, String targetId, String logDetail, HttpServletRequest request) {
        try {
            SUserActivityLog log = SUserActivityLog.builder()
                    .userId(userId)
                    .sessionId(request != null ? request.getSession().getId() : null) // 获取sessionId
                    .actionType(actionType)
                    .targetType(targetType)
                    .targetId(targetId)
                    .logDetail(logDetail)
                    .ipAddress(request != null ? IpUtils.getIpAddr(request) : null) // 使用工具类获取IP
                    .userAgent(request != null ? request.getHeader("User-Agent") : null)
                    .requestUrl(request != null ? request.getRequestURI() : null)
                    .requestMethod(request != null ? request.getMethod() : null)
                    .logTime(new Date()) // 由应用设置时间
                    .build();
            activityLogMapper.insertLog(log);
        } catch (Exception e) {
            logger.error("Error recording user activity log asynchronously: actionType={}, userId={}", actionType, userId, e);
        }
    }

    @Override
    @Async // 标记为异步方法
    public void recordActivityAsync(SUserActivityLog log) {
        try {
            if (log.getLogTime() == null) {
                log.setLogTime(new Date()); // 确保有时间
            }
            activityLogMapper.insertLog(log);
        } catch (Exception e) {
            logger.error("Error recording pre-built user activity log asynchronously: actionType={}, userId={}", log.getActionType(), log.getUserId(), e);
        }
    }
}
