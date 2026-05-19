package com.suios.admin.common.enums;

import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    PENDING_PAYMENT(0, "待支付"),
    WAIT_SHIP(1, "待发货"),
    SHIPPED(2, "已发货"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消"),
    REFUNDING(5, "退款中"),
    REFUNDED(6, "已退款");

    private final Integer code;
    private final String label;

    public static OrderStatus fromCode(Long code) {
        return Arrays.stream(values())
                .filter(status -> status.code.longValue() == (code == null ? Long.MIN_VALUE : code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("订单状态不合法: " + code));
    }

    public static boolean supports(Long code) {
        return code != null && Arrays.stream(values()).anyMatch(status -> status.code.longValue() == code);
    }
}
