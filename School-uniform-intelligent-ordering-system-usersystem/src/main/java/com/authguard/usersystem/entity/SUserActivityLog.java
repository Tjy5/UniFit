package com.authguard.usersystem.entity;

import lombok.Data;
import lombok.Builder; // 可选，方便构建对象
import lombok.NoArgsConstructor; // 可选
import lombok.AllArgsConstructor; // 可选

import java.util.Date;

@Data
@Builder // Lombok Builder模式，方便创建对象
@NoArgsConstructor
@AllArgsConstructor
public class SUserActivityLog {
    private Long logId;
    private Long userId;
    private String sessionId;
    private String actionType;
    private String targetType;
    private String targetId;
    private String logDetail; // 可以是JSON字符串
    private String ipAddress;
    private String userAgent;
    private String requestUrl;
    private String requestMethod;
    private Date logTime; // 对应数据库的TIMESTAMP，通常由数据库自动生成或代码设置
}
