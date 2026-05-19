package com.suios.admin.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class InventoryMovementRequest {

    @NotNull(message = "库存SKU不能为空")
    private Long skuId;

    @NotBlank(message = "库存流水类型不能为空")
    private String movementType;

    @NotNull(message = "库存流水数量不能为空")
    @PositiveOrZero(message = "库存流水数量不能为负数")
    private Long quantity;

    private Long relatedOrderId;

    private Long relatedOrderItemId;

    private String reason;
}
