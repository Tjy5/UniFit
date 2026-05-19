package com.suios.admin.analytics.dto;

import lombok.Data;

@Data
public class BehaviorFunnelStageCountDto {

    private String stage;

    private Long sessionsReached;
}
