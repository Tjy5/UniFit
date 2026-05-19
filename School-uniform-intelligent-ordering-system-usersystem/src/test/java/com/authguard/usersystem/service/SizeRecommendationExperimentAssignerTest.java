package com.authguard.usersystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.authguard.usersystem.config.SizeRecommendationExperimentProperties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class SizeRecommendationExperimentAssignerTest {

    @Test
    void shouldKeepSameUserInSameVariant() {
        SizeRecommendationExperimentAssigner assigner = new SizeRecommendationExperimentAssigner(enabledProperties(50));

        var first = assigner.assign(42L);
        var second = assigner.assign(42L);

        assertTrue(first.enabled());
        assertEquals("size-calibration-ab-test", first.experimentKey());
        assertEquals("v1", first.strategyVersion());
        assertEquals(first.variant(), second.variant());
    }

    @Test
    void shouldDistributeDifferentUsersAcrossVariants() {
        SizeRecommendationExperimentAssigner assigner = new SizeRecommendationExperimentAssigner(enabledProperties(50));

        Set<String> variants = LongStream.rangeClosed(1, 200)
                .mapToObj(userId -> assigner.assign(userId).variant())
                .collect(Collectors.toSet());

        assertTrue(variants.contains("A"));
        assertTrue(variants.contains("B"));
    }

    @Test
    void shouldReturnNoAttributionWhenDisabledOrAnonymous() {
        SizeRecommendationExperimentProperties properties = enabledProperties(50);
        properties.setEnabled(false);
        SizeRecommendationExperimentAssigner assigner = new SizeRecommendationExperimentAssigner(properties);

        assertFalse(assigner.assign(42L).enabled());
        assertFalse(new SizeRecommendationExperimentAssigner(enabledProperties(50)).assign(null).enabled());
    }

    @Test
    void shouldClampTrafficSplit() {
        assertEquals("A", new SizeRecommendationExperimentAssigner(enabledProperties(-10)).assign(42L).variant());
        assertEquals("B", new SizeRecommendationExperimentAssigner(enabledProperties(150)).assign(42L).variant());
    }

    private SizeRecommendationExperimentProperties enabledProperties(int bTrafficPercentage) {
        SizeRecommendationExperimentProperties properties = new SizeRecommendationExperimentProperties();
        properties.setEnabled(true);
        properties.setExperimentKey("size-calibration-ab-test");
        properties.setStrategyVersion("v1");
        properties.setBTrafficPercentage(bTrafficPercentage);
        return properties;
    }
}
