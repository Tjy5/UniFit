package com.authguard.usersystem.mapper;

import com.authguard.usersystem.entity.InventoryMovement;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface InventoryMovementMapper {

    int insert(InventoryMovement movement);

    long countByOrderAndType(@Param("orderId") Long orderId, @Param("movementType") String movementType);

    List<InventoryMovement> selectByOrderAndType(@Param("orderId") Long orderId, @Param("movementType") String movementType);
}
