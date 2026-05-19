package com.authguard.usersystem.entity;

import java.util.Date;
import lombok.Data;

@Data
public class SizeFeedback {
    private Long feedbackId;
    private Long userId;
    private Long orderId;
    private Long orderItemId;
    private String recommendedSize;
    private String purchasedSize;
    private String satisfaction;
    private String issueParts;
    private String note;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}
