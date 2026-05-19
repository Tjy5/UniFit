package com.authguard.usersystem.entity;

import java.util.Date;
import lombok.Data;

@Data
public class UserSizePreferenceEvent {
    private Long eventId;
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
    private Integer directionDelta;
    private String sourceType;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}
