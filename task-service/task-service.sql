-- ============================================================
-- Hawkeye Cloud — 任务调度服务 建表 DDL
-- 版本：v4.0（合并 task_item 和 detection_result）
-- ============================================================
-- 约定：
--   主键          table_id 格式（task_id, item_id）
--   租户          0 = 平台通用, >0 = 租户私有
--   逻辑删除       deleted_at BIGINT UNSIGNED, 0=未删除
--   BIGINT       全部 UNSIGNED
--   时间          DATETIME, NOT NULL, 有默认值
--   人            VARCHAR(64) — 直接存用户名
-- ============================================================

USE hawkeye;

DROP TABLE IF EXISTS `task_item`;
DROP TABLE IF EXISTS `task`;

-- 任务表
CREATE TABLE IF NOT EXISTS `task`
(
    `task_id`         BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
    `task_name`       VARCHAR(200)     NOT NULL COMMENT '任务名称',
    `target_ids`      VARCHAR(2000)    NOT NULL COMMENT '资产 ID 列表（JSON 数组）',
    `vul_ids`         VARCHAR(2000)    NOT NULL COMMENT '模板 ID 列表（JSON 数组）',
    `status`          TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态: 0=待执行,1=执行中,2=已完成,3=已取消,4=异常终止',
    `total_items`     INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '检测项总数（拆分后回填）',
    `completed_items` INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '已完成数（轮询 Redis 回填）',
    `failed_items`    INT UNSIGNED     NOT NULL DEFAULT 0 COMMENT '失败数',
    `priority`        TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '优先级: 0=高,1=中,2=低',
    `start_time`      DATETIME COMMENT '拆分开始时间',
    `end_time`        DATETIME COMMENT '最后 item 完成时间',
    `result_summary`  JSON COMMENT '结果摘要 {"matched":3,"notMatched":46,"error":1}',
    `tenant_id`       BIGINT UNSIGNED  NOT NULL DEFAULT 0 COMMENT '租户ID, 0=平台通用',
    `create_time`     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`       VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '创建人用户名',
    `update_by`       VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '更新人用户名',
    `deleted_at`      BIGINT UNSIGNED  NOT NULL DEFAULT 0 COMMENT '删除时间戳(毫秒), 0=未删除',
    PRIMARY KEY (`task_id`),
    KEY `idx_tenant_status` (`tenant_id`, `status`),
    KEY `idx_tenant_deleted` (`tenant_id`, `deleted_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='检测任务表';


-- 检测项表（合并原 detection_result 表）
CREATE TABLE IF NOT EXISTS `task_item`
(
    `item_id`              BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
    `task_id`              BIGINT UNSIGNED  NOT NULL COMMENT '所属任务 ID',
    `asset_id`             BIGINT UNSIGNED  NOT NULL COMMENT '资产 ID',
    `vul_id`               BIGINT UNSIGNED  NOT NULL COMMENT '模板 ID',
    `status`               TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '状态: 0=待执行,1=匹配,2=未匹配,3=失败,4=跳过',
    `response_status_code` INT COMMENT 'HTTP 响应状态码',
    `response_size`        INT COMMENT '响应体大小(字节)',
    `response_summary`     VARCHAR(2048) COMMENT '响应摘要',
    `matched_matcher`      VARCHAR(255) COMMENT '命中的匹配器名称',
    `matched_at`           DATETIME COMMENT '匹配时间',
    `error_message`        VARCHAR(2048) COMMENT '错误信息',
    `duration_ms`          INT COMMENT 'HTTP请求耗时(毫秒)',
    `tenant_id`            BIGINT UNSIGNED  NOT NULL DEFAULT 0 COMMENT '租户ID, 0=平台通用',
    `create_time`          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`            VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '创建人用户名',
    `update_by`            VARCHAR(64)      NOT NULL DEFAULT '' COMMENT '更新人用户名',
    `deleted_at`           BIGINT UNSIGNED  NOT NULL DEFAULT 0 COMMENT '删除时间戳(毫秒), 0=未删除',
    PRIMARY KEY (`item_id`),
    UNIQUE KEY `uk_task_asset_vul` (`task_id`, `asset_id`, `vul_id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_asset_id` (`asset_id`),
    KEY `idx_vul_id` (`vul_id`),
    KEY `idx_task_status` (`task_id`, `status`),
    KEY `idx_tenant_deleted` (`tenant_id`, `deleted_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='检测项表（合并原detection_result）';
