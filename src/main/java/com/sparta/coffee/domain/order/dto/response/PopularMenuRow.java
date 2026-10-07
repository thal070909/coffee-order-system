package com.sparta.coffee.domain.order.dto.response;

/**
 * 인기 메뉴 집계 쿼리의 결과를 그대로 받는 타입.
 */
public record PopularMenuRow(
        Long menuId,
        String menuName,
        Long orderCount
) {
}
