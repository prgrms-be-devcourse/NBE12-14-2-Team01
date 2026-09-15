package com.merge.backend.global.exception;

import com.merge.backend.global.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 예상 가능한 비즈니스 예외 처리.
     *
     * 예:
     * - 잘못된 로그인
     * - 이미 존재하는 Schedule
     * - Shift 시간 충돌
     * - 잘못된 Request 상태
     *
     * Service에서는 HTTP Response를 직접 만들지 않고
     * BusinessException을 던진다.
     *
     * 여기서 ErrorCode에 정의된
     * HttpStatus / code / message를 이용해
     * 공통 ApiResponse 형태로 변환한다.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
        BusinessException e
    ) {
        ErrorCode errorCode = e.getErrorCode();

        log.warn(
            "BusinessException: [{}] {}",
            errorCode.getCode(),
            errorCode.getMessage()
        );

        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(
                ApiResponse.error(
                    errorCode.getCode(),
                    errorCode.getMessage()
                )
            );
    }


    /**
     * @Valid 검증 실패 처리.
     *
     * DTO의 @NotBlank, @NotNull, @Size 등
     * Bean Validation에 실패하면
     * MethodArgumentNotValidException이 발생한다.
     *
     * P0에서는 여러 필드 오류 중
     * 첫 번째 오류 메시지만 반환한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
        MethodArgumentNotValidException e
    ) {
        ErrorCode errorCode =
            GlobalErrorCode.VALIDATION_ERROR;

        String message =
            e.getBindingResult().getFieldError() != null
                ? e.getBindingResult()
                  .getFieldError()
                  .getDefaultMessage()
                : errorCode.getMessage();

        log.warn(
            "ValidationException: [{}] {}",
            errorCode.getCode(),
            message
        );

        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(
                ApiResponse.error(
                    errorCode.getCode(),
                    message
                )
            );
    }


    /**
     * JSON Body 파싱 실패 처리.
     *
     * 예:
     * - 잘못된 JSON 문법
     * - Enum에 존재하지 않는 값
     * - 타입이 맞지 않는 값
     *
     * 이런 오류는 서버 오류가 아니라
     * Client의 잘못된 요청이므로 400으로 처리한다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>>
    handleHttpMessageNotReadableException(
        HttpMessageNotReadableException e
    ) {
        ErrorCode errorCode =
            GlobalErrorCode.INVALID_REQUEST_BODY;

        log.warn(
            "HttpMessageNotReadableException: [{}] {}",
            errorCode.getCode(),
            e.getMessage()
        );

        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(
                ApiResponse.error(
                    errorCode.getCode(),
                    errorCode.getMessage()
                )
            );
    }


    /**
     * 위에서 처리되지 않은 모든 예상하지 못한 예외 처리.
     *
     * NullPointerException, DB 오류, 코드 버그 등은
     * Client에게 내부 내용을 노출하지 않는다.
     *
     * 실제 Exception과 StackTrace는 서버 로그에 남기고
     * Client에게는 고정된 500 응답만 전달한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
        Exception e
    ) {
        ErrorCode errorCode =
            GlobalErrorCode.INTERNAL_SERVER_ERROR;

        log.error(
            "Unhandled Exception 발생",
            e
        );

        return ResponseEntity
            .status(errorCode.getHttpStatus())
            .body(
                ApiResponse.error(
                    errorCode.getCode(),
                    errorCode.getMessage()
                )
            );
    }
}
