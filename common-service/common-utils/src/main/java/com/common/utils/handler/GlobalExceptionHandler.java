package com.common.utils.handler;

import com.common.utils.response.ApiException;
import com.common.utils.response.ApiResponse;
import com.common.utils.response.CommonErrorCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 * <p>
 * 使用 {@code @RestControllerAdvice} 统一拦截所有 Controller 层抛出的异常，
 * 将各种异常统一转换为 {@link ApiResponse} 格式的 JSON 返回，避免前端收到
 * Tomcat 默认的 HTML 错误页或非结构化响应。
 * <p>
 * <b>异常处理层次（从具体到通用）：</b>
 * <ol>
 *   <li>{@link ApiException} —— 业务层主动抛出的异常，直接使用异常自带的 ErrorCode 返回</li>
 *   <li>{@link MethodArgumentNotValidException} —— {@code @Valid} 校验失败，提取字段级错误信息并拼接</li>
 *   <li>{@link ConstraintViolationException} —— 方法参数上直接标注的约束校验失败（如 {@code @NotNull @RequestParam}）</li>
 *   <li>{@link MethodArgumentTypeMismatchException} —— 参数类型转换失败（如期望 Long 但传入 "abc"）</li>
 *   <li>{@link MissingServletRequestParameterException} —— 缺少必填的请求参数</li>
 *   <li>{@link HttpMessageNotReadableException} —— 请求体 JSON 格式错误无法解析</li>
 *   <li>{@link NoResourceFoundException} —— Spring Boot 4.x 中访问不存在的静态资源</li>
 *   <li>{@link Exception} —— 兜底处理器，捕获上述之外的所有未知异常，返回 500</li>
 * </ol>
 * <p>
 * <b>注意：</b> Spring 的 {@code @ExceptionHandler} 匹配规则是从具体异常到父类异常，
 * 因此处理器顺序不影响实际匹配结果，这里按业务语义排列仅为可读性。
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理业务异常 {@link ApiException}。
     * <p>
     * 这是项目中最常见的异常类型，由 Service 层通过
     * {@code throw new ApiException("xxx")} 或 {@code throw new ApiException(ErrorCode)} 抛出。
     * 直接使用异常内置的 ErrorCode 中的 code、httpCode、message 构造响应。
     */
    @ExceptionHandler(ApiException.class)
    public ApiResponse<Void> handleApiException(ApiException ex) {
        log.warn("业务异常 [{}] {} | {}", ex.getCode(), ex.getHttpCode(), ex.getMessage());
        return ApiResponse.failure(ex);
    }

    /**
     * 处理 {@code @Valid} 校验失败异常。
     * <p>
     * 当 Controller 方法参数标注了 {@code @Valid} 时，Spring 会自动校验 Request Body 中的约束注解
     * （如 {@code @NotBlank, @NotNull, @Size} 等），校验不通过时抛出此异常。
     * 这里遍历所有字段错误，拼接成 "field1: 错误信息; field2: 错误信息" 的格式返回给前端。
     * <p>
     * 注意：使用 {@code Stream.reduce} 替代 {@code Collectors.joining("; ")}，
     * 因为后者会先创建 StringBuilder 而 reduce 在单元素时更轻量。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        log.warn("参数校验失败: {}", message);
        return ApiResponse.failure(CommonErrorCode.PARAM_INVALID.getCode(), message, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理方法参数约束校验失败异常。
     * <p>
     * 与 {@link MethodArgumentNotValidException} 不同，此异常在方法参数直接标注校验注解时触发
     * （例如 {@code public void foo(@NotNull @RequestParam String name)}），
     * 而非通过 {@code @Valid} + Request Body 触发。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("约束校验失败: {}", ex.getMessage());
        return ApiResponse.failure(CommonErrorCode.PARAM_INVALID);
    }

    /**
     * 处理参数类型转换失败异常。
     * <p>
     * 例如接口定义为 {@code @RequestParam Long id}，但前端传了 "abc"，
     * Spring 无法将 "abc" 转为 Long 时抛出此异常。
     * 返回格式化为 "参数 'id' 类型错误，期望 Long" 的友好提示。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = String.format("参数 '%s' 类型错误，期望 %s",
                ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "Unknown");
        log.warn(message);
        return ApiResponse.failure(CommonErrorCode.PARAM_INVALID.getCode(), message, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理缺少必填请求参数异常。
     * <p>
     * 当接口有必填的 {@code @RequestParam} 但请求中未携带该参数时触发。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<Void> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("缺少必要参数: {}", ex.getParameterName());
        return ApiResponse.failure(CommonErrorCode.PARAM_INVALID.getCode(),
                "缺少必要参数: " + ex.getParameterName(), HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理请求体 JSON 解析失败异常。
     * <p>
     * 例如请求体为空、JSON 格式错误（多余的逗号、未闭合的花括号）等场景。
     * 返回 400 和通用提示，不暴露详细的解析错误信息给前端。
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败: {}", ex.getMessage());
        return ApiResponse.failure(CommonErrorCode.PARAM_INVALID.getCode(), "请求体格式错误", HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理 Spring Boot 4.x 静态资源 404 异常。
     * <p>
     * Spring Boot 4.x 中，请求不存在的静态资源（如 /favicon.ico）不再返回 null，
     * 而是抛出 {@link NoResourceFoundException}。如果不单独处理此异常，
     * 它会被兜底的 {@code Exception} 处理器捕获，导致返回 500 而非 404。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResponse<Void> handleNoResourceFound(NoResourceFoundException ex) {
        return ApiResponse.failure(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    /**
     * 兜底异常处理器：捕获所有未被上述 handler 明确处理的异常。
     * <p>
     * 通常包括 NPE、数据库连接失败、第三方服务调用异常等未知运行时异常。
     * 记录完整堆栈到日志，但对外只返回统一的 "服务器内部错误" 信息，不泄露内部细节。
     */
    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        log.error("未预期的异常", ex);
        return ApiResponse.internalServerError(CommonErrorCode.INTERNAL_ERROR.getCode(),
                CommonErrorCode.INTERNAL_ERROR.getMessage());
    }
}
