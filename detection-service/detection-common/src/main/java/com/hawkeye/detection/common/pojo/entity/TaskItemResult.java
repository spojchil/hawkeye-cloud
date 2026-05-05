package com.hawkeye.detection.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 检测项结果实体（对应 task_item 表）。
 * <p>
 * detection-service 用于更新检测结果。
 */
@Data
@TableName("task_item")
public class TaskItemResult {

    @TableId(type = IdType.AUTO)
    private Long itemId;

    /** 状态: 0=待执行, 1=匹配, 2=未匹配, 3=失败, 4=跳过 */
    private Integer status;

    /** HTTP 响应状态码 */
    private Integer responseStatusCode;

    /** 响应体大小(字节) */
    private Integer responseSize;

    /** 响应摘要 */
    private String responseSummary;

    /** 命中的匹配器名称 */
    private String matchedMatcher;

    /** 匹配时间 */
    private LocalDateTime matchedAt;

    /** 错误信息 */
    private String errorMessage;

    /** HTTP请求耗时(毫秒) */
    private Integer durationMs;
}
