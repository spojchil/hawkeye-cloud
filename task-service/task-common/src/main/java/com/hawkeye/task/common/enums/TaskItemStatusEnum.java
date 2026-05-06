package com.hawkeye.task.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 检测项状态枚举。
 * <p>
 * 值必须与 DDL 注释一致：0=待执行, 1=匹配, 2=未匹配, 3=失败, 4=跳过
 */
@Getter
public enum TaskItemStatusEnum implements IEnum<Integer> {

    PENDING(0, "待执行"),
    MATCHED(1, "匹配"),
    NOT_MATCHED(2, "未匹配"),
    FAILED(3, "失败"),
    SKIPPED(4, "跳过");

    @EnumValue
    private final Integer value;

    @JsonValue
    private final String description;

    TaskItemStatusEnum(Integer value, String description) {
        this.value = value;
        this.description = description;
    }
}
