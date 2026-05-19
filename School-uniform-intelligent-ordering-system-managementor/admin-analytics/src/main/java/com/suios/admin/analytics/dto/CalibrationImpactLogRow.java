package com.suios.admin.analytics.dto;

import java.util.Date;
import lombok.Data;

@Data
public class CalibrationImpactLogRow {
    private Long logId;
    private Long schoolId;
    private String schoolName;
    private Long uniformId;
    private String uniformName;
    private Long recommendedSizeId;
    private String recommendedSizeName;
    private String calibrationDetails;
    private Date createTime;
}
