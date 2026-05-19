package com.suios.admin.order.dto;

import com.suios.admin.common.order.FulfillmentCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderFulfillmentCommandRequest {

    @NotNull(message = "command不能为空")
    private FulfillmentCommand command;

    @Size(max = 500, message = "reason不能超过500个字符")
    private String reason;
}
