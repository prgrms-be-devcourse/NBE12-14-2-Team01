package com.merge.backend.global.dto;

import java.util.List;

public record ValidationErrorResponse(
    List<FieldErrorDetail> fieldErrors
) {

    public record FieldErrorDetail(
        String field,
        String message
    ) {

    }
}