-- SUIOS public schema snapshot.
-- This file intentionally contains schema only. Do not add exported user,
-- address, order, login, password hash, or other runtime data to this file.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `recommendation_log`;
CREATE TABLE `recommendation_log` (
  `log_id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NULL DEFAULT NULL,
  `school_id` bigint(0) NULL DEFAULT NULL,
  `uniform_id` bigint(0) NULL DEFAULT NULL,
  `category_key` varchar(64) NULL DEFAULT NULL,
  `height` decimal(10,2) NULL DEFAULT NULL,
  `weight` decimal(10,2) NULL DEFAULT NULL,
  `recommended_size_id` bigint(0) NULL DEFAULT NULL,
  `recommended_size_name` varchar(64) NULL DEFAULT NULL,
  `order_item_id` bigint(0) NULL DEFAULT NULL,
  `request_source` varchar(64) NULL DEFAULT NULL,
  `confidence_score` decimal(10,4) NULL DEFAULT NULL,
  `calibration_applied` tinyint(1) NOT NULL DEFAULT 0,
  `calibration_details` json NULL,
  `personalization_applied` tinyint(1) NOT NULL DEFAULT 0,
  `personalization_details` json NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `s_shopping_cart_items`;
CREATE TABLE `s_shopping_cart_items` (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `uniform_id` bigint(0) NOT NULL,
  `size_id` bigint(0) NULL DEFAULT NULL,
  `quantity` int(0) NOT NULL DEFAULT 1,
  `recommendation_log_id` bigint(0) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `s_order_items`;
CREATE TABLE `s_order_items` (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `order_id` bigint(0) NOT NULL,
  `uniform_id` bigint(0) NOT NULL,
  `size_id` bigint(0) NULL DEFAULT NULL,
  `uniform_name_snapshot` varchar(255) NULL DEFAULT NULL,
  `size_name_snapshot` varchar(64) NULL DEFAULT NULL,
  `quantity` int(0) NOT NULL DEFAULT 1,
  `unit_price` decimal(10,2) NOT NULL,
  `total_price` decimal(10,2) NOT NULL,
  `image_snapshot` varchar(8000) NULL DEFAULT NULL,
  `review_id` bigint(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_orderitem_uniform_size`(`uniform_id`, `size_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `s_uniform`;
CREATE TABLE `s_uniform` (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `school` varchar(255) NULL DEFAULT NULL,
  `grade` varchar(255) NULL DEFAULT NULL,
  `category_key` varchar(64) NULL DEFAULT NULL,
  `intro` varchar(255) NULL DEFAULT NULL,
  `image` varchar(8000) NULL DEFAULT NULL,
  `price` decimal(10,2) NULL DEFAULT NULL,
  `status` bigint(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `s_inventory_sku`;
CREATE TABLE `s_inventory_sku` (
  `sku_id` bigint(0) NOT NULL AUTO_INCREMENT,
  `uniform_id` bigint(0) NOT NULL,
  `size_id` bigint(0) NOT NULL,
  `stock_quantity` int(0) NOT NULL DEFAULT 0,
  `reserved_quantity` int(0) NOT NULL DEFAULT 0,
  `safety_stock` int(0) NOT NULL DEFAULT 0,
  `reorder_point` int(0) NOT NULL DEFAULT 0,
  `status` varchar(32) NOT NULL DEFAULT 'ACTIVE',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`sku_id`) USING BTREE,
  UNIQUE INDEX `uk_inventory_sku_uniform_size_status`(`uniform_id`, `size_id`, `status`) USING BTREE,
  INDEX `idx_inventory_sku_low_stock`(`status`, `stock_quantity`, `reorder_point`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `s_inventory_movements`;
CREATE TABLE `s_inventory_movements` (
  `movement_id` bigint(0) NOT NULL AUTO_INCREMENT,
  `sku_id` bigint(0) NOT NULL,
  `movement_type` varchar(32) NOT NULL,
  `quantity_delta` int(0) NOT NULL,
  `before_quantity` int(0) NOT NULL,
  `after_quantity` int(0) NOT NULL,
  `related_order_id` bigint(0) NULL DEFAULT NULL,
  `related_order_item_id` bigint(0) NULL DEFAULT NULL,
  `remark` varchar(500) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`movement_id`) USING BTREE,
  CONSTRAINT `fk_inventory_movement_order_item` FOREIGN KEY (`related_order_item_id`) REFERENCES `s_order_items` (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `size_feedback`;
CREATE TABLE `size_feedback` (
  `feedback_id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `order_id` bigint(0) NOT NULL,
  `order_item_id` bigint(0) NOT NULL,
  `recommended_size` varchar(64) NULL DEFAULT NULL,
  `purchased_size` varchar(64) NULL DEFAULT NULL,
  `satisfaction` varchar(32) NULL DEFAULT NULL,
  `issue_parts` json NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`feedback_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `calibration_params`;
CREATE TABLE `calibration_params` (
  `param_id` bigint(0) NOT NULL AUTO_INCREMENT,
  `scope_type` varchar(32) NOT NULL,
  `scope_id` bigint(0) NULL DEFAULT NULL,
  `target_size_id` bigint(0) NOT NULL,
  `adjustment_value` decimal(10,4) NOT NULL DEFAULT 0,
  `sample_size` int(0) NOT NULL DEFAULT 0,
  `unique_user_count` int(0) NOT NULL DEFAULT 0,
  `feedback_distribution` json NULL,
  `confidence_level` decimal(10,4) NULL DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`param_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `calibration_audit`;
CREATE TABLE `calibration_audit` (
  `audit_id` bigint(0) NOT NULL AUTO_INCREMENT,
  `param_id` bigint(0) NOT NULL,
  `action` varchar(32) NOT NULL,
  `old_value` json NULL,
  `new_value` json NULL,
  `reason` varchar(500) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`audit_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `user_size_preference_profile`;
CREATE TABLE `user_size_preference_profile` (
  `profile_id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `category_key` varchar(64) NOT NULL,
  `explicit_preference` varchar(32) NULL DEFAULT NULL,
  `learned_direction` decimal(10,4) NULL DEFAULT NULL,
  `source_summary` json NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`profile_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `user_size_preference_event`;
CREATE TABLE `user_size_preference_event` (
  `event_id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NOT NULL,
  `order_item_id` bigint(0) NULL DEFAULT NULL,
  `feedback_id` bigint(0) NULL DEFAULT NULL,
  `uniform_id` bigint(0) NULL DEFAULT NULL,
  `category_key` varchar(64) NULL DEFAULT NULL,
  `recommended_size` varchar(64) NULL DEFAULT NULL,
  `purchased_size` varchar(64) NULL DEFAULT NULL,
  `satisfaction` varchar(32) NULL DEFAULT NULL,
  `issue_parts` json NULL,
  `direction_delta` decimal(10,4) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `s_order_status_logs`;
CREATE TABLE `s_order_status_logs` (
  `id` bigint(0) NOT NULL AUTO_INCREMENT,
  `order_id` bigint(0) NOT NULL,
  `event_type` varchar(64) NOT NULL,
  `actor_type` varchar(32) NOT NULL,
  `actor_id` bigint(0) NULL DEFAULT NULL,
  `from_order_status` varchar(32) NULL DEFAULT NULL,
  `to_order_status` varchar(32) NULL DEFAULT NULL,
  `from_payment_status` varchar(32) NULL DEFAULT NULL,
  `to_payment_status` varchar(32) NULL DEFAULT NULL,
  `from_shipping_status` varchar(32) NULL DEFAULT NULL,
  `to_shipping_status` varchar(32) NULL DEFAULT NULL,
  `reason` varchar(500) NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

DROP TABLE IF EXISTS `s_user_activity_logs`;
CREATE TABLE `s_user_activity_logs` (
  `log_id` bigint(0) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(0) NULL DEFAULT NULL,
  `session_id` varchar(128) NULL DEFAULT NULL,
  `action_type` varchar(64) NOT NULL,
  `target_type` varchar(64) NULL DEFAULT NULL,
  `target_id` bigint(0) NULL DEFAULT NULL,
  `log_detail` json NULL,
  `request_url` varchar(500) NULL DEFAULT NULL,
  `request_method` varchar(16) NULL DEFAULT NULL,
  `log_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`) USING BTREE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
