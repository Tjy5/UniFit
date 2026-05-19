package com.authguard.usersystem.dto;

import com.authguard.usersystem.order.FulfillmentCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderFulfillmentCommandDto {

    @NotNull(message = "command不能为空")
    private FulfillmentCommand command;

    @Size(max = 500, message = "reason不能超过500个字符")
    private String reason;
}
