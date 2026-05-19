CREATE TABLE IF NOT EXISTS s_order_status_logs (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单状态审计ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    event_type VARCHAR(64) NOT NULL COMMENT '履约事件类型',
    actor_type VARCHAR(32) NOT NULL COMMENT '操作者类型: USER/ADMIN/SYSTEM',
    actor_id BIGINT NULL COMMENT '操作者ID',
    from_order_status BIGINT NULL COMMENT '变更前订单状态',
    to_order_status BIGINT NULL COMMENT '变更后订单状态',
    from_payment_status VARCHAR(50) NULL COMMENT '变更前支付状态',
    to_payment_status VARCHAR(50) NULL COMMENT '变更后支付状态',
    from_shipping_status VARCHAR(50) NULL COMMENT '变更前物流状态',
    to_shipping_status VARCHAR(50) NULL COMMENT '变更后物流状态',
    reason VARCHAR(500) NULL COMMENT '变更原因',
    request_id VARCHAR(128) NULL COMMENT '请求或追踪ID',
    context_json TEXT NULL COMMENT '恢复退款前状态等扩展上下文',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_order_status_logs_order_id (order_id),
    KEY idx_order_status_logs_create_time (create_time),
    CONSTRAINT fk_order_status_logs_order FOREIGN KEY (order_id) REFERENCES s_orders (id) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单状态审计表';

INSERT INTO s_order_status_logs (
    order_id,
    event_type,
    actor_type,
    actor_id,
    from_order_status,
    to_order_status,
    from_payment_status,
    to_payment_status,
    from_shipping_status,
    to_shipping_status,
    reason,
    create_time
)
SELECT
    o.id,
    'LEGACY_STATUS_IMPORTED',
    'SYSTEM',
    o.user_id,
    NULL,
    o.status,
    NULL,
    o.payment_status,
    NULL,
    o.shipping_status,
    '历史订单状态导入基线',
    COALESCE(o.update_time, o.create_time, o.order_date, NOW())
FROM s_orders o
WHERE NOT EXISTS (
    SELECT 1
    FROM s_order_status_logs l
    WHERE l.order_id = o.id
);
