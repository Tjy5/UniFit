package com.authguard.usersystem.dto;

import java.util.Date;
import lombok.Data;

@Data
public class SizePreferenceProfileDto {
    private Long userId;
    private String categoryKey;
    private String explicitPreference;
    private String learnedDirection;
    private String effectivePreference;
    private String confidenceLevel;
    private Integer confidenceScore;
    private Integer sampleCount;
    private Integer fitCount;
    private Integer tooSmallCount;
    private Integer tooLargeCount;
    private Date updatedAt;
}
