package com.sparta.coffee.domain.menu.dto.response;

import com.sparta.coffee.domain.menu.entity.Menu;

public record MenuResponse(
        Long menuId,
        String name,
        long price,
        String saleStatus
) {
    public static MenuResponse from(Menu menu) {
        return new MenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getPrice(),
                menu.getSaleStatus().name()
        );
    }
}
