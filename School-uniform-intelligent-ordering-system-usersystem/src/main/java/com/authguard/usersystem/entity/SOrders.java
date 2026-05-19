package com.authguard.usersystem.entity;

import lombok.Data;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class SOrders {

    private Long id;            // ID
    private Long userId;        // 用户ID

    private Date orderDate;     // 订单下单日期

    private BigDecimal totalPrice; // 总价
    // private String address;     // 已移除：旧地址字段
    private Long addressId;     // 已添加：关联的地址ID (对应数据库的 address_id 列)

    private Long status;        // 订单状态
    private String paymentStatusCode; // 支付状态编码
    private String shippingStatusCode; // 物流状态编码

    private String createBy;    // 创建者
    private Date createTime;    // 记录创建时间
    private String updateBy;    // 更新者
    private Date updateTime;    // 更新时间
    private String remark;      // 备注

    // 已添加：用于接收关联查询出的完整地址信息，不会直接映射到 s_orders 表的列
    // MyBatis 的 resultMap 会负责填充这个对象
    private SAddress shippingAddress;

    private List<SOrderItem> orderItems; // 订单项列表
    private List<String> allowedFulfillmentActions = new ArrayList<>();
}
