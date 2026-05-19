ALTER TABLE user_message
    ADD COLUMN chest DECIMAL(10, 2) NULL COMMENT '胸围(cm)' AFTER weight,
    ADD COLUMN waist DECIMAL(10, 2) NULL COMMENT '腰围(cm)' AFTER chest,
    ADD COLUMN hip DECIMAL(10, 2) NULL COMMENT '臀围(cm)' AFTER waist,
    ADD COLUMN shoulder DECIMAL(10, 2) NULL COMMENT '肩宽(cm)' AFTER hip;

ALTER TABLE s_sizes
    ADD COLUMN min_chest DECIMAL(10, 2) NULL COMMENT '最小胸围(cm)' AFTER max_weight,
    ADD COLUMN max_chest DECIMAL(10, 2) NULL COMMENT '最大胸围(cm)' AFTER min_chest,
    ADD COLUMN min_waist DECIMAL(10, 2) NULL COMMENT '最小腰围(cm)' AFTER max_chest,
    ADD COLUMN max_waist DECIMAL(10, 2) NULL COMMENT '最大腰围(cm)' AFTER min_waist,
    ADD COLUMN min_hip DECIMAL(10, 2) NULL COMMENT '最小臀围(cm)' AFTER max_waist,
    ADD COLUMN max_hip DECIMAL(10, 2) NULL COMMENT '最大臀围(cm)' AFTER min_hip,
    ADD COLUMN min_shoulder DECIMAL(10, 2) NULL COMMENT '最小肩宽(cm)' AFTER max_hip,
    ADD COLUMN max_shoulder DECIMAL(10, 2) NULL COMMENT '最大肩宽(cm)' AFTER min_shoulder;

CREATE TABLE IF NOT EXISTS size_feedback (
    feedback_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_id BIGINT NOT NULL COMMENT '订单ID',
    order_item_id BIGINT NOT NULL COMMENT '订单项ID',
    recommended_size VARCHAR(64) NULL DEFAULT NULL COMMENT '推荐尺码',
    purchased_size VARCHAR(64) NULL DEFAULT NULL COMMENT '购买尺码',
    satisfaction VARCHAR(32) NOT NULL COMMENT '反馈结果(FIT/TOO_LARGE/TOO_SMALL)',
    issue_parts VARCHAR(255) NULL DEFAULT NULL COMMENT '问题部位，逗号分隔',
    note VARCHAR(500) NULL DEFAULT NULL COMMENT '补充说明',
    create_by VARCHAR(64) NULL DEFAULT '' COMMENT '创建者',
    create_time DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    update_by VARCHAR(64) NULL DEFAULT '' COMMENT '更新者',
    update_time DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (feedback_id),
    UNIQUE KEY uk_size_feedback_order_item (order_item_id),
    KEY idx_size_feedback_user (user_id),
    KEY idx_size_feedback_order (order_id),
    CONSTRAINT fk_size_feedback_order_item FOREIGN KEY (order_item_id) REFERENCES s_order_items (order_item_id) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='尺码反馈表';

CREATE TABLE IF NOT EXISTS recommendation_log (
    log_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '推荐日志ID',
    user_id BIGINT NULL DEFAULT NULL COMMENT '用户ID',
    request_source VARCHAR(64) NULL DEFAULT NULL COMMENT '请求来源',
    input_summary TEXT NULL COMMENT '推荐输入摘要',
    result_summary TEXT NULL COMMENT '推荐结果摘要',
    confidence_score INT NULL DEFAULT NULL COMMENT '置信度分数',
    confidence_level VARCHAR(32) NULL DEFAULT NULL COMMENT '置信度等级',
    create_by VARCHAR(64) NULL DEFAULT '' COMMENT '创建者',
    create_time DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    PRIMARY KEY (log_id),
    KEY idx_recommendation_log_user (user_id),
    KEY idx_recommendation_log_source (request_source)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='尺码推荐日志表';
