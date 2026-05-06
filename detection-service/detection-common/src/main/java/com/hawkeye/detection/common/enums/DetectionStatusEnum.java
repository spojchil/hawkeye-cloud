package com.hawkeye.detection.common.enums;

import lombok.Getter;

/**
 * 检测结果状态枚举。
 * <p>
 * 数据库存 VARCHAR，值域：matched / not_matched / error
 * <p>
 * 状态说明：
 * <ul>
 *   <li>MATCHED - 命中漏洞（检测成功且匹配到特征）</li>
 *   <li>NOT_MATCHED - 未命中漏洞（检测成功但未匹配到特征）</li>
 *   <li>ERROR - 执行异常（网络超时、解析错误等）</li>
 * </ul>
 * <p>
 * 使用示例：
 * <pre>
 *   result.setStatus(DetectionStatusEnum.MATCHED.getValue());
 *   // 或
 *   DetectionStatusEnum status = DetectionStatusEnum.fromValue("matched");
 * </pre>
 */
@Getter
public enum DetectionStatusEnum {

    /** 命中漏洞 */
    MATCHED("matched", "命中"),

    /** 未命中漏洞 */
    NOT_MATCHED("not_matched", "未命中"),

    /** 执行异常 */
    ERROR("error", "错误");

    /** 数据库存储值 */
    private final String value;

    /** 中文描述 */
    private final String description;

    DetectionStatusEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * 根据 value 查找枚举。
     *
     * @param value 存储值（matched/not_matched/error）
     * @return 对应的枚举
     * @throws IllegalArgumentException 如果 value 不存在
     */
    public static DetectionStatusEnum fromValue(String value) {
        for (DetectionStatusEnum status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的检测状态: " + value);
    }
}
