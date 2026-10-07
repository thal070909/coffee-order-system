package com.sparta.coffee.domain.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderCreateRequest(
        @NotNull(message = "사용자 식별값은 필수입니다.")
        Long userId,

        @NotNull(message = "메뉴 ID는 필수입니다.")
        Long menuId,

        @NotNull(message = "주문 수량은 필수입니다.")
        @Min(value = 1, message = "주문 수량은 1개 이상이어야 합니다.")
        Integer quantity
) {
}
