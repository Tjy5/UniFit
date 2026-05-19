package com.authguard.usersystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemRequestDto {
    @NotNull(message = "uniformId不能为空")
    private Long uniformId;

    @NotNull(message = "sizeId不能为空")
    private Long sizeId;

    @Min(value = 1, message = "quantity必须大于等于1")
    private int quantity = 1;

    private Long recommendationLogId;
}
