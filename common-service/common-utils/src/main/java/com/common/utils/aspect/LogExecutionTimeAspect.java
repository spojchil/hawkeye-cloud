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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 方法执行耗时监控切面
 * <p>
 * 通过 {@code @Around} 环绕通知拦截标注了 {@link LogExecutionTime} 的方法，
 * 在 finally 块中统一记录执行耗时、参数、返回值、异常等监控数据。
 * <p>
 * <b>设计要点：</b>
 * <ul>
 *   <li><b>异常不吞：</b> try-catch 后重新 throw，finally 块仅负责记录日志，不改变原有异常传播行为</li>
 *   <li><b>数据截断：</b> 对参数值和序列化后的返回值做长度限制，避免大对象（如文件字节流）撑爆日志</li>
 *   <li><b>分级输出：</b> 超过 slowThresholdMs 的成功请求用 WARN 级别，低于阈值的用 INFO，失败的用 ERROR</li>
 *   <li><b>全局开关：</b> 通过 {@code hawkeye.log.execution-time.enabled=false} 可关闭全部监控日志</li>
 *   <li><b>请求关联：</b> 自动从 RequestContext 中获取当前账号 ID 拼入日志，方便问题定位</li>
 * </ul>
 *
 * @see LogExecutionTime 对应的标记注解
 * @see RequestContext 请求上下文（提供 accountId 等请求头信息）
 */
@Aspect
@Component
@Slf4j
@ConditionalOnProperty(name = "hawkeye.log.execution-time.enabled", havingValue = "true", matchIfMissing = true)
public class LogExecutionTimeAspect {

    /** 返回值序列化后的 JSON 字符串最大长度，超出部分截断并追加 "...(truncated)" */
    private static final int MAX_SERIALIZE_LENGTH = 2048;
    /** 单个参数值（当参数为 String 类型时）的最大长度，超出部分截断 */
    private static final int MAX_PARAM_VALUE_LENGTH = 512;

    /**
     * 环绕通知：拦截标注了 {@link LogExecutionTime} 的方法并记录监控信息。
     * <p>
     * <b>执行流程：</b>
     * <ol>
     *   <li>记录开始时间戳 {@code startTime}</li>
     *   <li>通过 {@link MethodSignature} 获取类名、方法名、参数名列表</li>
     *   <li>调用 {@code joinPoint.proceed()} 执行目标方法（可能抛出异常）</li>
     *   <li>在 {@code finally} 块中计算耗时并组装日志消息，<b>无论成功失败都会执行</b></li>
     * </ol>
     * <p>
     * <b>异常处理策略：</b> catch 中标记 {@code success = false} 并记录异常对象，
     * 然后重新 throw，不吞异常，因此上游调用者仍能收到原始异常。
     *
     * @param joinPoint        切点连接点，封装了被拦截方法的所有反射信息
     * @param logExecutionTime 方法上标注的 {@link LogExecutionTime} 注解实例，用于读取各项配置
     * @return 目标方法的原始返回值，不做任何修改
     * @throws Throwable 目标方法抛出的原始异常，不做任何包装
     */
    @Around("@annotation(logExecutionTime)")
    public Object around(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 通过 MethodSignature 反射获取方法元信息（类名、方法名、参数名）
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        // 参数名列表（编译时需要 -parameters 参数保留，否则为 arg0, arg1...）
        String[] paramNames = signature.getParameterNames();
        // 参数值列表（通过 joinPoint 获取实际运行时传入的值）
        Object[] paramValues = joinPoint.getArgs();

        // 执行状态标记，用于 finally 块中判断日志级别
        boolean success = true;
        Object result = null;
        Throwable throwable = null;

        try {
            // 执行目标方法，正常返回
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            // 标记失败并记录异常对象（后续在 finally 块中记录日志）
            success = false;
            throwable = t;
            // 重新抛出，不吞异常
            throw t;
        } finally {
            // === 以下都在 finally 块中执行，保证无论成功失败都会输出日志 ===
            long costTime = System.currentTimeMillis() - startTime;

            // 展示名称：优先用注解自定义名，否则用 "类名.方法名"
            String displayName = logExecutionTime.value().isEmpty()
                    ? className + "." + methodName
                    : logExecutionTime.value();

            // 组装日志消息
            StringBuilder logMsg = new StringBuilder(256);
            logMsg.append("[Exec] ").append(displayName);
            logMsg.append(" | ").append(success ? "OK" : "FAIL");
            logMsg.append(" | ").append(costTime).append("ms");

            // 慢请求标记：超过注解指定的阈值时追加 [SLOW] 标识
            if (costTime >= logExecutionTime.slowThresholdMs()) {
                logMsg.append(" [SLOW]");
            }

            // 关联当前请求的账号 ID，方便日志追踪
            String accountId = RequestContext.getHeader(HeaderConstants.HEADER_ACCOUNT_ID);
            if (accountId != null) {
                logMsg.append(" | uid=").append(accountId);
            }

            // 入参日志（仅在注解开启且参数列表非空时打印）
            if (logExecutionTime.logArguments() && ArrayUtil.isNotEmpty(paramNames)) {
                Map<String, Object> paramMap = new HashMap<>();
                for (int i = 0; i < Math.min(paramNames.length, paramValues.length); i++) {
                    // 每个参数值先经过截断处理再放入 Map
                    paramMap.put(paramNames[i], truncateValue(paramValues[i]));
                }
                logMsg.append(" | args=").append(serializeSafe(paramMap));
            }

            // 返回值日志（仅在注解开启、方法有返回值且返回值非 null 时打印）
            // 注意：对序列化后的 JSON 字符串做长度截断，防止返回值过大
            if (logExecutionTime.logReturnValue()
                    && !signature.getReturnType().equals(Void.TYPE) && result != null) {
                logMsg.append(" | ret=").append(truncateString(serializeSafe(result), MAX_SERIALIZE_LENGTH));
            }

            // 异常日志（仅在注解开启且有异常对象时打印）
            if (logExecutionTime.logThrowable() && throwable != null) {
                logMsg.append(" | ex=").append(throwable.getClass().getSimpleName())
                        .append(": ").append(throwable.getMessage());
            }

            // 日志级别决策：
            // - 成功 + 慢请求 → WARN，便于日志平台告警
            // - 成功 + 正常 → INFO，常规监控
            // - 失败         → ERROR，必须关注
            if (success) {
                if (costTime >= logExecutionTime.slowThresholdMs()) {
                    log.warn(logMsg.toString());
                } else {
                    log.info(logMsg.toString());
                }
            } else {
                log.error(logMsg.toString());
            }
        }
    }

