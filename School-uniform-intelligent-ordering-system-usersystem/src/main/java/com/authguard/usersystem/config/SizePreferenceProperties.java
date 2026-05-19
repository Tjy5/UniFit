package com.authguard.usersystem.config;

import java.math.BigDecimal;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "size-preference")
public class SizePreferenceProperties {
    private boolean enabled = true;
    private int minFeedbackSamples = 2;
    private BigDecimal maxScoreOffset = BigDecimal.valueOf(8);
}
