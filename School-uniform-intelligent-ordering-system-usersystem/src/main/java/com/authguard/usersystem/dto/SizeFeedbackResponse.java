package com.authguard.usersystem.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Data;

@Data
public class SizeFeedbackResponse {
    private Long feedbackId;
    private Long orderId;
    private Long orderItemId;
    private String recommendedSize;
    private String purchasedSize;
    private String satisfaction;
    private List<String> issueParts = new ArrayList<>();
    private String note;
    private boolean submitted;
    private Date updatedAt;
}
