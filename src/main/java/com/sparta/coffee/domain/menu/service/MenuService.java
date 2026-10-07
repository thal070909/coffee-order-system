package com.sparta.coffee.domain.menu.service;

import com.sparta.coffee.domain.menu.dto.response.MenuListResponse;
import com.sparta.coffee.domain.menu.dto.response.MenuResponse;
import com.sparta.coffee.domain.menu.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuRepository menuRepository;

    @Transactional(readOnly = true)
    public MenuListResponse getMenus() {
        List<MenuResponse> menus = menuRepository.findAll().stream()
                .map(MenuResponse::from)
                .toList();

        return MenuListResponse.of(menus);
    }
}
