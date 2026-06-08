package com.libseat.exception;

import com.libseat.common.ErrorCode;
import com.libseat.common.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleBusiness_returns_error_code_and_custom_message() {
        BusinessException ex = new BusinessException(ErrorCode.SEAT_UNAVAILABLE, "座位 3F-A-012 维修中");

        Result<?> result = handler.handleBusiness(ex);

        assertThat(result.getCode()).isEqualTo("B0100");
        assertThat(result.getMessage()).isEqualTo("座位 3F-A-012 维修中");
        assertThat(result.getData()).isNull();
    }

    @Test
    void handleBusiness_uses_default_message_when_no_custom_message() {
        BusinessException ex = new BusinessException(ErrorCode.RESERVATION_CONFLICT);

        Result<?> result = handler.handleBusiness(ex);

        assertThat(result.getCode()).isEqualTo("B0200");
        assertThat(result.getMessage()).isEqualTo(ErrorCode.RESERVATION_CONFLICT.getMessage());
    }

    @Test
    void handleValidation_joins_all_field_errors() {
        FieldError e1 = new FieldError("req", "email", "邮箱格式不正确");
        FieldError e2 = new FieldError("req", "password", "密码长度不足");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(e1, e2));
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        Result<?> result = handler.handleValidation(ex);

        assertThat(result.getCode()).isEqualTo("A0400");
        assertThat(result.getMessage()).contains("邮箱格式不正确").contains("密码长度不足");
    }

    @Test
    void handleConstraintViolation_includes_field_and_message() {
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("startTime");
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("不能为空");
        ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

        Result<?> result = handler.handleConstraintViolation(ex);

        assertThat(result.getCode()).isEqualTo("A0400");
        assertThat(result.getMessage()).contains("startTime").contains("不能为空");
    }

    @Test
    void handleNotReadable_returns_format_error() {
        Result<?> result = handler.handleNotReadable(mock(HttpMessageNotReadableException.class));

        assertThat(result.getCode()).isEqualTo("A0402");
    }

    @Test
    void handleTypeMismatch_returns_format_error() {
        Result<?> result = handler.handleTypeMismatch(mock(MethodArgumentTypeMismatchException.class));

        assertThat(result.getCode()).isEqualTo("A0402");
    }

    @Test
    void handleNoResource_returns_not_found() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/unknown/path", "No static resource unknown/path.");

        Result<?> result = handler.handleNoResource(ex);

        assertThat(result.getCode()).isEqualTo("A0410");
    }

    @Test
    void handleException_returns_internal_error_without_exposing_detail() {
        Result<?> result = handler.handleException(new RuntimeException("database connection lost"));

        assertThat(result.getCode()).isEqualTo("C0001");
        assertThat(result.getMessage()).doesNotContain("database connection lost");
    }
}
