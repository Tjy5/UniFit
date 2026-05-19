package com.suios.admin.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.suios.admin.common.entity.AuditEntity;
import com.suios.admin.order.dto.OrderAnomalySummary;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("s_orders")
@EqualsAndHashCode(callSuper = true)
public class SOrders extends AuditEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDateTime orderDate;

    private BigDecimal totalPrice;

    private Long addressId;

    private String paymentStatus;

    private String shippingStatus;

    private Long status;

    @TableField(exist = false)
    private String userAccount;

    @TableField(exist = false)
    private String recipientName;

    @TableField(exist = false)
    private String phoneNumber;

    @TableField(exist = false)
    private String fullAddress;

    @TableField(exist = false)
    private List<SOrderItem> orderItems;

    @TableField(exist = false)
    private List<String> allowedFulfillmentActions = new ArrayList<>();

    @TableField(exist = false)
    private OrderAnomalySummary anomalySummary;
}
