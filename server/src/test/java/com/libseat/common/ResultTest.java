package com.libseat.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResultTest {

    @Test
    void success_with_data_returns_ok_code_and_data() {
        Result<String> result = Result.success("hello");

        assertThat(result.getCode()).isEqualTo("00000");
        assertThat(result.getMessage()).isEqualTo("success");
        assertThat(result.getData()).isEqualTo("hello");
        assertThat(result.getTimestamp()).isNotBlank();
    }

    @Test
    void success_without_data_has_null_data() {
        Result<Void> result = Result.success();

        assertThat(result.getCode()).isEqualTo("00000");
        assertThat(result.getData()).isNull();
    }

    @Test
    void fail_with_error_code_uses_default_message() {
        Result<?> result = Result.fail(ErrorCode.RESOURCE_NOT_FOUND);

        assertThat(result.getCode()).isEqualTo("A0410");
        assertThat(result.getMessage()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getMessage());
        assertThat(result.getData()).isNull();
    }

    @Test
    void fail_with_custom_message_overrides_default() {
        Result<?> result = Result.fail(ErrorCode.PARAM_INVALID, "email 格式不正确");

        assertThat(result.getCode()).isEqualTo("A0400");
        assertThat(result.getMessage()).isEqualTo("email 格式不正确");
    }

    @Test
    void different_calls_produce_independent_instances() {
        Result<String> r1 = Result.success("a");
        Result<String> r2 = Result.success("b");

        assertThat(r1).isNotSameAs(r2);
        assertThat(r1.getData()).isEqualTo("a");
        assertThat(r2.getData()).isEqualTo("b");
    }
}
