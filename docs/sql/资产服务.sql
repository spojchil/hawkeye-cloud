USE hawkeye;


-- ============================================================
-- 1. 资产表
--    资产是检测的目标对象
--    ★ 分布式不建议使用自增主键, 后续可以考虑改为雪花ID
-- ============================================================
DROP TABLE IF EXISTS `asset`;
CREATE TABLE IF NOT EXISTS `asset`
(
    `asset_id`         BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
    `name`             VARCHAR(128)      NOT NULL COMMENT '资产名称',
    `request_protocol` VARCHAR(10)       NOT NULL DEFAULT 'https' COMMENT '协议: http / https',
    `request_host`     VARCHAR(255)      NOT NULL COMMENT '请求主机(域名或IP)',
    `request_port`     SMALLINT UNSIGNED NOT NULL COMMENT '端口号, -1表示使用协议默认端口（应用层处理）',
    `request_path`     VARCHAR(1024)     NOT NULL DEFAULT '/' COMMENT '请求路径',
    `description`      VARCHAR(500) COMMENT '资产描述',
    `status`           VARCHAR(20)       NOT NULL DEFAULT 'ENABLED' COMMENT '状态: DISABLED/ENABLED/DEPRECATED',
    `risk_level`       VARCHAR(20)                DEFAULT NULL COMMENT '风险等级: UNKNOWN/LOW/MEDIUM/HIGH',
    `last_scan_time`   DATETIME COMMENT '最近扫描时间',
    `tenant_id`        BIGINT UNSIGNED   NOT NULL DEFAULT 0 COMMENT '租户ID, 0=平台通用',
    `create_time`      DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME          NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`        VARCHAR(64)       NOT NULL DEFAULT '' COMMENT '创建人用户名',
    `update_by`        VARCHAR(64)       NOT NULL DEFAULT '' COMMENT '更新人用户名',
    `deleted_at`       BIGINT UNSIGNED   NOT NULL DEFAULT 0 COMMENT '删除时间戳(毫秒), 0=未删除',
    PRIMARY KEY (`asset_id`),
    KEY `idx_tenant_host` (`tenant_id`, `request_host`(100)),
    KEY `idx_tenant_status` (`tenant_id`, `deleted_at`, `status`),
    KEY `idx_tenant_scan_time` (`tenant_id`, `last_scan_time`),
    KEY `idx_tenant_name` (`tenant_id`, `name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='资产表';


-- ============================================================
-- 2. 资产分类表（支持树形）
--    通过 parent_id 自引用实现层级
-- ============================================================
DROP TABLE IF EXISTS `asset_category`;
CREATE TABLE IF NOT EXISTS `asset_category`
(
    `category_id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(128)    NOT NULL COMMENT '分类名称',
    `parent_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父分类ID, 0=顶级分类',
    `sort_order`  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '同级排序',
    `description` VARCHAR(500) COMMENT '分类描述',
    `tenant_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID, 0=平台通用',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '创建人用户名',
    `update_by`   VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '更新人用户名',
    `deleted_at`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳(毫秒), 0=未删除',
    PRIMARY KEY (`category_id`),
    UNIQUE KEY `uk_tenant_name_parent_deleted` (`tenant_id`, `name`, `parent_id`, `deleted_at`),
    KEY `idx_parent_deleted` (`parent_id`, `deleted_at`),
    KEY `idx_tenant_deleted` (`tenant_id`, `deleted_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='资产分类表';


-- ============================================================
-- 3. 资产-分类关联表（M2M）
--    删除时 UPDATE deleted_at, 同理重新关联时 UPDATE 回 0
-- ============================================================
DROP TABLE IF EXISTS `asset_category_mapping`;
CREATE TABLE IF NOT EXISTS `asset_category_mapping`
(
    `asset_id`    BIGINT UNSIGNED NOT NULL COMMENT '资产ID',
    `category_id` BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID, 0=平台通用',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '创建人用户名',
    `update_by`   VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '更新人用户名',
    `deleted_at`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳(毫秒), 0=未删除',
    PRIMARY KEY (`asset_id`, `category_id`,`deleted_at`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='资产-分类关联表';
