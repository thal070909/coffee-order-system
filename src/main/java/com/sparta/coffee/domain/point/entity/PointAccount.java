package com.sparta.coffee.domain.point.entity;

import com.sparta.coffee.common.entity.BaseEntity;
import com.sparta.coffee.common.exception.BusinessException;
import com.sparta.coffee.common.exception.ErrorCode;
import com.sparta.coffee.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 포인트 잔액을 users 가 아니라 별도 테이블로 분리했다.
 * 주문마다 이 행에 비관적 락을 걸기 때문에, 회원 정보 조회 같은 무관한 작업이 같은 행을 건드리지 않게 하기 위함이다.
 */
@Entity
@Getter
@Table(
        name = "point_accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_point_accounts_user_id", columnNames = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointAccount extends BaseEntity {

    private static final long MAX_CHARGE_PER_REQUEST = 1_000_000L;
    private static final long MAX_BALANCE = 10_000_000L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_account_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private long balance;

    private PointAccount(User user) {
        this.user = user;
        this.balance = 0L;
    }

    public static PointAccount of(User user) {
        return new PointAccount(user);
    }

    public void charge(long amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_CHARGE_AMOUNT);
        }
        if (amount > MAX_CHARGE_PER_REQUEST) {
            throw new BusinessException(ErrorCode.EXCEED_CHARGE_LIMIT);
        }
        if (this.balance + amount > MAX_BALANCE) {
            throw new BusinessException(ErrorCode.EXCEED_BALANCE_LIMIT);
        }

        this.balance += amount;
    }

    // 잔액 검증과 차감을 한 메서드 안에 묶어, 검증 없이 차감되는 경로를 만들지 않는다
    public void use(long amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (this.balance < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }

        this.balance -= amount;
    }
}
