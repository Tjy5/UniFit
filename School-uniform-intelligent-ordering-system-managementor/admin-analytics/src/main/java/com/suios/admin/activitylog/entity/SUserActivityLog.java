package com.suios.admin.activitylog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("s_user_activity_logs")
public class SUserActivityLog {

    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    private Long userId;

    private String sessionId;

    private String actionType;

    private String targetType;

    private String targetId;

    private String logDetail;

    private String ipAddress;

    private String userAgent;

    private String requestUrl;

    private String requestMethod;

    private LocalDateTime logTime;

    @TableField(exist = false)
    private String userAccount;
}
