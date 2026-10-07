package com.sparta.coffee.domain.menu.controller;

import com.sparta.coffee.common.response.ApiResponse;
import com.sparta.coffee.domain.menu.dto.response.MenuListResponse;
import com.sparta.coffee.domain.menu.service.MenuService;
import com.sparta.coffee.domain.order.dto.response.PopularMenuListResponse;
import com.sparta.coffee.domain.order.service.PopularMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;
    private final PopularMenuService popularMenuService;

    @GetMapping
    public ResponseEntity<ApiResponse<MenuListResponse>> getMenus() {
        return ResponseEntity.ok(
                ApiResponse.ok("메뉴 목록 조회에 성공했습니다.", menuService.getMenus()));
    }

    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<PopularMenuListResponse>> getPopularMenus() {
        return ResponseEntity.ok(
                ApiResponse.ok("인기 메뉴 조회에 성공했습니다.", popularMenuService.getPopularMenus()));
    }
}
