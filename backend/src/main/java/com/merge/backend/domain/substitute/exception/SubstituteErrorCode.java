package com.merge.backend.domain.substitute.exception;

import com.merge.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SubstituteErrorCode implements ErrorCode {

    SHIFT_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "SUB-001",
        "해당 shift를 찾을 수 없습니다."
    ),

    SHIFT_NOT_PUBLISHED(
        HttpStatus.BAD_REQUEST,
        "SUB-002",
        "아직 공개되지 않은 근무입니다."
    ),

    NOT_OWN_SHIFT(
        HttpStatus.FORBIDDEN,
        "SUB-003",
        "본인 근무에 대해서만 대체근무 요청을 할 수 있습니다"
    ),

    SHIFT_ALREADY_STARTED(
        HttpStatus.BAD_REQUEST,
        "SUB-004",
        "이미 시작된 근무는 대체근무를 요청할 수 없습니다"
    ),

    ACTIVE_REQUEST_EXISTS(
        HttpStatus.CONFLICT,
        "SUB-005",
        "이미 진행중인 대체근무 요청이 있습니다"
    ),

    SHIFT_CANCELLED(
        HttpStatus.BAD_REQUEST,
        "SUB-006",
        "취소된 근무에는 대체근무를 요청할 수 없습니다"
    );


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
