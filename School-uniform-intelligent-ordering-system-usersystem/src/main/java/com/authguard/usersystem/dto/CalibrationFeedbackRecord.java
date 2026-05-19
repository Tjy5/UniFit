package com.authguard.usersystem.dto;

import java.util.Date;
import lombok.Data;

@Data
public class CalibrationFeedbackRecord {
    private Long userId;
    private Long orderId;
    private Long orderItemId;
    private String satisfaction;
    private Date feedbackTime;
    private Long schoolId;
    private Long uniformId;
    private Long recommendedSizeId;
}
