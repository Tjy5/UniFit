package com.authguard.usersystem.entity;

import lombok.Data;
import java.util.Date;

@Data
public class ShoppingCartItem {
    private Long cartItemId;
    private Long userId;
    private Long uniformId;
    private Long sizeId; // Assuming s_sizes.id
    private Long recommendationLogId;

    private Integer quantity;
    private Date addedAt;

    private String uniformName;
    private java.math.BigDecimal unitPrice;
    private String uniformImage;
    private String sizeNameDisplay;
}
