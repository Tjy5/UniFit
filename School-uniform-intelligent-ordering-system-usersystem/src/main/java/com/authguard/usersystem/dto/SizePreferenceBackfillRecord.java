package com.authguard.usersystem.dto;

import java.util.Date;
import lombok.Data;

@Data
public class SizePreferenceBackfillRecord {
    private Long userId;
    private Long orderId;
    private Long orderItemId;
    private Long feedbackId;
    private Long uniformId;
    private String categoryKey;
    private String recommendedSize;
    private String purchasedSize;
    private String satisfaction;
    private String issueParts;
    private Date feedbackTime;
}
