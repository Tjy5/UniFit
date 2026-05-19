package com.suios.admin.analytics.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class SupplyTrendPointDto {

    private String bucket;

    private Long totalQuantity;

    private BigDecimal totalAmount;

    private Long orderCount;
}
