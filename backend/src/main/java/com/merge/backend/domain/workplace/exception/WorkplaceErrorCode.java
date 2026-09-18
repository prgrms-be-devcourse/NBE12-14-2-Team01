package com.merge.backend.domain.workplace.exception;

import com.merge.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WorkplaceErrorCode implements ErrorCode {

    INVALID_INVITE_CODE(
        HttpStatus.NOT_FOUND,
        "WPL-001",
        "유효하지 않은 초대 코드입니다."
    ),
    ALREADY_JOINED_WORKPLACE(
        HttpStatus.CONFLICT,
        "WPL-002",
        "이미 참여 중인 근무지입니다."
    ),
    NOT_WORKPLACE_MEMBER(
        HttpStatus.FORBIDDEN,
        "WPL-003",
        "해당 근무지의 현재 구성원이 아닙니다."
    ),
    MANAGER_REQUIRED(
        HttpStatus.FORBIDDEN,
        "WPL-004",
        "관리자 권한이 필요합니다."
    ),
    WORKPLACE_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "WPL-005",
        "존재하지 않는 근무지입니다."
    );


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
