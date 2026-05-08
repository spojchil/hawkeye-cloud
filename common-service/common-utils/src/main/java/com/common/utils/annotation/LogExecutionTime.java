package com.common.utils.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记方法以自动记录执行耗时
 * 正常→DEBUG，慢→WARN，异常→WARN 记录上下文后重新抛出。
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogExecutionTime {

    /** 展示名称（不填则用 "类名.方法名"） */
    String value() default "";

    /** 是否打印入参 */
    boolean logArguments() default false;

    /** 是否打印返回值 */
    boolean logReturnValue() default false;
}
