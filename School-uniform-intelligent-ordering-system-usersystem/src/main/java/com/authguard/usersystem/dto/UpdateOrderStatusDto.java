package com.authguard.usersystem.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateOrderStatusDto {
    @Min(value = 0, message = "orderStatus不能小于0")
    @Max(value = 99, message = "orderStatus不能大于99")
    private Long status;                // Overall order status

    @Size(max = 32, message = "paymentStatusCode不能超过32个字符")
    @Pattern(regexp = "^[A-Z_]*$", message = "paymentStatusCode只能包含大写字母和下划线")
    private String paymentStatusCode;   // e.g., PAID, REFUNDED

    @Size(max = 32, message = "shippingStatusCode不能超过32个字符")
    @Pattern(regexp = "^[A-Z_]*$", message = "shippingStatusCode只能包含大写字母和下划线")
    private String shippingStatusCode;  // e.g., SHIPPED, DELIVERED
}
