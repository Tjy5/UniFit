package com.suios.admin.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("s_order_status_logs")
public class SOrderStatusLog {

    @TableId(type = IdType.AUTO)
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
    private LocalDateTime createTime;
}
