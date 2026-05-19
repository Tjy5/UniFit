package com.authguard.usersystem.dto;

public record SizeRecommendationExperimentAssignment(
        boolean enabled,
        String experimentKey,
        String variant,
        String strategyVersion
) {
    public static SizeRecommendationExperimentAssignment disabled() {
        return new SizeRecommendationExperimentAssignment(false, null, null, null);
    }

    public boolean isVariantA() {
        return enabled && "A".equals(variant);
    }

    public boolean isVariantB() {
        return enabled && "B".equals(variant);
    }
}
