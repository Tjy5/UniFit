CREATE TABLE IF NOT EXISTS admin_user (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(100) DEFAULT '',
    avatar VARCHAR(255) DEFAULT '',
    status TINYINT DEFAULT 1 COMMENT '0=禁用, 1=启用',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS admin_oper_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    module VARCHAR(100) COMMENT '操作模块: uniform/orders/reviews/...',
    operation VARCHAR(100) COMMENT '操作类型: INSERT/UPDATE/DELETE/EXPORT',
    method VARCHAR(200),
    request_url VARCHAR(500),
    request_body TEXT,
    response_code INT,
    ip_address VARCHAR(50),
    oper_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Public repositories should not ship an enabled default administrator account.
-- Create the first admin user with an environment-specific, BCrypt-encoded
-- password during deployment or local database setup.
