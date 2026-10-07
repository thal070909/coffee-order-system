package com.sparta.coffee.domain.order.service;

import com.sparta.coffee.common.exception.BusinessException;
import com.sparta.coffee.common.exception.ErrorCode;
import com.sparta.coffee.domain.menu.entity.Menu;
import com.sparta.coffee.domain.menu.repository.MenuRepository;
import com.sparta.coffee.domain.order.dto.response.OrderResponse;
import com.sparta.coffee.domain.order.entity.Order;
import com.sparta.coffee.domain.order.event.OrderCompletedEvent;
import com.sparta.coffee.domain.order.repository.OrderRepository;
import com.sparta.coffee.domain.point.entity.PointAccount;
import com.sparta.coffee.domain.point.entity.PointHistory;
import com.sparta.coffee.domain.point.repository.PointAccountRepository;
import com.sparta.coffee.domain.point.repository.PointHistoryRepository;
import com.sparta.coffee.domain.user.entity.User;
import com.sparta.coffee.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserRepository userRepository;
    private final MenuRepository menuRepository;
    private final OrderRepository orderRepository;
    private final PointAccountRepository pointAccountRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 주문 및 포인트 결제.
     *
     * 1) 같은 멱등키의 주문이 이미 있으면 재처리하지 않고 기존 결과를 반환한다.
     * 2) 포인트 잔액 행에 비관적 락을 걸어 동시 주문 시 중복 차감을 막는다.
     * 3) 잔액 차감과 주문 생성이 한 트랜잭션이므로, 하나라도 실패하면 함께 롤백된다.
     * 4) 외부 전송은 커밋 이후로 넘긴다. 락을 잡은 채 외부 API 를 기다리지 않기 위함이다.
     */
    @Transactional
    public OrderResponse order(Long userId, Long menuId, int quantity, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);

        Optional<Order> alreadyProcessed = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (alreadyProcessed.isPresent()) {
            return OrderResponse.of(alreadyProcessed.get(), getBalance(userId));
        }

        User user = getUser(userId);
        Menu menu = getMenu(menuId);
        menu.validateOrderable();

        PointAccount account = getAccountForUpdate(user);
        Order order = Order.of(user, menu, quantity, idempotencyKey);

        account.use(order.getTotalPrice());

        // 커밋 시점이 아니라 지금 INSERT 를 날려, 멱등키 중복이면 이 트랜잭션 안에서 바로 드러나게 한다
        orderRepository.saveAndFlush(order);
        pointHistoryRepository.save(
                PointHistory.ofUse(user, order.getTotalPrice(), account.getBalance(), order.getId()));

        eventPublisher.publishEvent(new OrderCompletedEvent(
                order.getId(), userId, menuId, order.getTotalPrice()));

        return OrderResponse.of(order, account.getBalance());
    }

    @Transactional(readOnly = true)
    public OrderResponse findByIdempotencyKey(Long userId, String idempotencyKey) {
        Order order = orderRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        return OrderResponse.of(order, getBalance(userId));
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }
    }

    private long getBalance(Long userId) {
        return pointAccountRepository.findByUserId(userId)
                .map(PointAccount::getBalance)
                .orElse(0L);
    }

    private PointAccount getAccountForUpdate(User user) {
        return pointAccountRepository.findByUserIdForUpdate(user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.POINT_ACCOUNT_NOT_FOUND));
    }

    private Menu getMenu(Long menuId) {
        return menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
