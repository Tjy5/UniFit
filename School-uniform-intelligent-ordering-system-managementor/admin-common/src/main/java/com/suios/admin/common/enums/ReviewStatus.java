package com.suios.admin.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewStatus {
    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已拒绝");

    private final Integer code;
    private final String label;
}
