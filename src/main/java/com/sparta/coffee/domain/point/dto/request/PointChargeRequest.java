package com.sparta.coffee.domain.point.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PointChargeRequest(
        @NotNull(message = "충전 금액은 필수입니다.")
        @Min(value = 1, message = "충전 금액은 1P 이상이어야 합니다.")
        Long amount
) {
}
