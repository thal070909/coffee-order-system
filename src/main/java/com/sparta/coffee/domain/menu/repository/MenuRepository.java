package com.sparta.coffee.domain.menu.repository;

import com.sparta.coffee.domain.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {
}
