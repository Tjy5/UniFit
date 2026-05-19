package com.suios.admin.analytics.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class RecommendationAdoptionStatsDto {

    private Long linkedOrderCount;

    private Long adoptedCount;

    private Long sizeChangeCount;

    private Long withFeedbackCount;

    private Long withoutFeedbackCount;

    private BigDecimal adoptionRate;

    private BigDecimal sizeChangeRate;
}
