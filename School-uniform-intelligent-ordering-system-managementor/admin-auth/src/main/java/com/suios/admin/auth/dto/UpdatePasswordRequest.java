package com.suios.admin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdatePasswordRequest {

    @NotBlank(message = "oldPassword is required")
    private String oldPassword;

    @NotBlank(message = "newPassword is required")
    private String newPassword;
}
