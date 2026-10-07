package com.sparta.coffee.domain.menu.dto.response;

import java.util.List;

public record MenuListResponse(
        List<MenuResponse> menus
) {
    public static MenuListResponse of(List<MenuResponse> menus) {
        return new MenuListResponse(menus);
    }
}
