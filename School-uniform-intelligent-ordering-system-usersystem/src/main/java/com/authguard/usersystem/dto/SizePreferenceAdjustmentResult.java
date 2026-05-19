package com.authguard.usersystem.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

@Data
public class SizePreferenceAdjustmentResult {
    private boolean applied;
    private String categoryKey;
    private String preference;
    private String confidenceLevel;
    private Integer confidenceScore;
    private Integer sampleCount;
    private String preferenceSource;
    private String issuePartSummary;
    private Map<String, Integer> issuePartCounts = new LinkedHashMap<>();
    private List<String> topIssueParts = List.of();
    private String summary;
    private Map<Long, BigDecimal> offsetsBySizeId = new LinkedHashMap<>();
}
