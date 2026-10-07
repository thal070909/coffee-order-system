package com.sparta.coffee.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 오류가 발생했습니다."),

    // 사용자
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),

    // 메뉴
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "MENU_001", "메뉴를 찾을 수 없습니다."),
    MENU_SOLD_OUT(HttpStatus.BAD_REQUEST, "MENU_002", "품절된 메뉴입니다."),

    // 포인트
    INVALID_CHARGE_AMOUNT(HttpStatus.BAD_REQUEST, "POINT_001", "충전 금액은 1P 이상이어야 합니다."),
    EXCEED_CHARGE_LIMIT(HttpStatus.BAD_REQUEST, "POINT_002", "1회 충전 한도(1,000,000P)를 초과했습니다."),
    EXCEED_BALANCE_LIMIT(HttpStatus.BAD_REQUEST, "POINT_003", "보유 가능한 포인트 한도(10,000,000P)를 초과했습니다."),
    INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "POINT_004", "포인트 잔액이 부족합니다."),
    POINT_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "POINT_005", "포인트 계정을 찾을 수 없습니다."),

    // 주문
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "ORDER_001", "주문 수량은 1개 이상이어야 합니다."),
    IDEMPOTENCY_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "ORDER_002", "Idempotency-Key 헤더가 필요합니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_003", "주문을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
