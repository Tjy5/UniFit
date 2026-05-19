package com.authguard.usersystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.authguard.usersystem.dto.ConfidenceResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ConfidenceCalculatorTest {

    private final ConfidenceCalculator confidenceCalculator = new ConfidenceCalculator();

    @Test
    void shouldReturnHighConfidenceForStrongMultiDimensionMatch() {
        ConfidenceResult result = confidenceCalculator.calculate(
                5,
                5,
                BigDecimal.valueOf(96),
                BigDecimal.valueOf(12),
                true,
                0
        );

        assertEquals("HIGH", result.getLevel());
        assertTrue(result.getScore() >= 90);
        assertTrue(result.getMessage().contains("推荐把握度较高"));
    }

    @Test
    void shouldReturnLowConfidenceWhenOnlyBaseDimensionsAndNearBoundary() {
        ConfidenceResult result = confidenceCalculator.calculate(
                2,
                1,
                BigDecimal.valueOf(62),
                BigDecimal.valueOf(3),
                false,
                2
        );

        assertEquals("LOW", result.getLevel());
        assertTrue(result.isLowConfidence());
        assertTrue(result.getScore() < 60);
        assertTrue(result.getMessage().contains("推荐把握度较低"));
    }
}
