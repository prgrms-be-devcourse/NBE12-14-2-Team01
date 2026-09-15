package com.merge.backend.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GlobalErrorCode implements ErrorCode {

    VALIDATION_ERROR(
        HttpStatus.BAD_REQUEST,
        "COM-001",
        "요청 값이 올바르지 않습니다."
    ),

    INVALID_REQUEST_BODY(
        HttpStatus.BAD_REQUEST,
        "COM-002",
        "요청 형식이 올바르지 않습니다."
    ),

    INVALID_INPUT_VALUE(
        HttpStatus.BAD_REQUEST,
        "COM-003",
        "입력값이 올바르지 않습니다."
    ),

    INVALID_WORKPLACE_VALUE(
        HttpStatus.BAD_REQUEST,
        "COM-004",
        "스케줄이 해당 workplace 소속이 아닙니다."
    ),

    INVALID_WORKPLACE_MEMBER_VALUE(
        HttpStatus.BAD_REQUEST,
        "COM-005",
        "멤버가 해당 workplace 소속이 아닙니다."
    ),

    INVALID_STATUS_VALUE(
        HttpStatus.BAD_REQUEST,
        "COM-006",
        "적절한 상태가 아닙니다."
    ),

    INVALID_TIME_VALUE(
        HttpStatus.BAD_REQUEST,
        "COM-007",
        "시간 형식이나 값이 올바르지 않습니다."
    ),

    UNAVAILABLE_TIME_CONFLICT(
        HttpStatus.BAD_REQUEST,
        "UNAVAILABLE_TIME_CONFLICT",
        "해당 구성원이 이 시간에 근무 불가능 일정을 등록했습니다."
    ),

    NOT_FOUND_ERROR(
        HttpStatus.NOT_FOUND,
        "COM-404",
        "대상을 찾을 수 없습니다."
    ),

    INTERNAL_SERVER_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "COM-500",
        "서버 내부 오류가 발생했습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}