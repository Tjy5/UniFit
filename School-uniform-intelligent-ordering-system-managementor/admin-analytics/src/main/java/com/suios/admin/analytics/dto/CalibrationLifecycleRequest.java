package com.suios.admin.analytics.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CalibrationLifecycleRequest {
    @NotBlank(message = "操作原因不能为空")
    private String reason;
}
