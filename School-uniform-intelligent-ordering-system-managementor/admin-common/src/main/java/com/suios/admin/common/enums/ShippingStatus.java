package com.suios.admin.common.enums;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShippingStatus {
    NOT_SHIPPED("NOT_SHIPPED", "未发货"),
    SHIPPED("SHIPPED", "已发货"),
    DELIVERED("DELIVERED", "已签收"),
    RETURN_REQUESTED("RETURN_REQUESTED", "退货申请中"),
    RETURNED("RETURNED", "已退货");

    private final String code;
    private final String label;

    public static ShippingStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("物流状态不合法: " + code));
    }

    public static boolean supports(String code) {
        return code != null && Arrays.stream(values()).anyMatch(status -> status.code.equals(code));
    }
}
