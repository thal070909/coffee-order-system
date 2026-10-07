package com.sparta.coffee.domain.order.event;

public record OrderCompletedEvent(
        Long orderId,
        Long userId,
        Long menuId,
        long paidAmount
) {
}
