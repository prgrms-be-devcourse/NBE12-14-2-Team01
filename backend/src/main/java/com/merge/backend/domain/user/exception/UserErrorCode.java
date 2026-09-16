package com.merge.backend.domain.user.exception;

import com.merge.backend.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "AUTH-001", "이미 사용중인 이메일입니다"),
    //api명세서 AUTH-02의 방식대로 세분화 하지 않고 로그인 실패 메시지 통합
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH-002", "이메일 또는 비밀번호가 올바르지 않습니다"),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-003", "유효하지 않은 인증 정보입니다"),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH-004", "로그인 후 이용해주세요."),
    INVALID_AUTHORIZATION_HEADER(
        HttpStatus.UNAUTHORIZED, "AUTH-005", "Authorization 헤더가 Bearer 형식이 아닙니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-006", "Access Token이 유효하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;


}
