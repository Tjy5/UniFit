ALTER TABLE s_order_items
    ADD COLUMN size_id BIGINT NULL COMMENT '尺码ID' AFTER uniform_id,
    ADD INDEX idx_orderitem_size_id (size_id),
    ADD INDEX idx_orderitem_uniform_size (uniform_id, size_id),
    ADD CONSTRAINT fk_orderitem_size FOREIGN KEY (size_id) REFERENCES s_sizes (id) ON DELETE RESTRICT ON UPDATE RESTRICT;

UPDATE s_order_items oi
JOIN (
    SELECT size_name, MIN(id) AS size_id
    FROM s_sizes
    WHERE size_name IS NOT NULL
    GROUP BY size_name
    HAVING COUNT(*) = 1
) matched_size ON matched_size.size_name = oi.size_name_snapshot
SET oi.size_id = matched_size.size_id
WHERE oi.size_id IS NULL;

ALTER TABLE s_orders
    ADD INDEX idx_orders_status_date (payment_status, shipping_status, status, order_date),
    ADD INDEX idx_orders_order_date (order_date);

CREATE TABLE IF NOT EXISTS s_inventory_sku (
    sku_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '库存SKU ID',
    uniform_id BIGINT NOT NULL COMMENT '校服ID',
    size_id BIGINT NOT NULL COMMENT '尺码ID',
    stock_quantity BIGINT NOT NULL DEFAULT 0 COMMENT '库存数量',
    reserved_quantity BIGINT NOT NULL DEFAULT 0 COMMENT '预留数量',
    safety_stock BIGINT NOT NULL DEFAULT 0 COMMENT '安全库存',
    reorder_point BIGINT NOT NULL DEFAULT 0 COMMENT '补货点',
    lead_time_days INT NOT NULL DEFAULT 0 COMMENT '交付提前期天数',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    create_by VARCHAR(64) NULL DEFAULT '' COMMENT '创建者',
    create_time DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    update_by VARCHAR(64) NULL DEFAULT '' COMMENT '更新者',
    update_time DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    remark VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (sku_id),
    UNIQUE KEY uk_inventory_sku_uniform_size_status (uniform_id, size_id, status),
    KEY idx_inventory_sku_uniform (uniform_id),
    KEY idx_inventory_sku_size (size_id),
    KEY idx_inventory_sku_status (status),
    KEY idx_inventory_sku_low_stock (status, reorder_point, stock_quantity, reserved_quantity),
    CONSTRAINT fk_inventory_sku_uniform FOREIGN KEY (uniform_id) REFERENCES s_uniform (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_inventory_sku_size FOREIGN KEY (size_id) REFERENCES s_sizes (id) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存SKU表';

CREATE TABLE IF NOT EXISTS s_inventory_movements (
    movement_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '库存流水ID',
    sku_id BIGINT NOT NULL COMMENT '库存SKU ID',
    movement_type VARCHAR(32) NOT NULL COMMENT '流水类型: RESERVE/RELEASE/OUTBOUND/INBOUND/ADJUST',
    quantity BIGINT NOT NULL COMMENT '变动数量',
    before_stock_quantity BIGINT NOT NULL COMMENT '变动前库存数量',
    after_stock_quantity BIGINT NOT NULL COMMENT '变动后库存数量',
    before_reserved_quantity BIGINT NOT NULL COMMENT '变动前预留数量',
    after_reserved_quantity BIGINT NOT NULL COMMENT '变动后预留数量',
    related_order_id BIGINT NULL COMMENT '关联订单ID',
    related_order_item_id BIGINT NULL COMMENT '关联订单项ID',
    actor_type VARCHAR(32) NULL COMMENT '操作者类型',
    actor_id BIGINT NULL COMMENT '操作者ID',
    reason VARCHAR(500) NULL COMMENT '原因',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (movement_id),
    KEY idx_inventory_movement_sku (sku_id),
    KEY idx_inventory_movement_type (movement_type),
    KEY idx_inventory_movement_order (related_order_id),
    KEY idx_inventory_movement_order_item (related_order_item_id),
    KEY idx_inventory_movement_create_time (create_time),
    CONSTRAINT fk_inventory_movement_sku FOREIGN KEY (sku_id) REFERENCES s_inventory_sku (sku_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_inventory_movement_order FOREIGN KEY (related_order_id) REFERENCES s_orders (id) ON DELETE SET NULL ON UPDATE RESTRICT,
    CONSTRAINT fk_inventory_movement_order_item FOREIGN KEY (related_order_item_id) REFERENCES s_order_items (order_item_id) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';

INSERT INTO s_inventory_sku (
    uniform_id,
    size_id,
    stock_quantity,
    reserved_quantity,
    safety_stock,
    reorder_point,
    lead_time_days,
    status,
    create_by,
    create_time,
    update_by,
    update_time,
    remark
)
SELECT DISTINCT
    seed.uniform_id,
    seed.size_id,
    0,
    0,
    0,
    0,
    0,
    'ACTIVE',
    'system',
    NOW(),
    'system',
    NOW(),
    '迁移初始化SKU'
FROM (
    SELECT uniform_id, size_id FROM s_order_items WHERE size_id IS NOT NULL
    UNION
    SELECT uniform_id, size_id FROM s_shopping_cart_items WHERE size_id IS NOT NULL
) seed
WHERE NOT EXISTS (
    SELECT 1
    FROM s_inventory_sku sku
    WHERE sku.uniform_id = seed.uniform_id
      AND sku.size_id = seed.size_id
      AND sku.status = 'ACTIVE'
);
