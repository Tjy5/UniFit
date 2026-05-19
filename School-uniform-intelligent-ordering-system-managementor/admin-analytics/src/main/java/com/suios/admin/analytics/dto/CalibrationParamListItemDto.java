package com.suios.admin.analytics.dto;

import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

@Data
public class CalibrationParamListItemDto {
    private Long paramId;
    private String scopeType;
    private String scopeId;
    private String scopeName;
    private Long targetSizeId;
    private String targetSizeName;
    private String calibrationType;
    private BigDecimal adjustmentValue;
    private Boolean isManual;
    private Boolean enabled;
    private String status;
    private Integer sampleSize;
    private Integer uniqueUserCount;
    private Object feedbackDistribution;
    private String confidenceLevel;
    private Date effectiveFrom;
    private Date effectiveUntil;
    private Integer version;
    private Boolean latestVersion;
    private Boolean effectiveNow;
    private Boolean shadowedByHigherVersion;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}
