-- ============================================================
-- Hawkeye Cloud — 漏洞管理服务 & 检测服务 建表 DDL
-- 版本：v3.0（11 表）
-- ============================================================
-- 约定：
--   主键          table_id 格式（如 template_id, http_id）
--   租户          0 = 平台通用, >0 = 租户私有
--   逻辑删除       deleted_at BIGINT UNSIGNED, 0=未删除
--   布尔字段       TINYINT UNSIGNED, 0/1, 前缀 is_ / has_ / 动词
--   JSON字段      无默认值, 应用层判空
--   BIGINT       全部 UNSIGNED
--   时间          DATETIME, NOT NULL, 有默认值
--   TEXT/MEDIUMTEXT 独立 vul_text_content 表存储
--   condition     是 MySQL 保留字, 用 inner_condition 代替
-- ============================================================
-- ★ 持久层：将 MP 租户 SQL 拼接置为 WHERE 最前面，以命中索引
-- ============================================================


-- ============================================================
-- 1. 漏洞模板主表
--    只存元数据。检测逻辑拆到 http_step / matcher / extractor。
--    简单模板通过 UI 创建，复杂模板上传 YAML 自动解析。
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_template`
(
    `template_id`  BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
    `yaml_id`      VARCHAR(64) NOT NULL COMMENT 'YAML id',
    `name`         VARCHAR(128)     NOT NULL COMMENT '模板名称',
    `author`       VARCHAR(128) COMMENT '作者, 逗号分隔多人',
    `description`  VARCHAR(512) COMMENT '漏洞描述',
    `impact`       VARCHAR(512) COMMENT '漏洞影响说明',
    `severity`     VARCHAR(20)      NOT NULL DEFAULT 'unknown' COMMENT '严重程度: critical/high/medium/low/info/unknown',
    `metadata`     JSON COMMENT '自定义元数据, 自由 key-value',
    `cve_id`       VARCHAR(50) COMMENT 'CVE 编号, 如 CVE-2024-27292',
    `cwe_id`       VARCHAR(50) COMMENT 'CWE 编号, 复合用逗号分隔如 CWE-20,CWE-77',
    `cvss_metrics` VARCHAR(128) COMMENT 'CVSS 向量, 如 3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H',
    `cvss_score`   DOUBLE COMMENT 'CVSS 评分 0.0-10.0',
    `epss_score`   DOUBLE COMMENT 'EPSS 评分 0.0-1.0',
    `cpe`          VARCHAR(128) COMMENT 'CPE 标识, 如 cpe:/a:vendor:product:version',
    `remediation`  VARCHAR(128) COMMENT '修复建议',
    `flow`         VARCHAR(1024) COMMENT '多步骤执行流表达式, 如 http(1) && http(2)',
    `variables`    JSON COMMENT '模板级动态变量, 如 {"num":"{{rand_int(800000,999999)}}"}',
    `enabled`      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '启用: 0=禁用, 1=启用',
    `tenant_id`    BIGINT UNSIGNED  NOT NULL DEFAULT 0 COMMENT '租户ID, 0=平台通用',
    `create_time`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '创建人用户名',
    `update_by`    VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '更新人用户名',
    `deleted_at`   BIGINT UNSIGNED  NOT NULL DEFAULT 0 COMMENT '删除时间戳(毫秒), 0=未删除',
    PRIMARY KEY (`template_id`),
    UNIQUE KEY `uk_tenant_name_deleted` (`tenant_id`, `name`, `deleted_at`),
    UNIQUE KEY `uk_tenant_yaml_deleted` (`tenant_id`, `yaml_id`, `deleted_at`),
    KEY `idx_list_query` (`tenant_id`, `enabled`, `severity`, `create_time`),
    KEY `idx_name` (`name`(32))
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='漏洞检测模板主表';


-- ============================================================
-- 2. 标签字典表
--    标签由模板导入时自动创建, 不提供手动 CRUD
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_tag`
(
    `tag_id`      BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(100)    NOT NULL COMMENT '标签名',
    `tenant_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID, 0=平台通用',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '创建人用户名',
    `update_by`   VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '更新人用户名',
    `deleted_at`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳(毫秒), 0=未删除',
    PRIMARY KEY (`tag_id`),
    UNIQUE KEY `uk_tenant_name_deleted` (`tenant_id`, `name`, `deleted_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='漏洞标签字典表';


-- ============================================================
-- 3. 模板-标签关联表（M2M）
--    删除时 UPDATE deleted_at = UNIX_TIMESTAMP(), 不保留历史行
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_template_tag`
(
    `template_id` BIGINT UNSIGNED NOT NULL COMMENT '模板ID',
    `tag_id`      BIGINT UNSIGNED NOT NULL COMMENT '标签ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID, 0=平台通用',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '创建人用户名',
    `update_by`   VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '更新人用户名',
    `deleted_at`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳(毫秒), 0=未删除',
    PRIMARY KEY (`template_id`, `tag_id`),
    KEY `idx_tag_id` (`tag_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='模板-标签关联表';


-- ============================================================
-- 4. 大文本存储表
--    body / raw 等 MEDIUMTEXT 字段独立存储,
--    http_step 只存 text_id 引用, 减少主表宽度
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_text_content`
(
    `text_id`     BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
    `content`     MEDIUMTEXT      NOT NULL COMMENT '文本内容',
    `tenant_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID, 0=平台通用',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '创建人用户名',
    `update_by`   VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '更新人用户名',
    `deleted_at`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳(毫秒), 0=未删除',
    PRIMARY KEY (`text_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='大文本内容存储表';

-- ============================================================
-- 5. HTTP 请求步骤表
--    每个模板 1~N 步, step_order 从 1 开始
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_http_step`
(
    `http_id`             BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
    `template_id`         BIGINT UNSIGNED  NOT NULL COMMENT '模板ID',
    `step_order`          INT UNSIGNED     NOT NULL DEFAULT 1 COMMENT '步骤顺序, 1-based',
    `http_name`           VARCHAR(128) COMMENT '步骤命名, 用于多步骤条件匹配',
    `method`              VARCHAR(10) NOT NULL COMMENT 'HTTP 方法: GET/HEAD/POST/PUT/DELETE/CONNECT/OPTIONS/TRACE/PATCH/PURGE',
    `path`                JSON COMMENT '路径数组, 如 ["{{BaseURL}}/admin"]',
    `headers`             JSON COMMENT '请求头键值对',
    `body_text_id`        BIGINT UNSIGNED COMMENT '请求体文本ID → vul_text_content',
    `raw_text_id`         BIGINT UNSIGNED COMMENT '原始HTTP文本ID → vul_text_content (含请求行+头+体)',
    `matchers_condition`  VARCHAR(3)                DEFAULT 'or' COMMENT '步骤内多个matcher间关系: and / or',
    `attack`              VARCHAR(20) COMMENT '爆破模式: batteringram / pitchfork / clusterbomb',
    `payloads`            JSON COMMENT 'payload 定义, 键值对或文件引用',
    `stop_at_first_match` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '首个path命中即停止: 0=否, 1=是',
    `self_contained`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '请求是否自包含(不依赖外部变量): 0=否, 1=是',
    `redirects`           TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否遵循重定向: 0=否, 1=是',
    `max_redirects`       INT UNSIGNED COMMENT '最大重定向次数, 仅 redirects=1 时有效',
    `host_redirects`      TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否仅同主机重定向: 0=否, 1=是',
    `unsafe`              TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否使用rawhttp引擎: 0=否, 1=是',
    `cookie_reuse`        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否复用cookie: 0=否, 1=是',
    `req_condition`       TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否多请求条件匹配: 0=否, 1=是',
    `tenant_id`           BIGINT UNSIGNED  NOT NULL DEFAULT 0 COMMENT '租户ID, 0=平台通用',
    `create_time`         DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`           VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '创建人用户名',
    `update_by`           VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '更新人用户名',
    `deleted_at`          BIGINT UNSIGNED  NOT NULL DEFAULT 0 COMMENT '删除时间戳(毫秒), 0=未删除',
    PRIMARY KEY (`http_id`),
    KEY `idx_template_id` (`template_id`),
    KEY `idx_template_step` (`template_id`, `step_order`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='HTTP请求步骤表';


-- ============================================================
-- 6. 匹配器表
--    step_order = NULL → 全局 matcher（不属于任何步骤）
--    step_order != NULL → 某步骤专属 matcher
--    inner_condition 为本 matcher 内多条规则的关系
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_matcher`
(
    `matcher_id`       BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
    `template_id`      BIGINT UNSIGNED  NOT NULL COMMENT '模板ID',
    `step_order`       INT COMMENT '所属步骤序号, NULL=全局matcher',
    `type`             VARCHAR(20)      NOT NULL COMMENT '类型: word/status/dsl/regex/size/xpath/binary',
    `part`             VARCHAR(20) COMMENT '匹配目标(body/header/all/content_type/location 等)',
    `inner_condition`  VARCHAR(3)       NOT NULL DEFAULT 'or' COMMENT '本matcher内多条规则关系: and/or',
    `negative`         TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否反转结果: 0=否, 1=是',
    `case_insensitive` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否忽略大小写: 0=否, 1=是',
    `internal`         TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否仅内部使用: 0=否, 1=是(不输出到结果)',
    `match_all`        TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否启用所有值匹配: 0=否, 1=是',
    `matcher_name`     VARCHAR(255) COMMENT 'matcher名称标识, 日志/调试用',
    `config`           JSON             NOT NULL COMMENT '类型特定配置: words[]/status[]/dsl[]/regex[] 等',
    `tenant_id`        BIGINT UNSIGNED  NOT NULL DEFAULT 0 COMMENT '租户ID, 0=平台通用',
    `create_time`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`        VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '创建人用户名',
    `update_by`        VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '更新人用户名',
    `deleted_at`       BIGINT UNSIGNED  NOT NULL DEFAULT 0 COMMENT '删除时间戳(毫秒), 0=未删除',
    PRIMARY KEY (`matcher_id`),
    KEY `idx_template_id` (`template_id`),
    KEY `idx_type` (`type`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='匹配器表';


-- ============================================================
-- 7. 提取器表
--    从 HTTP 响应中提取变量, 供后续步骤使用
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_extractor`
(
    `extractor_id`   BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
    `template_id`    BIGINT UNSIGNED  NOT NULL COMMENT '模板ID',
    `step_order`     INT COMMENT '所属步骤序号, NULL=全局extractor',
    `type`           VARCHAR(20)      NOT NULL COMMENT '类型: regex/json/kval/dsl/xpath',
    `part`           VARCHAR(20) COMMENT '提取源: body/header',
    `extractor_name` VARCHAR(255) COMMENT '提取变量名, 写入 VariableContext 的 key',
    `config`         JSON             NOT NULL COMMENT '类型特定配置: regex[]/json[]/kval[]/dsl[] 等',
    `internal`       TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否仅内部传递: 0=否, 1=是',
    `group_num`      INT                       DEFAULT 1 COMMENT '正则捕获组编号, 仅 type=regex 时有效',
    `tenant_id`      BIGINT UNSIGNED  NOT NULL DEFAULT 0 COMMENT '租户ID, 0=平台通用',
    `create_time`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`      VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '创建人用户名',
    `update_by`      VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '更新人用户名',
    `deleted_at`     BIGINT UNSIGNED  NOT NULL DEFAULT 0 COMMENT '删除时间戳(毫秒), 0=未删除',
    PRIMARY KEY (`extractor_id`),
    KEY `idx_template_id` (`template_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='提取器表';

-- ============================================================
-- 8. 漏洞分类表（树形结构）
--    通过 parent_id 自引用实现层级
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_category`
(
    `category_id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(128)    NOT NULL COMMENT '分类名称',
    `parent_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父分类ID, 0=顶级分类',
    `sort_order`  INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '同级排序',
    `description` VARCHAR(500) COMMENT '分类描述',
    `tenant_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID, 0=平台通用',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '创建人用户名',
    `update_by`   VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '更新人用户名',
    `deleted_at`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳(毫秒), 0=未删除',
    PRIMARY KEY (`category_id`),
    UNIQUE KEY `uk_tenant_name_parent_deleted` (`tenant_id`, `name`, `parent_id`, `deleted_at`),
    KEY `idx_parent_deleted` (`parent_id`, `deleted_at`),
    KEY `idx_tenant_deleted` (`tenant_id`, `deleted_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='漏洞分类表';


-- ============================================================
-- 9. 模板-分类关联表（M2M）
--    删除时 UPDATE deleted_at, 重新关联时 UPDATE 回 0
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_template_category`
(
    `template_id` BIGINT UNSIGNED NOT NULL COMMENT '模板ID',
    `category_id` BIGINT UNSIGNED NOT NULL COMMENT '分类ID',
    `tenant_id`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID, 0=平台通用',
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`   VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '创建人用户名',
    `update_by`   VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '更新人用户名',
    `deleted_at`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳(毫秒), 0=未删除',
    PRIMARY KEY (`template_id`, `category_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='模板-分类关联表';


-- ============================================================
-- 10. 参考链接表
--     从 YAML info.reference 数组拆分, 每条一个链接
-- ============================================================
CREATE TABLE IF NOT EXISTS `vul_reference`
(
    `reference_id` BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
    `template_id`  BIGINT UNSIGNED NOT NULL COMMENT '模板ID',
    `url`          VARCHAR(2048)   NOT NULL COMMENT '参考链接',
    `title`        VARCHAR(512) COMMENT '链接标题',
    `tenant_id`    BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID, 0=平台通用',
    `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`    VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '创建人用户名',
    `update_by`    VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '更新人用户名',
    `deleted_at`   BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '删除时间戳(毫秒), 0=未删除',
    PRIMARY KEY (`reference_id`),
    KEY `idx_template_id` (`template_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='漏洞参考链接表';


-- ============================================================
-- 11. 检测结果表（vul-service DDL 统一管理，detection-service 写入）
--     detection-service 写入, vul-service DDL 统一管理
--     不可变事件日志, 不继承 BaseEntity(无 create_by/update_by/deleted)
-- ============================================================
CREATE TABLE IF NOT EXISTS `detection_result`
(
    `result_id`            BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
    `task_id`              BIGINT UNSIGNED NOT NULL COMMENT '检测任务ID',
    `task_item_id`         BIGINT UNSIGNED NOT NULL COMMENT '任务条目ID(幂等键)',
    `template_id`          BIGINT UNSIGNED NOT NULL COMMENT '漏洞模板ID',
    `asset_id`             BIGINT UNSIGNED NOT NULL COMMENT '资产ID',
    `status`               VARCHAR(20)     NOT NULL COMMENT '检测结果: matched/not_matched/error',
    `response_status_code` INT COMMENT '目标HTTP响应状态码',
    `response_size`        INT COMMENT '响应体大小(字节)',
    `response_summary`     VARCHAR(2048) COMMENT '响应摘要(截断)',
    `matched_matcher`      VARCHAR(255) COMMENT '命中的matcher名称',
    `matched_at`           DATETIME COMMENT '命中时间',
    `error_message`        VARCHAR(2048) COMMENT '错误信息',
    `duration_ms`          INT COMMENT 'HTTP请求耗时(毫秒)',
    `tenant_id`            BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '租户ID',
    `create_time`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`result_id`),
    UNIQUE KEY `uk_task_item_id` (`task_item_id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_template_id` (`template_id`),
    KEY `idx_asset_id` (`asset_id`),
    KEY `idx_status` (`status`),
    KEY `idx_tenant_task` (`tenant_id`, `task_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='检测结果表';
