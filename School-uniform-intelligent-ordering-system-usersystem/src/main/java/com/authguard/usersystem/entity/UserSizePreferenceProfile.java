package com.authguard.usersystem.entity;

import java.util.Date;
import lombok.Data;

@Data
public class UserSizePreferenceProfile {
    private Long profileId;
    private Long userId;
    private String categoryKey;
    private String explicitPreference;
    private String learnedDirection;
    private String confidenceLevel;
    private Integer confidenceScore;
    private Integer sampleCount;
    private Integer fitCount;
    private Integer tooSmallCount;
    private Integer tooLargeCount;
    private Integer looseSignalCount;
    private Integer slimSignalCount;
    private Integer standardSignalCount;
    private String sourceSummary;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}
