package com.authguard.usersystem.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class RecommendationSourceTest {

    @Test
    void shouldResolveKnownCanonicalAndCaseInsensitiveValues() {
        assertSame(RecommendationSource.MALL_HOME_DIALOG, RecommendationSource.fromString("mall-home-dialog"));
        assertSame(RecommendationSource.MALL_HOME_DIALOG, RecommendationSource.fromString("MALL_HOME_DIALOG"));
        assertSame(RecommendationSource.WISHLIST, RecommendationSource.fromString(" wishlist "));
        assertSame(RecommendationSource.ORDER_FEEDBACK, RecommendationSource.fromString("ORDER FEEDBACK"));
        assertSame(RecommendationSource.PRODUCT_DETAIL, RecommendationSource.fromString("product-detail"));
        assertSame(RecommendationSource.PRODUCT_DETAIL, RecommendationSource.fromString("PRODUCT_DETAIL"));
        assertSame(RecommendationSource.PRODUCT_DETAIL, RecommendationSource.fromString("Product-Detail"));
    }

    @Test
    void shouldReturnUnknownForBlankNullAndUnsupportedLabels() {
        assertSame(RecommendationSource.UNKNOWN, RecommendationSource.fromString(null));
        assertSame(RecommendationSource.UNKNOWN, RecommendationSource.fromString(""));
        assertSame(RecommendationSource.UNKNOWN, RecommendationSource.fromString("legacy-entry"));
        assertEquals("unknown", RecommendationSource.normalizeOrUnknown("legacy-entry"));
        assertEquals("unknown", RecommendationSource.normalizeOrUnknown("  "));
    }
}
