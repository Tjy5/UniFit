package com.suios.admin.analytics.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class RecommendationEntryComparisonRowDto {

    private String requestSource;

    private Long exposureCount;

    private Long linkedCartCount;

    private Long linkedOrderCount;

    private Long adoptedCount;

    private BigDecimal cartConversionRate;

    private BigDecimal orderConversionRate;

    private BigDecimal adoptionRate;
}
