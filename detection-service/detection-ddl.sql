-- ============================================================
-- Hawkeye Cloud — 检测服务 建表 DDL
-- ============================================================

CREATE TABLE IF NOT EXISTS `detection_result`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id`       BIGINT       NOT NULL COMMENT '关联的任务 ID',
    `task_item_id`  BIGINT       NOT NULL COMMENT '关联的检测项 ID（唯一键）',
    `asset_id`      BIGINT       NOT NULL COMMENT '关联的资产 ID',
    `vul_id`        BIGINT       NOT NULL COMMENT '关联的漏洞模板 ID',
    `status`        VARCHAR(20)  NOT NULL COMMENT '执行结果: SUCCESS / FAILED / ERROR / TIMEOUT',
    `response_body` MEDIUMTEXT            DEFAULT NULL COMMENT 'HTTP 响应体（截断后）',
    `status_code`   INT                   DEFAULT NULL COMMENT 'HTTP 状态码',
    `duration_ms`   BIGINT                DEFAULT NULL COMMENT '请求耗时（毫秒）',
    `error_message` VARCHAR(2000)         DEFAULT NULL COMMENT '错误信息',
    `match_detail`  TEXT                  DEFAULT NULL COMMENT '匹配结果详情（JSON）',
    `extract_result` TEXT                 DEFAULT NULL COMMENT '提取器结果（JSON）',
    `tenant_id`     BIGINT       NOT NULL DEFAULT 1 COMMENT '租户 ID',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by`     BIGINT                DEFAULT NULL COMMENT '创建人',
    `update_by`     BIGINT                DEFAULT NULL COMMENT '更新人',
    `deleted`       TINYINT(1)   NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_item_id` (`task_item_id`),
    KEY             `idx_task_id` (`task_id`),
    KEY             `idx_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='检测结果表';
