package com.authguard.usersystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "size-recommendation.experiment")
public class SizeRecommendationExperimentProperties {
    private boolean enabled = false;
    private String experimentKey = "size-calibration-ab-test";
    private String strategyVersion = "v1";
    private int bTrafficPercentage = 50;

    public int normalizedBTrafficPercentage() {
        return Math.max(0, Math.min(100, bTrafficPercentage));
    }
}
