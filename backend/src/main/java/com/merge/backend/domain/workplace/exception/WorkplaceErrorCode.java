package com.merge.backend.domain.workplace.exception;

import com.merge.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WorkplaceErrorCode implements ErrorCode {

    INVALID_INVITE_CODE(HttpStatus.NOT_FOUND, "WPL-001", "유효하지 않은 초대 코드입니다."),
    ALREADY_JOINED_WORKPLACE(HttpStatus.CONFLICT, "WPL-002", "이미 참여 중인 근무지입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
