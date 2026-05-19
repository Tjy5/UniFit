package com.suios.admin.analytics.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class RecommendationExperimentVariantMetricsDto {
    private String experimentKey;
    private String experimentVariant;
    private Long totalRecommendations = 0L;
    private Long linkedOrderCount = 0L;
    private Long totalFeedbacks = 0L;
    private Long fitCount = 0L;
    private Long tooLargeCount = 0L;
    private Long tooSmallCount = 0L;
    private Long lowConfidenceCount = 0L;
    private Integer avgConfidenceScore = 0;
    private Long calibrationHitCount = 0L;
    private Long calibrationAppliedCount = 0L;
    private Long recommendationChangedCount = 0L;
    private Long safetyGateSkipCount = 0L;
    private BigDecimal fitRate = BigDecimal.ZERO;
    private BigDecimal lowConfidenceRate = BigDecimal.ZERO;
    private BigDecimal feedbackSubmissionRate = BigDecimal.ZERO;
    private BigDecimal recommendationToOrderConversionRate = BigDecimal.ZERO;
    private BigDecimal sizeIssueRate = BigDecimal.ZERO;
    private BigDecimal calibrationHitRate = BigDecimal.ZERO;
    private BigDecimal calibrationApplicationRate = BigDecimal.ZERO;
    private BigDecimal recommendationChangeRate = BigDecimal.ZERO;
}
