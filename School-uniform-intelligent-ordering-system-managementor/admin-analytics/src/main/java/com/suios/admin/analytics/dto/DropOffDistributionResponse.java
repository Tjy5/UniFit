package com.suios.admin.analytics.dto;

import java.util.List;
import lombok.Data;

@Data
public class DropOffDistributionResponse {

    private List<DropOffDistributionRowDto> rows = List.of();

    private Long totalSessions = 0L;

    private Integer limit;
}
