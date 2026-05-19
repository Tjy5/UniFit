package com.authguard.usersystem.entity;

import lombok.Data;

@Data
public class InventorySku {

    private Long skuId;

    private Long uniformId;

    private Long sizeId;

    private Long stockQuantity;

    private Long reservedQuantity;

    private Long safetyStock;

    private Long reorderPoint;

    private Integer leadTimeDays;

    private String status;
}
