package com.suios.admin.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("s_inventory_movements")
public class InventoryMovement {

    @TableId(value = "movement_id", type = IdType.AUTO)
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

    private LocalDateTime createTime;
}