    /**
     * 安全序列化：将对象转为 JSON 字符串。
     * <p>
     * 使用 Fastjson2 的 {@link JSON#toJSONString(Object)} 进行序列化。
     * 若序列化过程中抛出异常（如循环引用、不可序列化对象），
     * 返回占位字符串 {@code "<serialization failed>"}，确保日志记录本身不会中断。
     *
     * @param obj 待序列化的对象，可为 null
     * @return JSON 字符串 或 "null" 或 "<serialization failed>"
     */
    private String serializeSafe(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return JSON.toJSONString(obj);
        } catch (Exception e) {
            return "<serialization failed>";
        }
    }

    /**
     * 参数值截断：对 String 类型的参数值做长度限制。
     * <p>
     * 如果参数值是 String 且长度超过 {@value #MAX_PARAM_VALUE_LENGTH}，
     * 截取前 N 个字符并追加 "...(truncated)" 后缀。
     * 非 String 类型的参数不做处理，直接原样返回。
     *
     * @param value 单个参数值，可能为 null
     * @return 截断后的值或原值
     */
    private Object truncateValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s && s.length() > MAX_PARAM_VALUE_LENGTH) {
            return s.substring(0, MAX_PARAM_VALUE_LENGTH) + "...(truncated)";
        }
        return value;
    }

    /**
     * 字符串截断：对序列化后的 JSON 字符串做长度限制。
     * <p>
     * 通常在调用 {@link #serializeSafe(Object)} 之后使用此方法。
     * 返回值可能是一个很长的 JSON，超过 {@code maxLen} 则截断。
     *
     * @param str    待截断的字符串
     * @param maxLen 最大允许长度
     * @return 截断后的字符串 或 "null"
     */
    private String truncateString(String str, int maxLen) {
        if (str == null) {
            return "null";
        }
        if (str.length() <= maxLen) {
            return str;
        }
        return str.substring(0, maxLen) + "...(truncated)";
    }
}
