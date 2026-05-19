package com.suios.admin.analytics.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class SupplySalesAnalyticsDto {

    private String groupKey;

    private String groupName;

    private Long totalQuantity;

    private BigDecimal totalAmount;

    private Long orderCount;

    private Long uniformId;

    private Long sizeId;

    private String uniformName;

    private String sizeName;

    private String schoolName;

    private String gradeName;

    private String categoryKey;
}
