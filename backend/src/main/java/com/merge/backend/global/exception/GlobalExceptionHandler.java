package com.merge.backend.global.exception;

import com.merge.backend.global.dto.ApiResponse;
import com.merge.backend.global.dto.ValidationErrorResponse;
import com.merge.backend.global.dto.ValidationErrorResponse.FieldErrorDetail;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>>
        handleBusinessException(BusinessException e)
    {
        ErrorCode errorCode = e.getErrorCode();

        log.warn("BusinessException: [{}] {}",
            errorCode.getCode(),
            errorCode.getMessage()
        );

        return ResponseEntity.status(errorCode.getHttpStatus())
            .body(ApiResponse.error(
                errorCode.getCode(),
                errorCode.getMessage(),
                e.getData()
            ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>>
        handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e)
    {
        ErrorCode errorCode = GlobalErrorCode.INVALID_REQUEST_BODY;

        log.warn("HttpMessageNotReadableException: [{}] {}",
            errorCode.getCode(),
            e.getMessage()
        );

        return ResponseEntity.status(errorCode.getHttpStatus())
            .body(ApiResponse.error(
                errorCode.getCode(),
                errorCode.getMessage()
            ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ValidationErrorResponse>>
        handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e)
    {
        ErrorCode errorCode = GlobalErrorCode.VALIDATION_ERROR;

        List<FieldErrorDetail> fieldErrors = e.getBindingResult().
            getFieldErrors().
            stream()
            .map(error -> new FieldErrorDetail(
                    error.getField(),
                    error.getDefaultMessage()
                )
            )
            .toList();

        ValidationErrorResponse data = new ValidationErrorResponse(fieldErrors);

        log.warn("MethodArgumentNotValidException: [{}] fieldErrors={}",
            errorCode.getCode(),
            fieldErrors
        );

        return ResponseEntity.status(errorCode.getHttpStatus())
            .body(ApiResponse.error(
                errorCode.getCode(),
                errorCode.getMessage(),
                data
            ));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>>
        handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e
    ) {
        ErrorCode errorCode = GlobalErrorCode.INVALID_REQUEST_PARAMETER;

        log.warn("MethodArgumentTypeMismatchException: [{}] parameter={}", errorCode.getCode(),
            e.getName());

        return ResponseEntity.status(errorCode.getHttpStatus())
            .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>>
        handleMissingServletRequestParameterException(
            MissingServletRequestParameterException e
    ) {
        ErrorCode errorCode = GlobalErrorCode.INVALID_REQUEST_PARAMETER;

        log.warn("MissingServletRequestParameterException: [{}] parameter={}", errorCode.getCode(),
            e.getParameterName());

        return ResponseEntity.status(errorCode.getHttpStatus())
            .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        ErrorCode errorCode = GlobalErrorCode.INTERNAL_SERVER_ERROR;

        log.error("Unhandled Exception 발생", e);

        return ResponseEntity.status(errorCode.getHttpStatus())
            .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

}