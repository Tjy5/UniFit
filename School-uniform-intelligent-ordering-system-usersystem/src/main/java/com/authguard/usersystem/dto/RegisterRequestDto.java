package com.authguard.usersystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequestDto {
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名长度不能超过64个字符")
    private String userAccount;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度应在6到100个字符之间")
    private String userPassword;
}
