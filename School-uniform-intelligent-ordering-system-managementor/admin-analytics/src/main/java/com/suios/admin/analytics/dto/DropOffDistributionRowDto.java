package com.suios.admin.analytics.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class DropOffDistributionRowDto {

    private String lastActionType;

    private Long sessionCount;

    private BigDecimal share;
}
