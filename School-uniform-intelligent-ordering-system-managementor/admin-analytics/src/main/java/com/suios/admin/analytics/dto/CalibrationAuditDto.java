package com.suios.admin.analytics.dto;

import java.util.Date;
import lombok.Data;

@Data
public class CalibrationAuditDto {
    private Long auditId;
    private Long paramId;
    private String action;
    private Object oldValue;
    private Object newValue;
    private String reason;
    private String operator;
    private Date createTime;
}
