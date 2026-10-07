package com.sparta.coffee.domain.order.entity;

import com.sparta.coffee.common.exception.BusinessException;
import com.sparta.coffee.common.exception.ErrorCode;
import com.sparta.coffee.domain.menu.entity.Menu;
import com.sparta.coffee.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_orders_idempotency_key", columnNames = "idempotency_key"),
        indexes = {
                @Index(name = "idx_orders_ordered_at_menu", columnList = "ordered_at, menu_id"),
                @Index(name = "idx_orders_user", columnList = "user_id, ordered_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    @Column(nullable = false)
    private int quantity;

    // 메뉴 가격은 바뀐다. 조인해서 계산하면 과거 주문 금액이 소급해서 달라지므로 주문 시점 가격을 남긴다
    @Column(nullable = false)
    private long unitPrice;

    @Column(nullable = false)
    private long totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    // 중복 주문 차단은 애플리케이션 체크가 아니라 DB 유니크 제약으로 보장한다
    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(name = "ordered_at", nullable = false)
    private LocalDateTime orderedAt;

    private Order(User user, Menu menu, int quantity, String idempotencyKey) {
        this.user = user;
        this.menu = menu;
        this.quantity = quantity;
        this.unitPrice = menu.getPrice();
        this.totalPrice = menu.getPrice() * quantity;
        this.status = OrderStatus.COMPLETED;
        this.idempotencyKey = idempotencyKey;
        this.orderedAt = LocalDateTime.now();
    }

    public static Order of(User user, Menu menu, int quantity, String idempotencyKey) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }

        return new Order(user, menu, quantity, idempotencyKey);
    }
}
