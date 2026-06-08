package com.libseat.exception;

import com.libseat.common.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    void constructor_with_error_code_uses_default_message() {
        BusinessException ex = new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getMessage());
    }

    @Test
    void constructor_with_custom_message_overrides_default() {
        BusinessException ex = new BusinessException(ErrorCode.SEAT_UNAVAILABLE, "座位 3F-A-012 维修中");

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SEAT_UNAVAILABLE);
        assertThat(ex.getMessage()).isEqualTo("座位 3F-A-012 维修中");
    }

    @Test
    void is_runtime_exception() {
        BusinessException ex = new BusinessException(ErrorCode.INTERNAL_ERROR);

        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
