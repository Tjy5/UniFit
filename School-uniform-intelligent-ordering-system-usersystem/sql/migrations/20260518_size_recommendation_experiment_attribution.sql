ALTER TABLE recommendation_log
    ADD COLUMN experiment_key VARCHAR(128) NULL COMMENT '推荐实验键' AFTER order_item_id,
    ADD COLUMN experiment_variant VARCHAR(16) NULL COMMENT '推荐实验分组' AFTER experiment_key,
    ADD COLUMN strategy_version VARCHAR(64) NULL COMMENT '推荐策略版本' AFTER experiment_variant,
    ADD INDEX idx_recommendation_log_experiment_variant_time (experiment_key, experiment_variant, create_time),
    ADD INDEX idx_recommendation_log_experiment_filters (experiment_key, experiment_variant, school_id, uniform_id, create_time),
    ADD INDEX idx_recommendation_log_order_item_experiment (order_item_id, experiment_key, experiment_variant);
