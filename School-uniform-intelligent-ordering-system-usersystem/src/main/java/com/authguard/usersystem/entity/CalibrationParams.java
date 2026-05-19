package com.authguard.usersystem.entity;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

@Data
public class CalibrationParams {
    private Long paramId;
    private String scopeType;
    private String scopeId;
    private Long targetSizeId;
    private String calibrationType;
    private BigDecimal adjustmentValue;
    private Boolean isManual;
    private Boolean enabled;
    private String status;
    private Integer sampleSize;
    private Integer uniqueUserCount;
    private String feedbackDistribution;
    private String confidenceLevel;
    private Date effectiveFrom;
    private Date effectiveUntil;
    private Integer version;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}
