package com.common.utils.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法以便 {@link com.common.utils.aspect.LogExecutionTimeAspect} 自动记录执行耗时日志。
 * <p>
 * 将此注解标注在需要监控的方法上，切面会自动在方法执行前后记录：
 * <ul>
 *   <li>方法名称（可自定义显示名）</li>
 *   <li>执行状态（成功/失败）</li>
 *   <li>执行耗时（毫秒）</li>
 *   <li>可选：入参、返回值、异常信息</li>
 *   <li>可选：关联当前请求的账号 ID</li>
 * </ul>
 * <p>
 * <b>使用示例：</b>
 * <pre>{@code
 * @LogExecutionTime(value = "查询用户资产", logArguments = true, slowThresholdMs = 500)
 * public List<Asset> getUserAssets(Long userId) { ... }
 * }</pre>
 *
 * @see com.common.utils.aspect.LogExecutionTimeAspect
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogExecutionTime {

    /**
     * 自定义方法展示名称。
     * 不填则默认使用 "类名.方法名" 格式。
     * 建议填写业务语义名称，方便在日志中快速定位。
     */
    String value() default "";

    /**
     * 是否在日志中打印方法入参。
     * 默认关闭，因为入参可能包含敏感信息或大对象。
     * 开启后，参数值会经过截断处理（String 类型超过 512 字符截断）。
     */
    boolean logArguments() default false;

    /**
     * 是否在日志中打印方法返回值。
     * 默认关闭，因为返回值可能包含大量数据。
     * 开启后，序列化后的 JSON 超过 2048 字符会截断。
     */
    boolean logReturnValue() default false;

    /**
     * 是否在日志中打印异常信息。
     * 默认开启，仅打印异常类名和 message，不打印完整堆栈。
     */
    boolean logThrowable() default true;

    /**
     * 慢请求阈值（毫秒）。
     * 超过此阈值的方法会用 WARN 级别输出日志并在条目中追加 [SLOW] 标记，
     * 方便在日志平台配置告警规则。
     * 默认 1000ms。
     */
    long slowThresholdMs() default 1000;
}
