package com.suios.admin.auth.dto;

import lombok.Builder;

@Builder
public record AdminInfoResponse(
        Long userId,
        String username,
        String nickname,
        String avatar
) {
}
