package com.sparta.coffee.common.exception;

import com.sparta.coffee.common.response.ApiErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BusinessException: {} - {}", errorCode.getCode(), errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiErrorResponse.of(errorCode));
    }

    // @Valid 검증 실패. 어떤 필드가 왜 틀렸는지 메시지로 돌려준다
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse(ErrorCode.INVALID_INPUT.getMessage());

        log.warn("ValidationException: {}", message);

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ApiErrorResponse.of(ErrorCode.INVALID_INPUT, message));
    }

    // Idempotency-Key 헤더 누락
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingHeaderException(MissingRequestHeaderException e) {
        log.warn("MissingRequestHeaderException: {}", e.getHeaderName());

        return ResponseEntity
                .status(ErrorCode.IDEMPOTENCY_KEY_REQUIRED.getStatus())
                .body(ApiErrorResponse.of(ErrorCode.IDEMPOTENCY_KEY_REQUIRED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception e) {
        log.error("Unhandled Exception", e);

        return ResponseEntity
                .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
