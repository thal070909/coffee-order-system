package com.sparta.coffee.domain.menu.entity;

import com.sparta.coffee.common.entity.BaseEntity;
import com.sparta.coffee.common.exception.BusinessException;
import com.sparta.coffee.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "menus")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false)
    private long price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus saleStatus;

    private Menu(String name, long price) {
        this.name = name;
        this.price = price;
        this.saleStatus = SaleStatus.ON_SALE;
    }

    public static Menu of(String name, long price) {
        return new Menu(name, price);
    }

    // 주문 가능한 상태인지는 메뉴 자신이 판단한다
    public void validateOrderable() {
        if (this.saleStatus == SaleStatus.SOLD_OUT) {
            throw new BusinessException(ErrorCode.MENU_SOLD_OUT);
        }
    }

    public void soldOut() {
        this.saleStatus = SaleStatus.SOLD_OUT;
    }
}
