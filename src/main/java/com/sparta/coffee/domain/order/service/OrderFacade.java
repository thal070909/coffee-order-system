package com.sparta.coffee.domain.order.service;

import com.sparta.coffee.domain.order.dto.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * 멱등키 경합 처리를 담당한다.
 *
 * 애플리케이션에서 "이미 있는지" 확인하는 것만으로는 인스턴스가 여러 대일 때 두 서버가 동시에
 * "없음"을 확인하고 둘 다 INSERT 할 수 있다. 최종 방어선은 DB 유니크 제약이다.
 *
 * 제약 위반이 나면 먼저 성공한 주문이 반드시 존재하므로, 트랜잭션 밖에서 그 주문을 다시 읽어 반환한다.
 * 이렇게 하면 동시에 들어온 중복 요청도 사용자에겐 "같은 주문 1건"으로 보인다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFacade {

    private final OrderService orderService;

    public OrderResponse order(Long userId, Long menuId, int quantity, String idempotencyKey) {
        try {
            return orderService.order(userId, menuId, quantity, idempotencyKey);
        } catch (DataIntegrityViolationException e) {
            log.info("멱등키 중복 요청 감지. 기존 주문을 반환한다: key={}", idempotencyKey);

            return orderService.findByIdempotencyKey(userId, idempotencyKey);
        }
    }
}
