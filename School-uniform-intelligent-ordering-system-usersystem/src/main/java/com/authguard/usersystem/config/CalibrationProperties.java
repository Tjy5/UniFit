package com.authguard.usersystem.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "calibration")
public class CalibrationProperties {
    private boolean enabled = false;
    private int cacheTtlMinutes = 15;
    private int decayDays = 90;
    private int windowDays = 180;
    private AutoJobProperties autoJob = new AutoJobProperties();
    private ThresholdProperties thresholds = new ThresholdProperties();

    @Data
    public static class AutoJobProperties {
        private boolean enabled = false;
        private String cron = "0 0 2 * * ?";
    }

    @Data
    public static class ThresholdProperties {
        private int minFeedbackCount = 20;
        private int minUniqueUsers = 10;
    }
}
