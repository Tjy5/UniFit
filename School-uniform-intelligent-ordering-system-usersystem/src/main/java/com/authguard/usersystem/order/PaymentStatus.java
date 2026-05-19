package com.authguard.usersystem.order;

import java.util.Arrays;

public enum PaymentStatus {
    PENDING("PENDING", "待支付"),
    PAID("PAID", "已支付"),
    FAILED("FAILED", "支付失败"),
    REFUNDING("REFUNDING", "退款处理中"),
    REFUNDED("REFUNDED", "已退款"),
    CLOSED("CLOSED", "已关闭");

    private final String code;
    private final String label;

    PaymentStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static PaymentStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("支付状态不合法: " + code));
    }

    public static boolean supports(String code) {
        return code != null && Arrays.stream(values()).anyMatch(status -> status.code.equals(code));
    }
}
