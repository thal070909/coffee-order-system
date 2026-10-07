package com.sparta.coffee.domain.order.dto.response;

import com.sparta.coffee.domain.order.entity.Order;

import java.time.LocalDateTime;

public record OrderResponse(
        Long orderId,
        Long menuId,
        String menuName,
        int quantity,
        long totalPrice,
        long balanceAfter,
        LocalDateTime orderedAt
) {
    public static OrderResponse of(Order order, long balanceAfter) {
        return new OrderResponse(
                order.getId(),
                order.getMenu().getId(),
                order.getMenu().getName(),
                order.getQuantity(),
                order.getTotalPrice(),
                balanceAfter,
                order.getOrderedAt()
        );
    }
}
