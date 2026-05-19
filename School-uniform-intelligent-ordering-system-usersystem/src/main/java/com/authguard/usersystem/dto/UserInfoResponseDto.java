package com.authguard.usersystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponseDto {
    private Long userId;
    private String userAccount;
}
