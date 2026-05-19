package com.suios.admin.analytics.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class LowConfidenceHotspotDto {
    private Long uniformId;
    private String uniformName;
    private Long sizeId;
    private String sizeName;
    private Long lowConfidenceCount;
    private Long totalRecommendations;
    private BigDecimal lowConfidenceRate;
    private Integer avgConfidence;
}
