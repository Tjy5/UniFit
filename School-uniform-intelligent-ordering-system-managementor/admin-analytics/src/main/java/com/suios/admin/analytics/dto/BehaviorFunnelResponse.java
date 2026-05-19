package com.suios.admin.analytics.dto;

import java.util.List;
import lombok.Data;

@Data
public class BehaviorFunnelResponse {

    private List<BehaviorFunnelStageDto> stages = List.of();

    private Long totalSessions = 0L;

    private Integer sessionInactivityGapMinutes;
}
