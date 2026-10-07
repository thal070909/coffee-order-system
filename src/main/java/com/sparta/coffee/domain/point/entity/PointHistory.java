package com.sparta.coffee.domain.point.entity;

import com.sparta.coffee.common.entity.BaseEntity;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 잔액만 저장하면 "왜 이 금액이 됐는지"를 추적할 수 없다.
 * balanceAfter 를 함께 남겨 특정 시점의 잔액을 재구성할 수 있게 했다.
 */
@Entity
@Getter
@Table(
        name = "point_histories",
        indexes = @Index(name = "idx_point_histories_user", columnList = "user_id, created_at")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PointType type;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private long balanceAfter;

    @Column(name = "order_id")
    private Long orderId;

    private PointHistory(User user, PointType type, long amount, long balanceAfter, Long orderId) {
        this.user = user;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.orderId = orderId;
    }

    public static PointHistory ofCharge(User user, long amount, long balanceAfter) {
        return new PointHistory(user, PointType.CHARGE, amount, balanceAfter, null);
    }

    public static PointHistory ofUse(User user, long amount, long balanceAfter, Long orderId) {
        return new PointHistory(user, PointType.USE, amount, balanceAfter, orderId);
    }
}
