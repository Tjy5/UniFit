package com.suios.admin.analytics.dto;

import lombok.Data;

@Data
public class ReplenishmentSuggestionDto {

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

    private Long forecastDemand;

    private Long suggestedQuantity;

    private String reasonText;
}
