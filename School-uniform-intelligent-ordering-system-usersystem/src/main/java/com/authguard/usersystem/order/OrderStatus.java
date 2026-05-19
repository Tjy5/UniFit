package com.authguard.usersystem.order;

import java.util.Arrays;

public enum OrderStatus {
    PENDING_PAYMENT(0L, "待支付"),
    WAIT_SHIP(1L, "待发货"),
    SHIPPED(2L, "已发货"),
    COMPLETED(3L, "已完成"),
    CANCELLED(4L, "已取消"),
    REFUNDING(5L, "退款中"),
    REFUNDED(6L, "已退款");

    private final Long code;
    private final String label;

    OrderStatus(Long code, String label) {
        this.code = code;
        this.label = label;
    }

    public Long getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static OrderStatus fromCode(Long code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("订单状态不合法: " + code));
    }

    public static boolean supports(Long code) {
        return code != null && Arrays.stream(values()).anyMatch(status -> status.code.equals(code));
    }
}
