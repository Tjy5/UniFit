package com.suios.admin.activitylog.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.suios.admin.activitylog.entity.SUserActivityLog;
import com.suios.admin.activitylog.mapper.SUserActivityLogMapper;
import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.page.PageResult;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class SUserActivityLogService {

    private final SUserActivityLogMapper activityLogMapper;

    public SUserActivityLogService(SUserActivityLogMapper activityLogMapper) {
        this.activityLogMapper = activityLogMapper;
    }

    public PageResult<SUserActivityLog> list(long pageNum,
                                             long pageSize,
                                             Long userId,
                                             String actionType,
                                             LocalDateTime startTime,
                                             LocalDateTime endTime) {
        IPage<SUserActivityLog> page = activityLogMapper.selectPageWithUser(
                new Page<>(pageNum, pageSize),
                userId,
                actionType,
                startTime,
                endTime
        );
        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    public SUserActivityLog getById(Long logId) {
        SUserActivityLog log = activityLogMapper.selectDetailById(logId);
        if (log == null) {
            throw new BizException("用户行为日志不存在");
        }
        return log;
    }
}
