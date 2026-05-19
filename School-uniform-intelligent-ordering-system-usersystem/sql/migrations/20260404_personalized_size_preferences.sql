ALTER TABLE s_uniform
    ADD COLUMN category_key VARCHAR(64) NULL COMMENT '校服品类键' AFTER grade_id,
    ADD INDEX idx_s_uniform_category_key (category_key);

ALTER TABLE recommendation_log
    ADD COLUMN category_key VARCHAR(64) NULL COMMENT '推荐品类键' AFTER uniform_id,
    ADD COLUMN personalization_applied BOOLEAN DEFAULT FALSE COMMENT '是否应用个人偏好' AFTER calibration_details,
    ADD COLUMN personalization_details TEXT NULL COMMENT '个人偏好摘要JSON' AFTER personalization_applied,
    ADD INDEX idx_recommendation_log_category (category_key);

CREATE TABLE IF NOT EXISTS user_size_preference_profile (
    profile_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '偏好画像ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    category_key VARCHAR(64) NOT NULL COMMENT '品类键',
    explicit_preference VARCHAR(32) NULL COMMENT '显式偏好: LOOSE/STANDARD/SLIM',
    learned_direction VARCHAR(32) NULL COMMENT '学习方向: LOOSE/STANDARD/SLIM',
    confidence_level VARCHAR(32) NULL COMMENT '置信等级: LOW/MEDIUM/HIGH',
    confidence_score INT NULL COMMENT '置信分数',
    sample_count INT NOT NULL DEFAULT 0 COMMENT '样本数',
    fit_count INT NOT NULL DEFAULT 0 COMMENT '合身反馈数',
    too_small_count INT NOT NULL DEFAULT 0 COMMENT '偏小反馈数',
    too_large_count INT NOT NULL DEFAULT 0 COMMENT '偏大反馈数',
    loose_signal_count INT NOT NULL DEFAULT 0 COMMENT '偏宽松信号数',
    slim_signal_count INT NOT NULL DEFAULT 0 COMMENT '偏修身信号数',
    standard_signal_count INT NOT NULL DEFAULT 0 COMMENT '标准信号数',
    source_summary TEXT NULL COMMENT '画像来源摘要JSON',
    create_by VARCHAR(64) NULL DEFAULT '' COMMENT '创建者',
    create_time DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    update_by VARCHAR(64) NULL DEFAULT '' COMMENT '更新者',
    update_time DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (profile_id),
    UNIQUE KEY uk_user_size_preference_profile_user_category (user_id, category_key),
    KEY idx_user_size_preference_profile_user_category (user_id, category_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户尺码偏好画像';

CREATE TABLE IF NOT EXISTS user_size_preference_event (
    event_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '偏好事件ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    order_id BIGINT NULL COMMENT '订单ID',
    order_item_id BIGINT NULL COMMENT '订单项ID',
    feedback_id BIGINT NULL COMMENT '反馈ID',
    uniform_id BIGINT NULL COMMENT '校服ID',
    category_key VARCHAR(64) NOT NULL COMMENT '品类键',
    recommended_size VARCHAR(64) NULL COMMENT '推荐尺码',
    purchased_size VARCHAR(64) NULL COMMENT '购买尺码',
    satisfaction VARCHAR(32) NULL COMMENT '反馈结果',
    issue_parts VARCHAR(255) NULL COMMENT '问题部位',
    direction_delta INT NOT NULL DEFAULT 0 COMMENT '方向信号: 1宽松, -1修身, 0标准',
    source_type VARCHAR(32) NOT NULL DEFAULT 'FEEDBACK' COMMENT '来源类型',
    create_by VARCHAR(64) NULL DEFAULT '' COMMENT '创建者',
    create_time DATETIME NULL DEFAULT NULL COMMENT '创建时间',
    update_by VARCHAR(64) NULL DEFAULT '' COMMENT '更新者',
    update_time DATETIME NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (event_id),
    UNIQUE KEY uk_user_size_preference_event_order_item (order_item_id),
    UNIQUE KEY uk_user_size_preference_event_feedback (feedback_id),
    KEY idx_user_size_preference_event_user_category (user_id, category_key),
    KEY idx_user_size_preference_event_uniform (uniform_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户尺码偏好事件';
