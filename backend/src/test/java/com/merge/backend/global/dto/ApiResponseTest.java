package com.merge.backend.global.dto;


import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiResponseTest {

    @Test
    void 데이터가_있는_성공_응답을_생성한다() {
        // given
        String data = "test-data";

        // when
        ApiResponse<String> response =
            ApiResponse.success(
                "TEST-001",
                "성공했습니다.",
                data
            );

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo("TEST-001");
        assertThat(response.getMessage()).isEqualTo("성공했습니다.");
        assertThat(response.getData()).isEqualTo(data);
    }

    @Test
    void 데이터가_없는_성공_응답을_생성한다() {
        // when
        ApiResponse<Void> response =
            ApiResponse.success(
                "TEST-001",
                "성공했습니다."
            );

        // then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getCode()).isEqualTo("TEST-001");
        assertThat(response.getMessage()).isEqualTo("성공했습니다.");
        assertThat(response.getData()).isNull();
    }

    @Test
    void 데이터가_있는_에러_응답을_생성한다() {
        // given
        String data = "error-data";

        // when
        ApiResponse<String> response =
            ApiResponse.error(
                "TEST-ERROR",
                "실패했습니다.",
                data
            );

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("TEST-ERROR");
        assertThat(response.getMessage()).isEqualTo("실패했습니다.");
        assertThat(response.getData()).isEqualTo(data);
    }

    @Test
    void 데이터가_없는_에러_응답을_생성한다() {
        // when
        ApiResponse<Void> response =
            ApiResponse.error(
                "TEST-ERROR",
                "실패했습니다."
            );

        // then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("TEST-ERROR");
        assertThat(response.getMessage()).isEqualTo("실패했습니다.");
        assertThat(response.getData()).isNull();
    }

}