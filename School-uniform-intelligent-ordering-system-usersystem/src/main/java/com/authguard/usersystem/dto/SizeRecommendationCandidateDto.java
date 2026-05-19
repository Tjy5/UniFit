package com.authguard.usersystem.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class SizeRecommendationCandidateDto {
    private Long sizeId;
    private String sizeName;
    private BigDecimal score;
    private List<String> reasons = new ArrayList<>();
    private List<SizeDimensionMatchDto> dimensionMatches = new ArrayList<>();
}
