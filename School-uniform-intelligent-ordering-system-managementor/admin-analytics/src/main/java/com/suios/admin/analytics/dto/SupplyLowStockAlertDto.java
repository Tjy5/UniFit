package com.suios.admin.analytics.dto;

import lombok.Data;

@Data
public class SupplyLowStockAlertDto {

    private Long skuId;

    private Long uniformId;

    private Long sizeId;

    private String uniformName;

    private String schoolName;

    private String gradeName;

    private String categoryKey;

    private String sizeName;

    private Long stockQuantity;

    private Long reservedQuantity;

    private Long availableQuantity;

    private Long safetyStock;

    private Long reorderPoint;

    private Long shortageQuantity;

    private String severity;
}
