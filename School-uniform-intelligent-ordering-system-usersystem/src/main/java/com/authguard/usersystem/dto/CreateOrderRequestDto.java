package com.authguard.usersystem.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateOrderRequestDto {
    @NotNull(message = "addressId不能为空")
    private Long addressId; // 已添加：收货地址ID

    @Size(max = 200, message = "备注不能超过200个字符")
    private String remark;  // 可选备注
}
