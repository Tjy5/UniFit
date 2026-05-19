package com.authguard.usersystem.service;

import com.authguard.usersystem.config.SizeRecommendationExperimentProperties;
import com.authguard.usersystem.dto.SizeRecommendationExperimentAssignment;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SizeRecommendationExperimentAssigner {

    private final SizeRecommendationExperimentProperties properties;

    public SizeRecommendationExperimentAssigner(SizeRecommendationExperimentProperties properties) {
        this.properties = properties;
    }

    public SizeRecommendationExperimentAssignment assign(Long userId) {
        if (userId == null || properties == null || !properties.isEnabled()) {
            return SizeRecommendationExperimentAssignment.disabled();
        }
        String experimentKey = StringUtils.hasText(properties.getExperimentKey())
                ? properties.getExperimentKey()
                : "size-calibration-ab-test";
        int bucket = stableBucket(experimentKey, userId);
        String variant = bucket < properties.normalizedBTrafficPercentage() ? "B" : "A";
        return new SizeRecommendationExperimentAssignment(
                true,
                experimentKey,
                variant,
                StringUtils.hasText(properties.getStrategyVersion()) ? properties.getStrategyVersion() : "v1"
        );
    }

    private int stableBucket(String experimentKey, Long userId) {
        byte[] digest = sha256(experimentKey + ":" + userId);
        long value = 0L;
        for (int index = 0; index < 8; index++) {
            value = (value << 8) | (digest[index] & 0xffL);
        }
        return (int) Long.remainderUnsigned(value, 100L);
    }

    private byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
