package com.common.utils.aspect;

import cn.hutool.core.util.ArrayUtil;
import com.alibaba.fastjson2.JSON;
import com.common.utils.annotation.LogExecutionTime;
import com.common.utils.constant.HeaderConstants;
import com.common.utils.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 方法执行耗时监控切面
 * 正常→DEBUG，慢方法→WARN，异常捕获后 WARN 级别记录上下文再重新抛出。
 */
@Aspect
@Component
@Slf4j
@ConditionalOnProperty(name = "hawkeye.log.execution-time.enabled", havingValue = "true", matchIfMissing = true)
public class LogExecutionTimeAspect {

    /** 慢方法阈值，可通过 hawkeye.log.execution-time.slow-threshold-ms 配置 */
    @Value("${hawkeye.log.execution-time.slow-threshold-ms:1000}")
    private long slowThresholdMs;

    /** 序列化最大长度，防大对象撑爆日志，可通过 hawkeye.log.execution-time.max-serialize-length 配置 */
    @Value("${hawkeye.log.execution-time.max-serialize-length:2048}")
    private int maxSerializeLength;

    /** 参数值最大长度 */
    @Value("${hawkeye.log.execution-time.max-param-length:512}")
    private int maxParamLength;

    @Around("@annotation(logExecutionTime)")
    public Object around(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime) throws Throwable {
        long startTime = System.currentTimeMillis();

        /* 反射获取方法元信息 */
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String[] paramNames = signature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();

        boolean success = true;
        Object result = null;
        Throwable throwable = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            success = false;
            throwable = t;
            throw t;
        } finally {
            long costTime = System.currentTimeMillis() - startTime;

            String displayName = logExecutionTime.value().isEmpty()
                    ? className + "." + methodName
                    : logExecutionTime.value();

            StringBuilder logMsg = new StringBuilder(128);
            logMsg.append("[Exec] ").append(displayName)
                    .append(" | ").append(success ? "OK" : "FAIL")
                    .append(" | ").append(costTime).append("ms");

            /* 慢请求标记 */
            if (costTime >= slowThresholdMs) {
                logMsg.append(" [SLOW]");
            }

            /* 关联当前用户 */
            String username = RequestContext.getHeader(HeaderConstants.HEADER_USERNAME);
            if (username != null) {
                logMsg.append(" | user=").append(username);
            }

            /* 入参 */
            if (logExecutionTime.logArguments() && ArrayUtil.isNotEmpty(paramNames)) {
                Map<String, Object> paramMap = new HashMap<>();
                for (int i = 0; i < Math.min(paramNames.length, paramValues.length); i++) {
                    paramMap.put(paramNames[i], truncateValue(paramValues[i]));
                }
                logMsg.append(" | args=").append(serializeSafe(paramMap));
            }

            /* 返回值 */
            if (logExecutionTime.logReturnValue()
                    && !signature.getReturnType().equals(Void.TYPE) && result != null) {
                logMsg.append(" | ret=").append(truncateStr(serializeSafe(result), maxSerializeLength));
            }

            /* 异常上下文——帮助排查，但不取代 GlobalExceptionHandler */
            if (!success) {
                logMsg.append(" | ex=").append(throwable.getClass().getSimpleName())
                        .append(": ").append(throwable.getMessage());
                log.warn(logMsg.toString());
            } else if (costTime >= slowThresholdMs) {
                log.warn(logMsg.toString());
            } else {
                log.debug(logMsg.toString());
            }
        }
    }

    private String serializeSafe(Object obj) {
        if (obj == null) return "null";
        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            return "<serialization failed>";
        }
    }

    /* 对 String 参数值做长度限制，防大字段撑爆日志 */
    private Object truncateValue(Object value) {
        if (value instanceof String s && s.length() > maxParamLength) {
            return s.substring(0, maxParamLength) + "...";
        }
        return value;
    }

    private String truncateStr(String str, int maxLen) {
        if (str == null) return "null";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen) + "...";
    }
}
