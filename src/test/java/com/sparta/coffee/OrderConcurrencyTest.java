package com.sparta.coffee;

import com.sparta.coffee.common.exception.BusinessException;
import com.sparta.coffee.domain.menu.entity.Menu;
import com.sparta.coffee.domain.menu.repository.MenuRepository;
import com.sparta.coffee.domain.order.repository.OrderRepository;
import com.sparta.coffee.domain.order.service.OrderFacade;
import com.sparta.coffee.domain.point.entity.PointAccount;
import com.sparta.coffee.domain.point.repository.PointAccountRepository;
import com.sparta.coffee.domain.point.repository.PointHistoryRepository;
import com.sparta.coffee.domain.point.service.PointService;
import com.sparta.coffee.domain.user.entity.User;
import com.sparta.coffee.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시 요청 상황에서 포인트 정합성이 깨지지 않는지 검증한다.
 * 실제 락 동작을 확인해야 하므로 테스트 메서드에 @Transactional 을 붙이지 않는다.
 */
@SpringBootTest
class OrderConcurrencyTest {

    @Autowired OrderFacade orderFacade;
    @Autowired PointService pointService;
    @Autowired UserRepository userRepository;
    @Autowired MenuRepository menuRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired PointAccountRepository pointAccountRepository;
    @Autowired PointHistoryRepository pointHistoryRepository;

    private Long userId;
    private Long menuId;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAllInBatch();
        pointHistoryRepository.deleteAllInBatch();
        pointAccountRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        menuRepository.deleteAllInBatch();

        User user = userRepository.save(User.of("동시성테스트"));
        pointAccountRepository.save(PointAccount.of(user));
        Menu menu = menuRepository.save(Menu.of("아메리카노", 4000));

        this.userId = user.getId();
        this.menuId = menu.getId();
    }

    @Test
    @DisplayName("잔액 10,000P 로 4,000P 주문을 동시에 10건 보내면 2건만 성공하고 잔액은 2,000P 가 된다")
    void 동시_주문_시_잔액이_음수가_되지_않는다() throws InterruptedException {
        // given
        pointService.charge(userId, 10_000L);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    orderFacade.order(userId, menuId, 1, UUID.randomUUID().toString());
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // then
        long balance = pointService.getBalance(userId);

        assertThat(successCount.get()).isEqualTo(2);
        assertThat(failCount.get()).isEqualTo(8);
        assertThat(balance).isEqualTo(2_000L);
        assertThat(orderRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("1,000P 충전을 동시에 10건 보내면 누락 없이 10,000P 가 된다")
    void 동시_충전_시_갱신이_유실되지_않는다() throws InterruptedException {
        // given
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    pointService.charge(userId, 1_000L);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // then
        assertThat(pointService.getBalance(userId)).isEqualTo(10_000L);
    }

    @Test
    @DisplayName("같은 멱등키로 동시에 10건을 보내도 주문은 1건만 생성된다")
    void 멱등키가_같으면_주문이_중복_생성되지_않는다() throws InterruptedException {
        // given
        pointService.charge(userId, 100_000L);

        String idempotencyKey = UUID.randomUUID().toString();
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    orderFacade.order(userId, menuId, 1, idempotencyKey);
                } catch (Exception ignored) {
                    // 경합에서 밀린 요청은 기존 주문을 반환하거나 예외로 끝난다
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();

        // then
        assertThat(orderRepository.count()).isEqualTo(1);
        assertThat(pointService.getBalance(userId)).isEqualTo(96_000L);
    }
}
