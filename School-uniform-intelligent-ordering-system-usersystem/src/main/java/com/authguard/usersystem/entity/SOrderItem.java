package com.authguard.usersystem.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SOrderItem {
    private Long orderItemId;
    private Long orderId;
    private Long uniformId;
    private Long sizeId;
    private String uniformNameSnapshot;
    private String sizeNameSnapshot;
    private Long quantity;
    private BigDecimal unitPriceSnapshot;
    private BigDecimal itemTotalPrice;
    private String imageSnapshot;
    private Long reviewId;
}
