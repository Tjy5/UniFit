package com.authguard.usersystem.entity;

import java.util.Date;
import lombok.Data;

@Data
public class RecommendationLog {
    private Long logId;
    private Long userId;
    private Long schoolId;
    private Long uniformId;
    private String categoryKey;
    private Long recommendedSizeId;
    private String recommendedSizeName;
    private Long orderItemId;
    private String experimentKey;
    private String experimentVariant;
    private String strategyVersion;
    private String requestSource;
    private String inputSummary;
    private String resultSummary;
    private Integer confidenceScore;
    private String confidenceLevel;
    private Boolean calibrationApplied;
    private String calibrationDetails;
    private Boolean personalizationApplied;
    private String personalizationDetails;
    private String createBy;
    private Date createTime;
}
