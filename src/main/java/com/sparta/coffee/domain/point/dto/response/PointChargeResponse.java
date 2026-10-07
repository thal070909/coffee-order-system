package com.sparta.coffee.domain.point.dto.response;

public record PointChargeResponse(
        Long userId,
        long chargedAmount,
        long balance
) {
    public static PointChargeResponse of(Long userId, long chargedAmount, long balance) {
        return new PointChargeResponse(userId, chargedAmount, balance);
    }
}
