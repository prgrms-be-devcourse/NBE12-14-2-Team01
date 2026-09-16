package com.merge.backend.domain.shift.exception;

import com.merge.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ScheduleErrorCode implements ErrorCode {

    INVALID_WEEK_START_DATE(
        HttpStatus.BAD_REQUEST,
        "SCH-001",
        "주 시작일은 월요일이어야 합니다."
    ),

    SCHEDULE_ALREADY_EXISTS(
        HttpStatus.CONFLICT,
        "SCH-002",
        "해당 주차의 근무표가 이미 존재합니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
