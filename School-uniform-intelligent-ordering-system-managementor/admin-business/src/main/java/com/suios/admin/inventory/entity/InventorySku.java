package com.suios.admin.inventory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.suios.admin.common.entity.AuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("s_inventory_sku")
@EqualsAndHashCode(callSuper = true)
public class InventorySku extends AuditEntity {

    @TableId(value = "sku_id", type = IdType.AUTO)
    private Long skuId;

    private Long uniformId;

    private Long sizeId;

    private Long stockQuantity;

    private Long reservedQuantity;

    private Long safetyStock;

    private Long reorderPoint;

    private Integer leadTimeDays;

    private String status;

    @TableField(exist = false)
    private String uniformName;

    @TableField(exist = false)
    private String schoolName;

    @TableField(exist = false)
    private String gradeName;

    @TableField(exist = false)
    private String categoryKey;

    @TableField(exist = false)
    private String sizeName;

    @TableField(exist = false)
    private Long availableQuantity;
}
