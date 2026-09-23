package com.merge.backend.domain.notification.exception;

import com.merge.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    NOT_FOUND_NOTIFICATION(
        HttpStatus.NOT_FOUND,
        "NOT-001",
        "알림을 찾을 수 없습니다."
    ),

    FORBIDDEN_ACCESS(
        HttpStatus.FORBIDDEN,
        "NOT-002",
        "해당 알림에 접근할 권한이 없습니다."
    );

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}