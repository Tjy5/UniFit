package com.authguard.usersystem.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class SizeDimensionMatchDto {
    private String dimensionKey;
    private String label;
    private BigDecimal userValue;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private Integer matchPercent;
    private Boolean withinRange;
    private String message;
}
