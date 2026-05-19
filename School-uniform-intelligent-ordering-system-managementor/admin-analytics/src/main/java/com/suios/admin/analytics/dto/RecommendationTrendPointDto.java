package com.suios.admin.analytics.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class RecommendationTrendPointDto {
    private String day;
    private Long totalRecommendations;
    private Long linkedOrderCount;
    private Long totalFeedbacks;
    private Long fitCount;
    private Long tooLargeCount;
    private Long tooSmallCount;
    private BigDecimal fitRate;
    private Integer avgConfidenceScore;
}
