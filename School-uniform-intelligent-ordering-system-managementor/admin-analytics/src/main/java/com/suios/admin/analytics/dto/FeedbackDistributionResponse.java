package com.suios.admin.analytics.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class FeedbackDistributionResponse {
    private List<FeedbackDistributionDto> groups = new ArrayList<>();
    private long total;
    private long limit;
    private long offset;
}
