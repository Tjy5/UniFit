package com.suios.admin.analytics.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CalibrationImpactParamHitDto {
    private Long sizeId;
    private Long paramId;
    private Integer version;
    private String calibrationType;
    private String source;
    private BigDecimal offset;
    private BigDecimal effectiveOffset;
    private Integer sampleSize;
    private Integer uniqueUserCount;
    private String confidenceLevel;
    private String status;
}
