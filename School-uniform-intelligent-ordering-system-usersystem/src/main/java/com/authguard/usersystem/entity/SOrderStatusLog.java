package com.authguard.usersystem.entity;

import java.util.Date;
import lombok.Data;

@Data
public class SOrderStatusLog {
    private Long id;
    private Long orderId;
    private String eventType;
    private String actorType;
    private Long actorId;
    private Long fromOrderStatus;
    private Long toOrderStatus;
    private String fromPaymentStatus;
    private String toPaymentStatus;
    private String fromShippingStatus;
    private String toShippingStatus;
    private String reason;
    private String requestId;
    private String contextJson;
    private Date createTime;
}
