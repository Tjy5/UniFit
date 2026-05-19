package com.suios.admin.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UniformStatus {
    ACTIVE(0, "上架"),
    INACTIVE(1, "下架");

    private final Integer code;
    private final String label;
}
