package com.authguard.usersystem.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.Data;

@Data
public class FeedbackAggregation {
    private String scopeType;
    private String scopeId;
    private Long targetSizeId;
    private int sampleSize;
    private int uniqueUserCount;
    private BigDecimal weightedFitCount = BigDecimal.ZERO;
    private BigDecimal weightedTooLargeCount = BigDecimal.ZERO;
    private BigDecimal weightedTooSmallCount = BigDecimal.ZERO;
    private BigDecimal totalWeightedCount = BigDecimal.ZERO;
    private String feedbackDistribution;

    public BigDecimal getWeightedFitRatio() {
        if (totalWeightedCount == null || totalWeightedCount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return weightedFitCount.divide(totalWeightedCount, 4, RoundingMode.HALF_UP);
    }
}
