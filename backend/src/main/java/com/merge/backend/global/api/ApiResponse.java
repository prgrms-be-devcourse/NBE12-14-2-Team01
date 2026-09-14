package com.merge.backend.global.api;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T>{

    private boolean success;
    private int code;
    private String message;
    private T data;

    // 가장 기본적인 성공 응답 (code + 데이터) 대부분 이걸 사용
    public static <T> ApiResponse<T> success(int code, T data){
        return ApiResponse.<T>builder().success(true).code(code).message("").data(data).build();
    }


    // 성공이지만 메시지와 데이터가 없을때 (주로 삭제)
    public static ApiResponse<Void> noContentSuccess() {
        return ApiResponse.<Void>builder().success(true).message("").code(204).data(null).build();
    }

    //성공이지만 데이터 없이 메시지는 보내고 싶을때
    public static ApiResponse<Void> noContentSuccess(String message) {
        return ApiResponse.<Void>builder().success(true).message(message).code(204).data(null).build();
    }

    // 실패 + 데이터 있음 (예: 검증 실패한 필드 목록 같이 내려줄 때)
    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return ApiResponse.<T>builder().success(false).code(code).message(message).data(data).build();
    }

    // 실패 + 데이터 없음 (제일 흔한 케이스, GlobalExceptionHandler에서 주로 사용)
    public static  ApiResponse<Void> error(int code, String message) {
        return ApiResponse.<Void>builder().success(false).code(code).message(message).data(null).build();
    }

}