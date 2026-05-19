package com.suios.admin.order.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OrderStatusLogDto {
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
    private String contextJson;
    private LocalDateTime createTime;
}
