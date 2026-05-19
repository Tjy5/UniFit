package com.suios.admin.analytics.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class CalibrationImpactResponseDto {
    private CalibrationImpactSummaryDto summary = new CalibrationImpactSummaryDto();
    private List<CalibrationImpactRecordDto> records = new ArrayList<>();
    private long total;
    private long limit;
    private long offset;
}
