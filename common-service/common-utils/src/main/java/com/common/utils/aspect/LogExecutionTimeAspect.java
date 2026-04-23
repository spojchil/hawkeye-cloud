package com.common.utils.aspect;

import cn.hutool.core.util.ArrayUtil;
import com.alibaba.fastjson2.JSON;
import com.common.utils.annotation.LogExecutionTime;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
@Slf4j
public class LogExecutionTimeAspect {

    @Around("@annotation(logExecutionTime)")
    public Object around(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime) throws Throwable {
        long startTime = System.currentTimeMillis();  // 记录开始时间戳
        // 获取方法名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        // 获取参数名
        String[] paramNames = signature.getParameterNames();
        // 获取参数值
        Object[] paramValues = joinPoint.getArgs();

        boolean success = true;
        Object result = null;
        Throwable throwable = null;

        try {
            // 执行目标方法
            result = joinPoint.proceed();
            return result;  // 正常返回
        } catch (Throwable t) {
            success = false;  // 标记异常
            throwable = t; // 记录异常
            throw t;  // 抛出异常，不影响业务原有逻辑
        } finally {
            // 计算耗时
            long costTime = System.currentTimeMillis() - startTime;
            // 动态构建日志信息
            StringBuilder logMsg = new StringBuilder();

            // 方法名称，注解里填了value就用自定义名，没填就用默认方法名
            String methodName = logExecutionTime.value().isEmpty()
                    ? signature.toShortString()
                    : logExecutionTime.value();

            // TODO 无请求ID，线程ID，类全路径名，请求ID
            // 拼接基础信息
            logMsg.append("【方法执行监控】").append(methodName);
            logMsg.append(" | 执行状态：").append(success ? "成功" : "失败");

            // TODO 无时间阈值
            logMsg.append(" | 耗时：").append(costTime).append("ms");

            // TODO 未脱敏，未考虑大对象
            // 入参打印，如果启用了
            if (logExecutionTime.logArguments()) {
                Map<String, Object> paramMap = new HashMap<>();
                if (ArrayUtil.isNotEmpty(paramNames) && ArrayUtil.isNotEmpty(paramValues)) {
                    for (int i = 0; i < paramNames.length; i++) {
                        paramMap.put(paramNames[i], paramValues[i]);
                    }
                }
                logMsg.append(" | 入参：").append(serializeSafe(paramMap));
            }

            // TODO 未脱敏，未考虑大对象，无堆栈信息
            // 返回值打印
            if (logExecutionTime.logReturnValue() && !signature.getReturnType().equals(Void.TYPE)) {
                logMsg.append(" | 返回值：").append(serializeSafe(result));
            }

            // 异常信息打印
            if (logExecutionTime.logThrowable() && throwable != null) {
                logMsg.append(" | 异常信息：").append(throwable.getMessage());
            }

            // TODO 非异步日志，无全局开关，非结构化日志
            // 日志级别区分
            if (success) {
                log.info(logMsg.toString());
            } else {
                log.error(logMsg.toString());
            }
        }
    }

    /**
     * 安全序列化
     */
    private String serializeSafe(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            return "序列化失败";
        }
    }
}