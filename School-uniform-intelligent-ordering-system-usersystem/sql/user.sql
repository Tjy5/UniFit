
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 创建用户账户表
-- ----------------------------
DROP TABLE IF EXISTS `user_account`;
CREATE TABLE `user_account`  (
                                 `user_id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                                 `user_account` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户账号',
                                 `user_password` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户密码',
                                 `create_by` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                 PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '用户账号管理' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 2. 创建用户信息表（无物理外键关联）
-- ----------------------------
DROP TABLE IF EXISTS `user_message`;
CREATE TABLE `user_message`  (
                                 `message_id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
                                 `message_user_id` bigint NULL DEFAULT NULL COMMENT '用户ID（逻辑关联user_account.user_id）',
                                 `message_user_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户姓名',
                                 `message_user_age` bigint NULL DEFAULT NULL COMMENT '用户年龄',
                                 `message_user_sex` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户性别',
                                 `message_user_address` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户地址',
                                 `height` decimal(10, 2) NULL DEFAULT NULL COMMENT '身高',
                                 `weight` decimal(10, 2) NULL DEFAULT NULL COMMENT '体重',
                                 `chest` decimal(10, 2) NULL DEFAULT NULL COMMENT '胸围(cm)',
                                 `waist` decimal(10, 2) NULL DEFAULT NULL COMMENT '腰围(cm)',
                                 `hip` decimal(10, 2) NULL DEFAULT NULL COMMENT '臀围(cm)',
                                 `shoulder` decimal(10, 2) NULL DEFAULT NULL COMMENT '肩宽(cm)',
                                 `create_by` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '创建者',
                                 `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
                                 `update_by` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '更新者',
                                 `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
                                 PRIMARY KEY (`message_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 28 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '用户信息管理' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
