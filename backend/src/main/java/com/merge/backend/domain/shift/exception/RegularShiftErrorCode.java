package com.merge.backend.domain.shift.exception;

import com.merge.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RegularShiftErrorCode implements ErrorCode {
    WORKPLACE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "SHIFT-001",
            "존재하지 않는 근무지입니다."
    ),

    MEMBER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "SHIFT-002",
            "존재하지 않는 근로자입니다."
    ),

    PATTERN_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "SHIFT-003",
            "존재하지 않는 정기 근무입니다."
    ),

    MEMBER_NOT_IN_WORKPLACE(
            HttpStatus.BAD_REQUEST,
            "SHIFT-004",
            "해당 근무지의 구성원이 아닙니다."
    ),

    MEMBER_NOT_ACTIVE(
            HttpStatus.BAD_REQUEST,
            "SHIFT-005",
            "현재 근무 중인 구성원이 아닙니다."
    ),

    INVALID_PATTERN_TIME(
            HttpStatus.BAD_REQUEST,
            "SHIFT-006",
            "시작 시간은 종료 시간보다 빨라야 합니다."
    ),

    PATTERN_TIME_OVERLAP(
            HttpStatus.CONFLICT,
            "SHIFT-007",
            "기존 정기 근무 시간과 겹칩니다."
    ),

    PATTERN_NOT_IN_WORKPLACE(
            HttpStatus.BAD_REQUEST,
            "SHIFT-008",
            "해당 근무지의 정기 근무가 아닙니다."
    ),

    MANAGER_REQUIRED(
            HttpStatus.FORBIDDEN,
            "SHIFT-009",
            "관리자 권한이 필요합니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
