-- ============================================================
-- Hawkeye Cloud — 漏洞管理服务 建表 DDL
-- ============================================================

-- 漏洞模板主表
CREATE TABLE IF NOT EXISTS `vul_template`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `template_id`    VARCHAR(255) NOT NULL COMMENT 'YAML id，业务唯一标识，如 CVE-2024-0012',
    `name`           VARCHAR(512)          DEFAULT NULL COMMENT '模板名称',
    `description`    TEXT                  DEFAULT NULL COMMENT '漏洞描述',
    `author`         VARCHAR(512)          DEFAULT NULL COMMENT '作者（逗号分隔）',
    `severity`       VARCHAR(20)           DEFAULT NULL COMMENT '严重程度: info/low/medium/high/critical/unknown',
    `tags`           VARCHAR(2048)         DEFAULT NULL COMMENT '逗号分隔标签，用于关键词搜索',
    `reference`      TEXT                  DEFAULT NULL COMMENT '参考链接（JSON数组）',
    `classification` TEXT                  DEFAULT NULL COMMENT 'CVE/CWE/CVSS/EPSS 标准分类（JSON）',
    `metadata`       TEXT                  DEFAULT NULL COMMENT '扩展元数据（JSON）',
    `flow`           VARCHAR(2000)         DEFAULT NULL COMMENT '执行流程表达式，如 http(1) && http(2)',
    `variables`      TEXT                  DEFAULT NULL COMMENT '模板级动态变量（JSON）',
    `http_requests`  MEDIUMTEXT            DEFAULT NULL COMMENT 'HTTP请求定义（JSON数组）',
    `matchers`       MEDIUMTEXT            DEFAULT NULL COMMENT '匹配器定义（JSON）',
    `extractors`     MEDIUMTEXT            DEFAULT NULL COMMENT '提取器定义（JSON数组）',
    `enabled`        TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '是否启用',
    `version`        INT         NOT NULL DEFAULT 1 COMMENT '模板版本号',
    `tenant_id`      BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID（多租户隔离）',
    `create_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      BIGINT               DEFAULT NULL COMMENT '创建人',
    `update_by`      BIGINT               DEFAULT NULL COMMENT '更新人',
    `deleted`        TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_id` (`template_id`),
    KEY              `idx_severity` (`severity`),
    KEY              `idx_name` (`name`),
    KEY              `idx_enabled` (`enabled`),
    FULLTEXT KEY `ft_tags` (`tags`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='漏洞检测模板表';

-- 漏洞分类表（树形结构）
CREATE TABLE IF NOT EXISTS `vul_category`
(
    `category_id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类主键',
    `name`        VARCHAR(128) NOT NULL COMMENT '分类名称',
    `parent_id`   BIGINT                DEFAULT NULL COMMENT '父分类ID（NULL=顶级分类）',
    `description` VARCHAR(500)          DEFAULT NULL COMMENT '分类描述',
    `tenant_id`   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   BIGINT                DEFAULT NULL COMMENT '创建人',
    `update_by`   BIGINT                DEFAULT NULL COMMENT '更新人',
    `deleted`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`category_id`),
    KEY           `idx_parent_id` (`parent_id`),
    KEY           `idx_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='漏洞分类表';

-- 模板-分类关联表（多对多）
CREATE TABLE IF NOT EXISTS `vul_category_mapping`
(
    `id`          BIGINT     NOT NULL AUTO_INCREMENT COMMENT '主键',
    `template_id` BIGINT     NOT NULL COMMENT '模板ID（关联 vul_template.id）',
    `category_id` BIGINT     NOT NULL COMMENT '分类ID（关联 vul_category.category_id）',
    `tenant_id`   BIGINT     NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   BIGINT              DEFAULT NULL COMMENT '创建人',
    `update_by`   BIGINT              DEFAULT NULL COMMENT '更新人',
    `deleted`     TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_category` (`template_id`, `category_id`),
    KEY           `idx_category_id` (`category_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='漏洞模板-分类关联表';
