package com.suios.admin.analytics.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class BehaviorFunnelStageDto {

    private String stage;

    private String label;

    private Long sessionsReached;

    private BigDecimal conversionRate;
}
