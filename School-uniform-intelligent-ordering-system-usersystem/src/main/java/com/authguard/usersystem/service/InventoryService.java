package com.authguard.usersystem.service;

import com.authguard.usersystem.entity.InventoryMovement;
import com.authguard.usersystem.entity.InventorySku;
import com.authguard.usersystem.exception.BizException;
import com.authguard.usersystem.mapper.InventoryMovementMapper;
import com.authguard.usersystem.mapper.InventorySkuMapper;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    public static final String RESERVE = "RESERVE";
    public static final String RELEASE = "RELEASE";
    public static final String OUTBOUND = "OUTBOUND";

    private final InventorySkuMapper skuMapper;
    private final InventoryMovementMapper movementMapper;

    public InventoryService(InventorySkuMapper skuMapper, InventoryMovementMapper movementMapper) {
        this.skuMapper = skuMapper;
        this.movementMapper = movementMapper;
    }

    @Transactional
    public void reserve(Long uniformId,
                        Long sizeId,
                        Long quantity,
                        Long orderId,
                        Long orderItemId,
                        Long actorId,
                        String reason) {
        InventorySku sku = skuMapper.selectActiveByUniformAndSizeForUpdate(uniformId, sizeId);
        if (sku == null) {
            throw new BizException("该商品尺码暂无可用库存SKU");
        }
        apply(sku, RESERVE, quantity, orderId, orderItemId, "USER", actorId, reason);
    }

    @Transactional
    public void release(Long skuId,
                        Long quantity,
                        Long orderId,
                        Long orderItemId,
                        String actorType,
                        Long actorId,
                        String reason) {
        InventorySku sku = lockSku(skuId);
        apply(sku, RELEASE, quantity, orderId, orderItemId, actorType, actorId, reason);
    }

    @Transactional
    public void outbound(Long skuId,
                         Long quantity,
                         Long orderId,
                         Long orderItemId,
                         String actorType,
                         Long actorId,
                         String reason) {
        InventorySku sku = lockSku(skuId);
        apply(sku, OUTBOUND, quantity, orderId, orderItemId, actorType, actorId, reason);
    }

    public boolean hasOrderMovement(Long orderId, String movementType) {
        if (orderId == null || movementType == null || movementType.isBlank()) {
            return false;
        }
        return movementMapper.countByOrderAndType(orderId, movementType) > 0;
    }

    public List<InventoryMovement> orderMovements(Long orderId, String movementType) {
        return movementMapper.selectByOrderAndType(orderId, movementType);
    }

    public InventorySku lockActiveSku(Long uniformId, Long sizeId) {
        InventorySku sku = skuMapper.selectActiveByUniformAndSizeForUpdate(uniformId, sizeId);
        if (sku == null) {
            throw new BizException("该商品尺码暂无可用库存SKU");
        }
        return sku;
    }

    private InventorySku lockSku(Long skuId) {
        InventorySku sku = skuMapper.selectByIdForUpdate(skuId);
        if (sku == null) {
            throw new BizException("库存SKU不存在");
        }
        return sku;
    }

    private void apply(InventorySku sku,
                       String movementType,
                       Long quantity,
                       Long orderId,
                       Long orderItemId,
                       String actorType,
                       Long actorId,
                       String reason) {
        validateQuantity(quantity);
        Long beforeStock = valueOrZero(sku.getStockQuantity());
        Long beforeReserved = valueOrZero(sku.getReservedQuantity());
        Long afterStock = beforeStock;
        Long afterReserved = beforeReserved;
        switch (movementType) {
            case RESERVE -> {
                if (beforeStock - beforeReserved < quantity) {
                    throw new BizException("商品库存不足，无法创建订单");
                }
                afterReserved = beforeReserved + quantity;
            }
            case RELEASE -> {
                if (beforeReserved < quantity) {
                    throw new BizException("预留库存不足，无法释放");
                }
                afterReserved = beforeReserved - quantity;
            }
            case OUTBOUND -> {
                if (beforeReserved < quantity) {
                    throw new BizException("预留库存不足，无法出库");
                }
                if (beforeStock < quantity) {
                    throw new BizException("库存不足，无法出库");
                }
                afterStock = beforeStock - quantity;
                afterReserved = beforeReserved - quantity;
            }
            default -> throw new BizException("库存流水类型不合法");
        }
        if (afterStock < 0 || afterReserved < 0 || afterReserved > afterStock) {
            throw new BizException("库存数量计算结果不合法");
        }
        sku.setStockQuantity(afterStock);
        sku.setReservedQuantity(afterReserved);
        int updated = skuMapper.updateQuantities(sku);
        if (updated == 0) {
            throw new BizException("库存更新失败");
        }
        InventoryMovement movement = new InventoryMovement();
        movement.setSkuId(sku.getSkuId());
        movement.setMovementType(movementType);
        movement.setQuantity(quantity);
        movement.setBeforeStockQuantity(beforeStock);
        movement.setAfterStockQuantity(afterStock);
        movement.setBeforeReservedQuantity(beforeReserved);
        movement.setAfterReservedQuantity(afterReserved);
        movement.setRelatedOrderId(orderId);
        movement.setRelatedOrderItemId(orderItemId);
        movement.setActorType(actorType);
        movement.setActorId(actorId);
        movement.setReason(reason);
        movement.setCreateTime(new Date());
        int inserted = movementMapper.insert(movement);
        if (inserted == 0) {
            throw new BizException("库存流水写入失败");
        }
    }

    private void validateQuantity(Long quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BizException("库存变动数量必须大于0");
        }
    }

    private Long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
