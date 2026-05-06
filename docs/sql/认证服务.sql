-- ============================================================
-- Hawkeye Cloud — 认证服务 建表 DDL
-- 版本：v3.0（与 vul/asset 规范统一）
-- ============================================================
-- 约定：
--   主键          table_id 格式（account_id）
--   租户          0 = 平台通用, >0 = 租户私有
--   逻辑删除       deleted_at BIGINT UNSIGNED, 0=未删除
--   BIGINT       全部 UNSIGNED
--   时间          DATETIME, NOT NULL, 有默认值
--   人            VARCHAR(64) — 直接存用户名
-- ============================================================

USE hawkeye;

DROP TABLE IF EXISTS `account`;
CREATE TABLE IF NOT EXISTS `account`
(
    `account_id`  BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
    `username`    VARCHAR(64)     NOT NULL COMMENT '用户名',
    `password`    VARCHAR(128)    NOT NULL COMMENT 'BCrypt 密码',
    `tenant_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID, 0=平台通用',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '创建人用户名',
    `update_by`   VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '更新人用户名',
    `deleted_at`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳(毫秒), 0=未删除',
    PRIMARY KEY (`account_id`),
    UNIQUE KEY `uk_username_deleted` (`username`, `deleted_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT = '账号表';

-- 初始管理员 / 密码是 password
INSERT INTO account(username, password, tenant_id)
VALUES ('admin', '$2a$10$gp1t.ArJv56l/aTNemMube9qdo1wHUhi5Fj2StYYhoPWK3QLg7jFS', 0);
