package com.sparta.coffee;

import com.sparta.coffee.common.exception.BusinessException;
import com.sparta.coffee.common.exception.ErrorCode;
import com.sparta.coffee.domain.point.entity.PointAccount;
import com.sparta.coffee.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 포인트 잔액 규칙 단위 테스트. 스프링 컨텍스트 없이 엔티티만 검증한다.
 */
class PointAccountTest {

    private PointAccount newAccount() {
        return PointAccount.of(User.of("테스터"));
    }

    @Test
    @DisplayName("충전하면 잔액이 늘어난다")
    void 충전_성공() {
        PointAccount account = newAccount();

        account.charge(5_000L);

        assertThat(account.getBalance()).isEqualTo(5_000L);
    }

    @Test
    @DisplayName("0 이하 금액은 충전할 수 없다")
    void 충전_금액이_0_이하면_실패() {
        PointAccount account = newAccount();

        assertThatThrownBy(() -> account.charge(0L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_CHARGE_AMOUNT.getMessage());
    }

    @Test
    @DisplayName("1회 충전 한도를 초과하면 실패한다")
    void 충전_한도_초과() {
        PointAccount account = newAccount();

        assertThatThrownBy(() -> account.charge(1_000_001L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.EXCEED_CHARGE_LIMIT.getMessage());
    }

    @Test
    @DisplayName("잔액보다 큰 금액은 사용할 수 없다")
    void 잔액_부족() {
        PointAccount account = newAccount();
        account.charge(3_000L);

        assertThatThrownBy(() -> account.use(4_000L))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INSUFFICIENT_POINT.getMessage());

        assertThat(account.getBalance()).isEqualTo(3_000L);
    }

    @Test
    @DisplayName("사용하면 잔액이 줄어든다")
    void 사용_성공() {
        PointAccount account = newAccount();
        account.charge(10_000L);

        account.use(4_000L);

        assertThat(account.getBalance()).isEqualTo(6_000L);
    }
}
