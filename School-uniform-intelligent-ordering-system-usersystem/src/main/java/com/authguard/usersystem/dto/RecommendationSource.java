package com.authguard.usersystem.dto;

import java.util.Locale;

/**
 * 推荐请求来源枚举，约束 {@link SizeRecommendationRequest#getSource()} 的取值范围，
 * 并提供归一化方法将历史与未来的输入统一映射到 lower-kebab-case 形式，
 * 写入 {@code recommendation_log.request_source} 时保持闭集语义。
 */
public enum RecommendationSource {

    MALL_HOME_DIALOG("mall-home-dialog"),
    WISHLIST("wishlist"),
    ORDER_FEEDBACK("order-feedback"),
    PRODUCT_DETAIL("product-detail"),
    UNKNOWN("unknown");

    private final String canonicalValue;

    RecommendationSource(String canonicalValue) {
        this.canonicalValue = canonicalValue;
    }

    /**
     * 返回写入数据库时使用的标准 lower-kebab-case 值。
     */
    public String getCanonicalValue() {
        return canonicalValue;
    }

    /**
     * 安全解析输入字符串到枚举。允许 {@code null}、空白、未知值，
     * 这些情况一律返回 {@link #UNKNOWN}。匹配过程忽略大小写、首尾空白
     * 以及下划线/空格等常见替换符。
     *
     * @param raw 调用方提供的原始来源字符串，可能为 {@code null}
     * @return 命中的枚举常量；找不到时返回 {@link #UNKNOWN}
     */
    public static RecommendationSource fromString(String raw) {
        if (raw == null) {
            return UNKNOWN;
        }
        String normalized = raw.trim();
        if (normalized.isEmpty()) {
            return UNKNOWN;
        }
        String canonical = normalized
                .toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-');
        for (RecommendationSource source : values()) {
            if (source.canonicalValue.equals(canonical) || source.name().toLowerCase(Locale.ROOT).equals(canonical)) {
                return source;
            }
        }
        return UNKNOWN;
    }

    /**
     * 将原始字符串归一化到标准 lower-kebab-case 形式。未知或空白输入返回 {@code "unknown"}。
     * 等价于 {@code fromString(raw).getCanonicalValue()}，仅作便利封装。
     *
     * @param raw 调用方提供的原始来源字符串，可能为 {@code null}
     * @return 标准化后的 lower-kebab-case 值
     */
    public static String normalizeOrUnknown(String raw) {
        return fromString(raw).getCanonicalValue();
    }
}
