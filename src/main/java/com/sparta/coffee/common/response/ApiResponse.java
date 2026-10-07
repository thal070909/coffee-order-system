package com.sparta.coffee.common.response;

public record ApiResponse<T>(
        String code,
        String message,
        T data
) {
    private static final String SUCCESS_CODE = "SUCCESS";

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(SUCCESS_CODE, message, data);
    }
}
