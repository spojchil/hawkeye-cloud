-- ============================================================
-- Hawkeye Cloud — task-service 迁移脚本
-- 版本：v3.0 → v4.0（合并 task_item 和 detection_result）
-- ============================================================
-- 执行前请备份数据库！

USE hawkeye;

-- 1. 添加新字段到 task_item 表
ALTER TABLE task_item
ADD COLUMN response_status_code INT COMMENT 'HTTP 响应状态码' AFTER status,
ADD COLUMN response_size INT COMMENT '响应体大小(字节)' AFTER response_status_code,
ADD COLUMN response_summary VARCHAR(2048) COMMENT '响应摘要' AFTER response_size,
ADD COLUMN matched_matcher VARCHAR(255) COMMENT '命中的匹配器名称' AFTER response_summary,
ADD COLUMN matched_at DATETIME COMMENT '匹配时间' AFTER matched_matcher,
ADD COLUMN error_message VARCHAR(2048) COMMENT '错误信息' AFTER matched_at,
ADD COLUMN duration_ms INT COMMENT 'HTTP请求耗时(毫秒)' AFTER error_message;

-- 2. 迁移 detection_result 数据到 task_item（如果需要保留历史数据）
-- UPDATE task_item ti
-- JOIN detection_result dr ON ti.item_id = dr.task_item_id
-- SET ti.response_status_code = dr.response_status_code,
--     ti.response_size = dr.response_size,
--     ti.response_summary = dr.response_summary,
--     ti.matched_matcher = dr.matched_matcher,
--     ti.matched_at = dr.matched_at,
--     ti.error_message = dr.error_message,
--     ti.duration_ms = dr.duration_ms,
--     ti.status = CASE dr.status
--         WHEN 'matched' THEN 1
--         WHEN 'not_matched' THEN 2
--         WHEN 'error' THEN 3
--         ELSE ti.status
--     END;

-- 3. 删除旧的 result 字段
ALTER TABLE task_item DROP COLUMN result;

-- 4. 删除 detection_result 表（确认不再需要后执行）
-- DROP TABLE IF EXISTS detection_result;
