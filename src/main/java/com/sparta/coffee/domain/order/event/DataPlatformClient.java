package com.sparta.coffee.domain.order.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 데이터 수집 플랫폼 전송 Mock.
 * 실제로는 외부 HTTP 호출이 일어나는 자리이며, 테스트에서는 이 빈을 대체해 호출 여부를 검증한다.
 */
@Slf4j
@Component
public class DataPlatformClient {

    public void send(OrderCompletedEvent event) {
        log.info("[데이터 수집 플랫폼 전송] orderId={}, userId={}, menuId={}, paidAmount={}",
                event.orderId(), event.userId(), event.menuId(), event.paidAmount());
    }
}
