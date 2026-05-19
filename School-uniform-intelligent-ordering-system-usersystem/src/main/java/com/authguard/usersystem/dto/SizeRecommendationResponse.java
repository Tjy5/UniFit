package com.authguard.usersystem.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class SizeRecommendationResponse {
    private boolean available;
    private String message;
    private Integer confidence;
    private String confidenceLevel;
    private String confidenceMessage;
    private boolean lowConfidence;
    private Long recommendationLogId;
    private boolean calibrationApplied;
    private boolean personalizationApplied;
    private String preferenceSummary;
    private Map<String, Object> calibrationDetails = new LinkedHashMap<>();
    private Map<String, Object> personalizationDetails = new LinkedHashMap<>();
    private List<String> usedDimensions = new ArrayList<>();
    private List<String> reasons = new ArrayList<>();
    private SizeRecommendationCandidateDto recommended;
    private List<SizeRecommendationCandidateDto> alternatives = new ArrayList<>();
}
