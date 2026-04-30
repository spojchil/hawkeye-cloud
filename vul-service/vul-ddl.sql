-- ============================================================
-- Hawkeye Cloud — 漏洞管理服务 & 检测服务 建表 DDL
-- 版本：v2.0（重构）
-- 说明：将原 vul_template 大 JSON 字段拆分归一化，
--       同时设计 detection_result 表供检测服务使用。
--
-- ⚠ 执行前请确保已经备份数据，或确认可以清空旧表。
--   下面 DROP 会删除旧版 vul 相关所有表。
-- ============================================================

-- 清理旧表（v1.0 遗留）
DROP TABLE IF EXISTS `vul_category_mapping`;
DROP TABLE IF EXISTS `vul_category`;
DROP TABLE IF EXISTS `vul_template`;

-- ============================================================
-- 1. 漏洞模板主表（只存元数据 + 1:1 分类信息）
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_template`
(
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `template_id`    VARCHAR(255) NOT NULL COMMENT '业务唯一键，来自 Nuclei YAML id',
    `name`           VARCHAR(512) NOT NULL COMMENT '模板名称',
    `description`    VARCHAR(2048)         DEFAULT NULL COMMENT '漏洞描述',
    `author`         VARCHAR(512)          DEFAULT NULL COMMENT '作者（逗号分隔多人）',
    `severity`       VARCHAR(20)  NOT NULL DEFAULT 'unknown' COMMENT '严重程度：critical/high/medium/low/info/unknown',
    `cve_id`         VARCHAR(50)           DEFAULT NULL COMMENT 'CVE 编号，如 CVE-2024-27292',
    `cwe_id`         VARCHAR(50)           DEFAULT NULL COMMENT 'CWE 编号，可复合如 CWE-20,CWE-77',
    `cvss_score`     DECIMAL(4,2)          DEFAULT NULL COMMENT 'CVSS 评分',
    `epss_score`     DECIMAL(5,4)          DEFAULT NULL COMMENT 'EPSS 评分',
    `flow`           VARCHAR(2000)         DEFAULT NULL COMMENT '多步骤执行流表达式，如 http(1) && http(2)',
    `variables`      JSON                  DEFAULT NULL COMMENT '模板级动态变量（简单 key-value）',
    `enabled`        TINYINT(1)  NOT NULL DEFAULT 1 COMMENT '是否启用',
    `version`        INT         NOT NULL DEFAULT 1 COMMENT '模板版本号',
    `tenant_id`      BIGINT      NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      BIGINT               DEFAULT NULL COMMENT '创建人',
    `update_by`      BIGINT               DEFAULT NULL COMMENT '更新人',
    `deleted`        TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_template_id` (`template_id`),
    KEY              `idx_severity` (`severity`),
    KEY              `idx_cve_id` (`cve_id`),
    KEY              `idx_enabled` (`enabled`),
    KEY              `idx_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='漏洞检测模板主表';

-- ============================================================
-- 2. 标签字典表
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_tag`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(100) NOT NULL COMMENT '标签名',
    `tenant_id`   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   BIGINT                DEFAULT NULL COMMENT '创建人',
    `update_by`   BIGINT                DEFAULT NULL COMMENT '更新人',
    `deleted`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_name` (`name`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='漏洞标签字典表';

-- ============================================================
-- 3. 模板-标签关联表（M2M，复合主键）
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_template_tag`
(
    `template_id` BIGINT     NOT NULL COMMENT '模板ID',
    `tag_id`      BIGINT     NOT NULL COMMENT '标签ID',
    `tenant_id`   BIGINT     NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   BIGINT              DEFAULT NULL COMMENT '创建人',
    `update_by`   BIGINT              DEFAULT NULL COMMENT '更新人',
    `deleted`     TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`template_id`, `tag_id`),
    KEY           `idx_tag_id` (`tag_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='模板-标签关联表';

-- ============================================================
-- 4. 参考链接表
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_reference`
(
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `template_id` BIGINT        NOT NULL COMMENT '模板ID',
    `url`         VARCHAR(2048) NOT NULL COMMENT '参考链接',
    `title`       VARCHAR(512)           DEFAULT NULL COMMENT '链接标题',
    `tenant_id`   BIGINT        NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   BIGINT                 DEFAULT NULL COMMENT '创建人',
    `update_by`   BIGINT                 DEFAULT NULL COMMENT '更新人',
    `deleted`     TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY           `idx_template_id` (`template_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='漏洞参考链接表';

-- ============================================================
-- 5. HTTP 请求步骤表
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_http_step`
(
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `template_id`         BIGINT       NOT NULL COMMENT '模板ID',
    `step_order`          INT          NOT NULL DEFAULT 1 COMMENT '步骤顺序（1-based）',
    `method`              VARCHAR(10)           DEFAULT NULL COMMENT 'HTTP 方法：GET/POST/PUT/DELETE 等',
    `path`                JSON                  DEFAULT NULL COMMENT '路径数组，如 ["{{BaseURL}}/admin"]',
    `headers`             JSON                  DEFAULT NULL COMMENT '请求头键值对',
    `body`                MEDIUMTEXT            DEFAULT NULL COMMENT '请求体（POST payload，可能较大）',
    `raw`                 MEDIUMTEXT            DEFAULT NULL COMMENT '原始 HTTP 文本（raw 模式，含请求行+头+体）',
    `matchers_condition`  VARCHAR(3)            DEFAULT 'or' COMMENT '本步骤内多个 matcher 间的组合关系：and/or',
    `attack`              VARCHAR(20)            DEFAULT NULL COMMENT '爆破模式：batteringram/pitchfork/clusterbomb',
    `stop_at_first_match` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '首个 path 命中即停止尝试剩余 path',
    `tenant_id`           BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`           BIGINT                DEFAULT NULL COMMENT '创建人',
    `update_by`           BIGINT                DEFAULT NULL COMMENT '更新人',
    `deleted`             TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY                   `idx_template_id` (`template_id`),
    KEY                   `idx_template_step` (`template_id`, `step_order`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='HTTP请求步骤表';

-- ============================================================
-- 6. 匹配器表
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_matcher`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `template_id`      BIGINT       NOT NULL COMMENT '模板ID',
    `step_order`       INT                   DEFAULT NULL COMMENT '所属步骤序号（NULL=全局matcher）',
    `type`             VARCHAR(20)  NOT NULL COMMENT '匹配器类型：word/status/dsl/regex/size/xpath/binary',
    `part`             VARCHAR(20)           DEFAULT NULL COMMENT '匹配目标：body/header/all',
    `condition`        VARCHAR(3)   NOT NULL DEFAULT 'or' COMMENT '内部多条规则关系：and/or',
    `negative`         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否反转结果',
    `case_insensitive` TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否忽略大小写',
    `internal`         TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否仅内部使用（不输出到最终结果）',
    `name`             VARCHAR(255)          DEFAULT NULL COMMENT 'matcher 名称标识（日志/调试用）',
    `config`           JSON         NOT NULL COMMENT '类型特定配置：words[]/status[]/dsl[]/regex[] 等',
    `tenant_id`        BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`        BIGINT                DEFAULT NULL COMMENT '创建人',
    `update_by`        BIGINT                DEFAULT NULL COMMENT '更新人',
    `deleted`          TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY                `idx_template_id` (`template_id`),
    KEY                `idx_type` (`type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='匹配器表';

-- ============================================================
-- 7. 提取器表
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_extractor`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `template_id` BIGINT       NOT NULL COMMENT '模板ID',
    `step_order`  INT                   DEFAULT NULL COMMENT '所属步骤序号（NULL=全局extractor）',
    `type`        VARCHAR(20)  NOT NULL COMMENT '提取器类型：regex/json/kval/dsl',
    `part`        VARCHAR(20)           DEFAULT NULL COMMENT '提取源：body/header',
    `name`        VARCHAR(255)          DEFAULT NULL COMMENT '提取变量名（可为空）',
    `config`      JSON         NOT NULL COMMENT '类型特定配置',
    `internal`    TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '是否仅内部传递（不输出到结果）',
    `group_num`   INT                   DEFAULT 1 COMMENT '正则捕获组编号',
    `tenant_id`   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   BIGINT                DEFAULT NULL COMMENT '创建人',
    `update_by`   BIGINT                DEFAULT NULL COMMENT '更新人',
    `deleted`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY           `idx_template_id` (`template_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='提取器表';

-- ============================================================
-- 8. 漏洞分类表（树形结构）
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_category`
(
    `category_id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类主键',
    `name`        VARCHAR(128) NOT NULL COMMENT '分类名称',
    `parent_id`   BIGINT                DEFAULT NULL COMMENT '父分类ID（NULL=顶级分类）',
    `sort_order`  INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `description` VARCHAR(500)          DEFAULT NULL COMMENT '分类描述',
    `tenant_id`   BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   BIGINT                DEFAULT NULL COMMENT '创建人',
    `update_by`   BIGINT                DEFAULT NULL COMMENT '更新人',
    `deleted`     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`category_id`),
    UNIQUE KEY `uk_name_parent` (`name`, `parent_id`),
    KEY           `idx_parent_id` (`parent_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='漏洞分类表';

-- ============================================================
-- 9. 模板-分类关联表（M2M，复合主键）
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_template_category`
(
    `template_id` BIGINT     NOT NULL COMMENT '模板ID',
    `category_id` BIGINT     NOT NULL COMMENT '分类ID',
    `tenant_id`   BIGINT     NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   BIGINT              DEFAULT NULL COMMENT '创建人',
    `update_by`   BIGINT              DEFAULT NULL COMMENT '更新人',
    `deleted`     TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`template_id`, `category_id`),
    KEY           `idx_category_id` (`category_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='模板-分类关联表';

-- ============================================================
-- 10. 检测结果表（detection-service 写入，vul-service DDL 统一管理）
-- ============================================================
CREATE TABLE IF NOT EXISTS `detection_result`
(
    `id`                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id`              BIGINT       NOT NULL COMMENT '检测任务ID',
    `task_item_id`         BIGINT       NOT NULL COMMENT '任务条目ID（幂等键）',
    `template_id`          BIGINT       NOT NULL COMMENT '漏洞模板ID',
    `asset_id`             BIGINT       NOT NULL COMMENT '资产ID',
    `status`               VARCHAR(20)  NOT NULL COMMENT '检测结果：matched/not_matched/error',
    `response_status_code` INT                   DEFAULT NULL COMMENT '目标 HTTP 响应状态码',
    `response_size`        INT                   DEFAULT NULL COMMENT '响应体大小（字节）',
    `response_summary`     VARCHAR(2048)         DEFAULT NULL COMMENT '响应摘要（截断）',
    `matched_matcher`      VARCHAR(255)          DEFAULT NULL COMMENT '命中的 matcher 名称',
    `matched_at`           DATETIME              DEFAULT NULL COMMENT '命中时间',
    `error_message`        VARCHAR(2048)         DEFAULT NULL COMMENT '错误信息',
    `duration_ms`          INT                   DEFAULT NULL COMMENT 'HTTP 请求耗时（毫秒）',
    `tenant_id`            BIGINT       NOT NULL DEFAULT 1 COMMENT '租户ID',
    `create_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `uk_task_item_id` (`task_item_id`),
    KEY                    `idx_task_id` (`task_id`),
    KEY                    `idx_template_id` (`template_id`),
    KEY                    `idx_asset_id` (`asset_id`),
    KEY                    `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='检测结果表';
