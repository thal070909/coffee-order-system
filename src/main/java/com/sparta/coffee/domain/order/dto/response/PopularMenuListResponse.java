package com.sparta.coffee.domain.order.dto.response;

import java.time.LocalDate;
import java.util.List;

public record PopularMenuListResponse(
        LocalDate periodFrom,
        LocalDate periodTo,
        List<PopularMenuResponse> menus
) {
    public static PopularMenuListResponse of(LocalDate from, LocalDate to, List<PopularMenuResponse> menus) {
        return new PopularMenuListResponse(from, to, menus);
    }
}
