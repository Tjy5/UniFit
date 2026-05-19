ALTER TABLE recommendation_log
    ADD COLUMN school_id BIGINT NULL COMMENT '学校ID' AFTER user_id,
    ADD COLUMN uniform_id BIGINT NULL COMMENT '校服ID' AFTER school_id,
    ADD COLUMN recommended_size_id BIGINT NULL COMMENT '推荐尺码ID' AFTER uniform_id,
    ADD COLUMN recommended_size_name VARCHAR(64) NULL COMMENT '推荐尺码名称' AFTER recommended_size_id,
    ADD COLUMN order_item_id BIGINT NULL COMMENT '关联订单项ID' AFTER recommended_size_name,
    ADD COLUMN calibration_applied BOOLEAN DEFAULT FALSE COMMENT '是否发生校准' AFTER confidence_level,
    ADD COLUMN calibration_details TEXT NULL COMMENT '校准摘要JSON' AFTER calibration_applied,
    ADD INDEX idx_recommendation_log_uniform (uniform_id),
    ADD INDEX idx_recommendation_log_school (school_id),
    ADD INDEX idx_recommendation_log_order_item (order_item_id),
    ADD INDEX idx_recommendation_log_calibration_time (calibration_applied, create_time);

ALTER TABLE s_shopping_cart_items
    ADD COLUMN recommendation_log_id BIGINT NULL COMMENT '关联推荐日志ID' AFTER size_id,
    ADD INDEX idx_cart_recommendation_log (recommendation_log_id);

CREATE TABLE IF NOT EXISTS calibration_params (
    param_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '校准参数ID',
    scope_type VARCHAR(32) NOT NULL COMMENT '作用域类型: GLOBAL/SCHOOL/PRODUCT',
    scope_id VARCHAR(128) NULL COMMENT '作用域ID',
    target_size_id BIGINT NULL COMMENT '目标尺码ID，可为空',
    calibration_type VARCHAR(32) NOT NULL COMMENT '校准类型: SCORE_OFFSET',
    adjustment_value DECIMAL(10,4) NOT NULL COMMENT '调整值',
    is_manual BOOLEAN DEFAULT FALSE COMMENT '是否人工参数，P0 保留字段',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    status VARCHAR(32) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/OBSERVING/DISABLED',
    sample_size INT NULL COMMENT '样本量',
    unique_user_count INT NULL COMMENT '不同用户数',
    feedback_distribution TEXT NULL COMMENT '反馈分布JSON',
    confidence_level VARCHAR(32) NULL COMMENT '校准置信度: HIGH/MEDIUM/LOW',
    effective_from DATETIME NOT NULL COMMENT '生效开始时间',
    effective_until DATETIME NULL COMMENT '生效结束时间',
    version INT DEFAULT 1 COMMENT '版本号',
    create_by VARCHAR(64) NULL,
    create_time DATETIME NULL,
    update_by VARCHAR(64) NULL,
    update_time DATETIME NULL,
    INDEX idx_calibration_scope (scope_type, scope_id),
    INDEX idx_calibration_scope_size (scope_type, scope_id, target_size_id),
    INDEX idx_calibration_identity_version (scope_type, scope_id, target_size_id, calibration_type, version),
    INDEX idx_calibration_enabled (enabled, effective_from, effective_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校准参数表';

CREATE TABLE IF NOT EXISTS calibration_audit (
    audit_id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '审计ID',
    param_id BIGINT NULL COMMENT '关联参数ID',
    action VARCHAR(32) NOT NULL COMMENT '操作: CREATE/UPDATE/ENABLE/DISABLE/ROLLBACK/OBSERVING',
    old_value TEXT NULL COMMENT '旧值JSON',
    new_value TEXT NULL COMMENT '新值JSON',
    reason VARCHAR(500) NULL COMMENT '操作原因',
    operator VARCHAR(64) NULL COMMENT '操作人',
    create_time DATETIME NULL COMMENT '操作时间',
    INDEX idx_calibration_audit_param (param_id),
    INDEX idx_calibration_audit_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='校准审计日志表';
