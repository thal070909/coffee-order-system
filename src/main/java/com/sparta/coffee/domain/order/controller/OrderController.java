package com.sparta.coffee.domain.order.controller;

import com.sparta.coffee.common.response.ApiResponse;
import com.sparta.coffee.domain.order.dto.request.OrderCreateRequest;
import com.sparta.coffee.domain.order.dto.response.OrderResponse;
import com.sparta.coffee.domain.order.service.OrderFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final OrderFacade orderFacade;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> order(
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @Valid @RequestBody OrderCreateRequest request) {

        OrderResponse response = orderFacade.order(
                request.userId(), request.menuId(), request.quantity(), idempotencyKey);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok("주문 및 결제가 완료되었습니다.", response));
    }
}
