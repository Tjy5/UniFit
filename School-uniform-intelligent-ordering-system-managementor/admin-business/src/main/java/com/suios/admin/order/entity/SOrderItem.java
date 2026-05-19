package com.suios.admin.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;

@Data
@TableName("s_order_items")
public class SOrderItem {

    @TableId(value = "order_item_id", type = IdType.AUTO)
    private Long orderItemId;

    private Long orderId;

    private Long uniformId;

    private Long sizeId;

    private String uniformNameSnapshot;

    private String sizeNameSnapshot;

    private Long quantity;

    private BigDecimal unitPriceSnapshot;

    private BigDecimal itemTotalPrice;

    private String imageSnapshot;

    private Long reviewId;
}
