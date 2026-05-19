package com.authguard.usersystem.order;

import java.util.Arrays;

public enum ShippingStatus {
    NOT_SHIPPED("NOT_SHIPPED", "未发货"),
    SHIPPED("SHIPPED", "已发货"),
    DELIVERED("DELIVERED", "已签收"),
    RETURN_REQUESTED("RETURN_REQUESTED", "退货申请中"),
    RETURNED("RETURNED", "已退货");

    private final String code;
    private final String label;

    ShippingStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

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
