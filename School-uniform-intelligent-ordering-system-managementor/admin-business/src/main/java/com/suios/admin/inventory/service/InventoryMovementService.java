package com.suios.admin.inventory.service;

import com.suios.admin.common.exception.BizException;
import com.suios.admin.common.security.AuthContext;
import com.suios.admin.inventory.dto.InventoryMovementRequest;
import com.suios.admin.inventory.entity.InventoryMovement;
import com.suios.admin.inventory.entity.InventorySku;
import com.suios.admin.inventory.mapper.InventoryMovementMapper;
import com.suios.admin.inventory.mapper.InventorySkuMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InventoryMovementService {

    private static final String INBOUND = "INBOUND";
    private static final String ADJUST = "ADJUST";
    private static final String RESERVE = "RESERVE";
    private static final String RELEASE = "RELEASE";
    private static final String OUTBOUND = "OUTBOUND";

    private final InventorySkuMapper skuMapper;
    private final InventoryMovementMapper movementMapper;
    private final AuthContext authContext;

    public InventoryMovementService(InventorySkuMapper skuMapper,
                                    InventoryMovementMapper movementMapper,
                                    AuthContext authContext) {
        this.skuMapper = skuMapper;
        this.movementMapper = movementMapper;
        this.authContext = authContext;
    }

    public List<InventoryMovement> list(Long skuId,
                                        String movementType,
                                        Long relatedOrderId,
                                        LocalDateTime startTime,
                                        LocalDateTime endTime) {
        return movementMapper.selectByFilters(skuId, movementType, relatedOrderId, startTime, endTime);
    }

    @Transactional
    public InventoryMovement apply(InventoryMovementRequest request) {
        return apply(
                request.getSkuId(),
                normalizeType(request.getMovementType()),
                request.getQuantity(),
                request.getRelatedOrderId(),
                request.getRelatedOrderItemId(),
                "ADMIN",
                authContext.getCurrentUserId(),
                request.getReason()
        );
    }

    @Transactional
    public InventoryMovement apply(Long skuId,
                                   String movementType,
                                   Long quantity,
                                   Long relatedOrderId,
                                   Long relatedOrderItemId,
                                   String actorType,
                                   Long actorId,
                                   String reason) {
        validateQuantity(quantity);
        String normalizedType = normalizeType(movementType);
        validateReason(normalizedType, reason);
        InventorySku sku = skuMapper.selectByIdForUpdate(skuId);
        if (sku == null) {
            throw new BizException("库存SKU不存在");
        }
        Long beforeStock = valueOrZero(sku.getStockQuantity());
        Long beforeReserved = valueOrZero(sku.getReservedQuantity());
        Long afterStock = beforeStock;
        Long afterReserved = beforeReserved;
        switch (normalizedType) {
            case INBOUND -> afterStock = beforeStock + quantity;
            case ADJUST -> afterStock = quantity;
            case RESERVE -> {
                if (available(beforeStock, beforeReserved) < quantity) {
                    throw new BizException("可用库存不足");
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
        int updated = skuMapper.updateById(sku);
        if (updated == 0) {
            throw new BizException("库存SKU更新失败");
        }
        InventoryMovement movement = new InventoryMovement();
        movement.setSkuId(skuId);
        movement.setMovementType(normalizedType);
        movement.setQuantity(quantity);
        movement.setBeforeStockQuantity(beforeStock);
        movement.setAfterStockQuantity(afterStock);
        movement.setBeforeReservedQuantity(beforeReserved);
        movement.setAfterReservedQuantity(afterReserved);
        movement.setRelatedOrderId(relatedOrderId);
        movement.setRelatedOrderItemId(relatedOrderItemId);
        movement.setActorType(StringUtils.hasText(actorType) ? actorType : "SYSTEM");
        movement.setActorId(actorId);
        movement.setReason(reason);
        movement.setCreateTime(LocalDateTime.now());
        int inserted = movementMapper.insert(movement);
        if (inserted == 0) {
            throw new BizException("库存流水写入失败");
        }
        return movement;
    }

    public boolean hasOrderMovement(Long orderId, String movementType) {
        if (orderId == null || !StringUtils.hasText(movementType)) {
            return false;
        }
        return movementMapper.countByOrderAndType(orderId, normalizeType(movementType)) > 0;
    }

    public List<InventoryMovement> orderMovements(Long orderId, String movementType) {
        if (orderId == null || !StringUtils.hasText(movementType)) {
            return List.of();
        }
        return movementMapper.selectByOrderAndType(orderId, normalizeType(movementType));
    }

    public long available(InventorySku sku) {
        return available(valueOrZero(sku.getStockQuantity()), valueOrZero(sku.getReservedQuantity()));
    }

    private long available(long stockQuantity, long reservedQuantity) {
        return stockQuantity - reservedQuantity;
    }

    private void validateQuantity(Long quantity) {
        if (quantity == null || quantity < 0) {
            throw new BizException("库存流水数量不能为负数");
        }
    }

    private String normalizeType(String movementType) {
        if (!StringUtils.hasText(movementType)) {
            throw new BizException("库存流水类型不能为空");
        }
        String normalizedType = movementType.trim().toUpperCase();
        if (!List.of(INBOUND, ADJUST, RESERVE, RELEASE, OUTBOUND).contains(normalizedType)) {
            throw new BizException("库存流水类型不合法");
        }
        return normalizedType;
    }

    private void validateReason(String movementType, String reason) {
        if (ADJUST.equals(movementType) && !StringUtils.hasText(reason)) {
            throw new BizException("库存调整必须填写原因");
        }
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
