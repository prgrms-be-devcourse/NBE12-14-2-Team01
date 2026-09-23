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
        "SUB-009",
        "해당 shift를 찾을 수 없습니다."
    ),

    SHIFT_NOT_PUBLISHED(
        HttpStatus.BAD_REQUEST,
        "SUB-010",
        "아직 공개되지 않은 근무입니다."
    ),

    NOT_OWN_SHIFT(
        HttpStatus.FORBIDDEN,
        "SUB-011",
        "본인 근무에 대해서만 대체근무 요청을 할 수 있습니다"
    ),

    SHIFT_ALREADY_STARTED(
        HttpStatus.BAD_REQUEST,
        "SUB-012",
        "이미 시작된 근무는 대체근무를 요청할 수 없습니다"
    ),

    ACTIVE_REQUEST_EXISTS(
        HttpStatus.CONFLICT,
        "SUB-013",
        "이미 진행중인 대체근무 요청이 있습니다"
    ),

    SHIFT_CANCELLED(
        HttpStatus.BAD_REQUEST,
        "SUB-014",
        "취소된 근무에는 대체근무를 요청할 수 없습니다"
    ),
    CANDIDATE_NOT_FOUND(
        HttpStatus.NOT_FOUND,
    "SUB-015",
        "해당 대타 후보를 찾을 수 없습니다."
    ),

    NOT_OWN_CANDIDATE(
        HttpStatus.FORBIDDEN,
    "SUB-016",
        "본인의 대타 요청에만 응답할 수 있습니다."
    ),

    INVALID_CANDIDATE_MEMBER(
        HttpStatus.FORBIDDEN,
    "SUB-017",
        "현재 유효한 직원만 대타 요청에 응답할 수 있습니다."
    ),

    CANDIDATE_ALREADY_RESPONDED(
        HttpStatus.CONFLICT,
    "SUB-018",
        "이미 응답한 대타 요청입니다."
    ),

    REQUEST_NOT_OPEN(
        HttpStatus.CONFLICT,
    "SUB-019",
        "더 이상 응답할 수 없는 대타 요청입니다."
    ),

    SHIFT_CONFLICT(
        HttpStatus.CONFLICT,
    "SUB-020",
        "해당 시간에 이미 다른 근무가 있습니다."
    ),

    UNAVAILABLE_TIME_CONFLICT(
        HttpStatus.CONFLICT,
    "SUB-021",
        "근무 불가능 시간과 겹쳐 대타 요청을 수락할 수 없습니다."
    ),

    ACCEPTED_SUBSTITUTE_CONFLICT(
        HttpStatus.CONFLICT,
    "SUB-022",
        "같은 시간대에 이미 수락한 다른 대타 요청이 있습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
