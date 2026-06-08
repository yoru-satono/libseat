package com.libseat.exception;

import com.libseat.common.ErrorCode;
import com.libseat.common.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

// Spring Security 的 AuthenticationException(401) 和 AccessDeniedException(403)
// 在过滤器层抛出，不经过此处——由 SecurityConfig 中的
// AuthenticationEntryPoint 和 AccessDeniedHandler 统一返回 Result 格式。
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：直接取 errorCode 和 message 返回 */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        return Result.fail(e.getErrorCode(), e.getMessage());
    }

    /** @Valid 注解触发的请求体校验失败，拼接所有字段错误描述 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.fail(ErrorCode.PARAM_INVALID, message);
    }

    /** @Validated 注解触发的方法参数（如路径变量、查询参数）校验失败 */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<?> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("; "));
        return Result.fail(ErrorCode.PARAM_INVALID, message);
    }

    /** 请求体 JSON 格式错误或无法反序列化 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.fail(ErrorCode.PARAM_FORMAT_ERROR);
    }

    /** 路径变量或请求参数类型不匹配，如传入字符串却期望 UUID */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.fail(ErrorCode.PARAM_FORMAT_ERROR);
    }

    /** 请求路径不存在 */
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<?> handleNoResource(NoResourceFoundException e) {
        return Result.fail(ErrorCode.RESOURCE_NOT_FOUND);
    }

    /** 兜底：记录完整堆栈，返回服务端错误，不向客户端泄露内部信息 */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("Unexpected error", e);
        return Result.fail(ErrorCode.INTERNAL_ERROR);
    }
}
