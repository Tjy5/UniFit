package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.InventorySku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InventorySkuMapper {

    InventorySku selectActiveByUniformAndSizeForUpdate(@Param("uniformId") Long uniformId, @Param("sizeId") Long sizeId);

    InventorySku selectByIdForUpdate(@Param("skuId") Long skuId);

    int updateQuantities(InventorySku sku);
}
