package com.authguard.usersystem.entity;

import java.util.Date;
import lombok.Data;

@Data
public class CalibrationAudit {
    private Long auditId;
    private Long paramId;
    private String action;
    private String oldValue;
    private String newValue;
    private String reason;
    private String operator;
    private Date createTime;
}
