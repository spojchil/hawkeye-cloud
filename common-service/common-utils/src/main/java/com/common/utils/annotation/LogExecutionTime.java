package com.common.utils.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解，供日志使用
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogExecutionTime {

    // 方法描述，默认为方法名
    String value() default "";

    // 是否打印入参，默认不打印
    boolean logArguments() default false;

    // 是否打印返回值，默认不打印
    boolean logReturnValue() default false;

    // 是否打印异常栈，默认不打印
    boolean logThrowable() default false;
}
