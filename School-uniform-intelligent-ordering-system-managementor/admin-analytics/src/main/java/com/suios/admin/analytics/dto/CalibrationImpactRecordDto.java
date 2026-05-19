package com.suios.admin.analytics.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class CalibrationImpactRecordDto {
    private Long logId;
    private Date createTime;
    private Long schoolId;
    private String schoolName;
    private Long uniformId;
    private String uniformName;
    private Long recommendedSizeId;
    private String recommendedSizeName;
    private Long baseBestSizeId;
    private String baseBestSizeName;
    private BigDecimal baseBestScore;
    private Long calibratedBestSizeId;
    private String calibratedBestSizeName;
    private BigDecimal calibratedBestScore;
    private Long finalBestSizeId;
    private String finalBestSizeName;
    private BigDecimal finalBestScore;
    private BigDecimal scoreDelta;
    private String attributionStatus;
    private List<CalibrationImpactParamHitDto> matchedParams = new ArrayList<>();
}
