package com.suios.admin.analytics.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class SupplyForecastDto {

    private Long skuId;

    private Long uniformId;

    private Long sizeId;

    private String uniformName;

    private String schoolName;

    private String gradeName;

    private String categoryKey;

    private String sizeName;

    private Long availableQuantity;

    private Long safetyStock;

    private Long reorderPoint;

    private Integer leadTimeDays;

    private Long historicalQuantity;

    private Long salesDayCount;

    private Integer lookbackDays;

    private Integer horizonDays;

    private BigDecimal dailyAverage;

    private Long forecastDemand;

    private BigDecimal dataCompleteness;

    private List<String> reasonCodes;
}
