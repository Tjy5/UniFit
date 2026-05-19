package com.suios.admin.size.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.suios.admin.common.entity.AuditEntity;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@TableName("s_sizes")
@EqualsAndHashCode(callSuper = true)
public class SSizes extends AuditEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank(message = "尺码名称不能为空")
    private String sizeName;

    @DecimalMin(value = "0", message = "最低身高不能小于 0")
    private Long minHeight;

    @DecimalMin(value = "0", message = "最高身高不能小于 0")
    private Long maxHeight;

    @DecimalMin(value = "0", message = "最小体重不能小于 0")
    private BigDecimal minWeight;

    @DecimalMin(value = "0", message = "最大体重不能小于 0")
    private BigDecimal maxWeight;

    @DecimalMin(value = "0", message = "最小胸围不能小于 0")
    private BigDecimal minChest;

    @DecimalMin(value = "0", message = "最大胸围不能小于 0")
    private BigDecimal maxChest;

    @DecimalMin(value = "0", message = "最小腰围不能小于 0")
    private BigDecimal minWaist;

    @DecimalMin(value = "0", message = "最大腰围不能小于 0")
    private BigDecimal maxWaist;

    @DecimalMin(value = "0", message = "最小臀围不能小于 0")
    private BigDecimal minHip;

    @DecimalMin(value = "0", message = "最大臀围不能小于 0")
    private BigDecimal maxHip;

    @DecimalMin(value = "0", message = "最小肩宽不能小于 0")
    private BigDecimal minShoulder;

    @DecimalMin(value = "0", message = "最大肩宽不能小于 0")
    private BigDecimal maxShoulder;
}
