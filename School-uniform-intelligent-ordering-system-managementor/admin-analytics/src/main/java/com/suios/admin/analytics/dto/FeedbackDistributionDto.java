package com.suios.admin.analytics.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class FeedbackDistributionDto {
    private String groupKey;
    private String groupName;
    private Long totalFeedback;
    private Long fit;
    private Long tooLarge;
    private Long tooSmall;
    private BigDecimal fitRate;
}
