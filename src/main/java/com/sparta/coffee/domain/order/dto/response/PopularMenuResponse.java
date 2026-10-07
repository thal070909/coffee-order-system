package com.sparta.coffee.domain.order.dto.response;

public record PopularMenuResponse(
        int rank,
        Long menuId,
        String name,
        long orderCount
) {
    public static PopularMenuResponse of(int rank, PopularMenuRow row) {
        return new PopularMenuResponse(rank, row.menuId(), row.menuName(), row.orderCount());
    }
}
