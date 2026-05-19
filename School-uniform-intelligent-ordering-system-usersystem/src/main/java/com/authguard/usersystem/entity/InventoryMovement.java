package com.authguard.usersystem.entity;

import java.util.Date;
import lombok.Data;

@Data
public class InventoryMovement {

    private Long movementId;

    private Long skuId;

    private String movementType;

    private Long quantity;

    private Long beforeStockQuantity;

    private Long afterStockQuantity;

    private Long beforeReservedQuantity;

    private Long afterReservedQuantity;

    private Long relatedOrderId;

    private Long relatedOrderItemId;

    private String actorType;

    private Long actorId;

    private String reason;

    private Date createTime;
}
