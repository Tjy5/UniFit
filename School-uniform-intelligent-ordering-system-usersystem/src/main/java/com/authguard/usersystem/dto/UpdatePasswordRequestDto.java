package com.authguard.usersystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePasswordRequestDto {
    @NotBlank(message = "旧密码不能为空")
    @Size(min = 6, max = 100, message = "旧密码长度应在6到100个字符之间")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 100, message = "新密码长度应在6到100个字符之间")
    private String newPassword;
}
