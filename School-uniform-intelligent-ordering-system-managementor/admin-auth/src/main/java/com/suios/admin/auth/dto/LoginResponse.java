package com.suios.admin.auth.dto;

public record LoginResponse(
        String token,
        AdminInfoResponse adminInfo
) {
}
