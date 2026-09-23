package com.merge.backend.domain.substitute.exception;

import com.merge.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SubstituteRequestErrorCode implements ErrorCode {

    SUBSTITUTE_REQUEST_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "SUB-001",
        "존재하지 않는 대체근무 요청입니다."
    ),
    INVALID_REQUEST(
        HttpStatus.BAD_REQUEST,
        "SUB-002",
        "후보자의 수락이 필요합니다."
    ),
    VALIDATE_STATUS_CLOSED(
        HttpStatus.BAD_REQUEST,
        "SUB-003",
        "해당 요청 이미 수락되거나 승인되었습니다."
    ),
    INVALID_MEMBER(
        HttpStatus.BAD_REQUEST,
        "SUB-004",
        "대체 요청자와 실제 근무 담당자가 같지 않습니다."
    ),
    CONFLICT_SHIFT(
        HttpStatus.CONFLICT,
        "SUB-005",
        "해당 근무 시간에 중복되는 근무가 있습니다."
    ),
    CONFLICT_UNAVAILABLE_TIME(
        HttpStatus.CONFLICT,
        "SUB-006",
            "등록한 불가능 시간과 중복됩니다."
    ),
    CONFLICT_ACTIVE_SUBSTITUTE(
        HttpStatus.CONFLICT,
        "SUB-007",
        "이미 수락한 대체 근무 시간과 중복됩니다."
    ),
    NOT_FOUND_CANDIDATE(
        HttpStatus.CONFLICT,
        "SUB-008",
        "수락한 후보자를 찾을 수 없습니다."
    ),
    ACCEPTED_CANDIDATE_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "SUB-015",
        "ACCEPTED 상태인 요청의 수락자 정보를 찾을 수 없습니다."
    ),
    ACCEPTED_CANDIDATE_DUPLICATED(
        HttpStatus.BAD_REQUEST,
        "SUB-016",
        "하나의 요청에 대해 수락자가 여러 명 조회되었습니다."
    ),
    ALREADY_TERMINATED(
        HttpStatus.BAD_REQUEST,
        "SUB-017",
        "이미 종료되었거나 승인된 요청은 종료할 수 없습니다."
    ),
    SHIFT_ALREADY_STARTED(
        HttpStatus.BAD_REQUEST,
        "SUB-018",
        "근무 시작 이후에는 요청을 종료할 수 없습니다."
    );
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
