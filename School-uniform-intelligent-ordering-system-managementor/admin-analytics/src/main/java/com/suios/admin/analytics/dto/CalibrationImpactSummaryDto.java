package com.suios.admin.analytics.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CalibrationImpactSummaryDto {
    private long appliedCount;
    private long bestSizeChangedCount;
    private BigDecimal avgBaseBestScore = BigDecimal.ZERO;
    private BigDecimal avgCalibratedBestScore = BigDecimal.ZERO;
    private BigDecimal avgScoreDelta = BigDecimal.ZERO;
}
