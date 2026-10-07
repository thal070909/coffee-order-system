package com.sparta.coffee.domain.order.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final DataPlatformClient dataPlatformClient;

    /**
     * 주문 트랜잭션이 커밋된 뒤 별도 스레드에서 전송한다.
     *
     * - @Async 만 쓰면 커밋 전에 실행될 수 있어 아직 없는 주문을 바라보게 된다.
     * - @TransactionalEventListener(AFTER_COMMIT) 만 쓰면 호출 스레드에서 동기로 실행되어
     *   외부 API 지연이 그대로 응답 지연이 된다.
     * 그래서 두 가지를 함께 사용한다.
     *
     * 전송 실패가 주문 실패는 아니므로 예외는 로그만 남기고 삼킨다.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        try {
            dataPlatformClient.send(event);
        } catch (Exception e) {
            log.error("주문 내역 전송 실패: orderId={}", event.orderId(), e);
        }
    }
}
