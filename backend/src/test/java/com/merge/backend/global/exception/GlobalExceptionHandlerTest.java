package com.merge.backend.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.merge.backend.global.dto.ApiResponse;
import com.merge.backend.global.dto.ConfirmationRequiredResponse;
import com.merge.backend.global.dto.ValidationErrorResponse;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
        new GlobalExceptionHandler();

    @Test
    void BusinessException을_ErrorCode에_맞는_응답으로_변환한다() {
        // given
        BusinessException exception =
            new BusinessException(
                GlobalErrorCode.INVALID_REQUEST_BODY
            );

        // when
        ResponseEntity<ApiResponse<Object>> response =
            handler.handleBusinessException(exception);

        // then
        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        ApiResponse<Object> body = response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getCode()).isEqualTo("COM-002");
        assertThat(body.getMessage())
            .isEqualTo("요청 형식이 올바르지 않습니다.");
        assertThat(body.getData()).isNull();
    }

    @Test
    void BusinessException의_data를_응답_data로_전달한다() {
        // given
        ConfirmationRequiredResponse data =
            new ConfirmationRequiredResponse(true);

        BusinessException exception =
            new BusinessException(
                GlobalErrorCode.INVALID_REQUEST_BODY,
                data
            );

        // when
        ResponseEntity<ApiResponse<Object>> response =
            handler.handleBusinessException(exception);

        // then
        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        ApiResponse<Object> body =
            response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getCode())
            .isEqualTo("COM-002");
        assertThat(body.getData())
            .isEqualTo(data);
    }

    @Test
    void 읽을_수_없는_RequestBody를_잘못된_요청_응답으로_변환한다() {
        // given
        HttpMessageNotReadableException exception =
            new HttpMessageNotReadableException(
                "JSON parse error",
                new MockHttpInputMessage(new byte[0])
            );

        // when
        ResponseEntity<ApiResponse<Void>> response =
            handler.handleHttpMessageNotReadableException(exception);

        // then
        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        ApiResponse<Void> body = response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getCode()).isEqualTo("COM-002");
        assertThat(body.getMessage())
            .isEqualTo("요청 형식이 올바르지 않습니다.");
        assertThat(body.getData()).isNull();
    }

    @Test
    void 예상하지_못한_예외를_서버_내부_오류_응답으로_변환한다() {
        // given
        Exception exception =
            new RuntimeException("내부 상세 오류");

        // when
        ResponseEntity<ApiResponse<Void>> response =
            handler.handleException(exception);

        // then
        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        ApiResponse<Void> body = response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getCode()).isEqualTo("COM-500");
        assertThat(body.getMessage())
            .isEqualTo("서버 내부 오류가 발생했습니다.");
        assertThat(body.getData()).isNull();
    }

    @Test
    void 여러_Validation_오류를_필드별로_구조화해서_반환한다()
        throws NoSuchMethodException {

        // given
        BeanPropertyBindingResult bindingResult =
            new BeanPropertyBindingResult(
                new Object(),
                "request"
            );

        bindingResult.addError(
            new FieldError(
                "request",
                "email",
                "이메일을 입력해주세요."
            )
        );

        bindingResult.addError(
            new FieldError(
                "request",
                "password",
                "비밀번호는 8자 이상이어야 합니다."
            )
        );

        Method method =
            GlobalExceptionHandlerTest.class
                .getDeclaredMethod(
                    "dummyControllerMethod",
                    Object.class
                );

        MethodParameter methodParameter =
            new MethodParameter(method, 0);

        MethodArgumentNotValidException exception =
            new MethodArgumentNotValidException(
                methodParameter,
                bindingResult
            );

        // when
        ResponseEntity<ApiResponse<ValidationErrorResponse>> response =
            handler.handleMethodArgumentNotValidException(exception);

        // then
        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        ApiResponse<ValidationErrorResponse> body =
            response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getCode()).isEqualTo("COM-001");
        assertThat(body.getMessage())
            .isEqualTo("요청 값이 올바르지 않습니다.");

        ValidationErrorResponse data = body.getData();

        assertThat(data).isNotNull();

        assertThat(data.fieldErrors())
            .containsExactlyInAnyOrder(
                new ValidationErrorResponse.FieldErrorDetail(
                    "email",
                    "이메일을 입력해주세요."
                ),
                new ValidationErrorResponse.FieldErrorDetail(
                    "password",
                    "비밀번호는 8자 이상이어야 합니다."
                )
            );
    }

    @Test
    void 요청_파라미터_타입_변환_실패를_잘못된_요청_응답으로_변환한다()
        throws NoSuchMethodException {

        // given
        Method method =
            GlobalExceptionHandlerTest.class
                .getDeclaredMethod(
                    "dummyLongParameterMethod",
                    Long.class
                );

        MethodParameter methodParameter =
            new MethodParameter(method, 0);

        MethodArgumentTypeMismatchException exception =
            new MethodArgumentTypeMismatchException(
                "abc",
                Long.class,
                "shiftId",
                methodParameter,
                new NumberFormatException("For input string: \"abc\"")
            );

        // when
        ResponseEntity<ApiResponse<Void>> response =
            handler.handleMethodArgumentTypeMismatchException(exception);

        // then
        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        ApiResponse<Void> body = response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getCode()).isEqualTo("COM-003");
        assertThat(body.getMessage())
            .isEqualTo("요청 파라미터가 올바르지 않습니다.");
        assertThat(body.getData()).isNull();
    }

    @Test
    void 필수_요청_파라미터_누락을_잘못된_요청_응답으로_변환한다() {
        // given
        MissingServletRequestParameterException exception =
            new MissingServletRequestParameterException(
                "weekStartDate",
                "LocalDate"
            );

        // when
        ResponseEntity<ApiResponse<Void>> response =
            handler.handleMissingServletRequestParameterException(exception);

        // then
        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.BAD_REQUEST);

        ApiResponse<Void> body = response.getBody();

        assertThat(body).isNotNull();
        assertThat(body.isSuccess()).isFalse();
        assertThat(body.getCode()).isEqualTo("COM-003");
        assertThat(body.getMessage())
            .isEqualTo("요청 파라미터가 올바르지 않습니다.");
        assertThat(body.getData()).isNull();
    }

    private void dummyControllerMethod(Object request) {
    }

    private void dummyLongParameterMethod(Long shiftId) {
    }

}