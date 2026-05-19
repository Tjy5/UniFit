package com.suios.admin.activitylog.controller;

import com.suios.admin.activitylog.entity.SUserActivityLog;
import com.suios.admin.activitylog.service.SUserActivityLogService;
import com.suios.admin.common.page.PageResult;
import com.suios.admin.common.result.R;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/activity-logs")
@RequiredArgsConstructor
public class SUserActivityLogController {

    private final SUserActivityLogService activityLogService;

    @GetMapping("/list")
    public R<PageResult<SUserActivityLog>> list(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return R.success(activityLogService.list(pageNum, pageSize, userId, actionType, startTime, endTime));
    }

    @GetMapping("/{logId}")
    public R<SUserActivityLog> getInfo(@PathVariable Long logId) {
        return R.success(activityLogService.getById(logId));
    }
}
