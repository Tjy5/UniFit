package com.suios.admin.analytics.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class StalledCartRowDto {

    private Long userId;

    private String userAccount;

    private Long uniformId;

    private String uniformName;

    private Long sizeId;

    private String sizeName;

    private Integer quantity;

    private LocalDateTime addedAt;

    private Long hoursSinceAdd;

    private Long recommendationLogId;

    private LocalDateTime lastActivityAt;
}
