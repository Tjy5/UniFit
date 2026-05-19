package com.suios.admin.analytics.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class RecommendationStatsDto {
    private Long totalRecommendations = 0L;
    private Long linkedOrderCount = 0L;
    private BigDecimal linkedOrderCoverage = BigDecimal.ZERO;
    private Long totalFeedbacks = 0L;
    private BigDecimal feedbackCoverage = BigDecimal.ZERO;
    private Long fitCount = 0L;
    private Long tooLargeCount = 0L;
    private Long tooSmallCount = 0L;
    private Map<String, Long> satisfactionDistribution = new LinkedHashMap<>();
    private BigDecimal fitRate = BigDecimal.ZERO;
    private Long lowConfidenceCount = 0L;
    private BigDecimal lowConfidenceRate = BigDecimal.ZERO;
    private Integer avgConfidenceScore = 0;
    private List<RecommendationTrendPointDto> trend = new ArrayList<>();
}
