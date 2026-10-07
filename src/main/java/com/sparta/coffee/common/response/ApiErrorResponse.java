package com.sparta.coffee.common.response;

import com.sparta.coffee.common.exception.ErrorCode;

public record ApiErrorResponse(
        String code,
        String message
) {
    public static ApiErrorResponse of(ErrorCode errorCode) {
        return new ApiErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }

    public static ApiErrorResponse of(ErrorCode errorCode, String message) {
        return new ApiErrorResponse(errorCode.getCode(), message);
    }
}
