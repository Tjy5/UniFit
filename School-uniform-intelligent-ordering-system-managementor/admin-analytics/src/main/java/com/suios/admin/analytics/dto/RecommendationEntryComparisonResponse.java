package com.suios.admin.analytics.dto;

import java.util.List;
import lombok.Data;

@Data
public class RecommendationEntryComparisonResponse {

    private List<RecommendationEntryComparisonRowDto> rows = List.of();
}
