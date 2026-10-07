package com.sparta.coffee.domain.order.repository;

import com.sparta.coffee.domain.order.dto.response.PopularMenuRow;
import com.sparta.coffee.domain.order.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    /**
     * 최근 N일 인기 메뉴 집계.
     * 정확성의 기준은 orders 테이블이므로 집계는 항상 DB 에서 수행하고, 결과만 캐시한다.
     * 주문 횟수가 같을 때 순위가 흔들리지 않도록 menuId 로 2차 정렬한다.
     */
    @Query("""
            SELECT new com.sparta.coffee.domain.order.dto.response.PopularMenuRow(
                       o.menu.id, o.menu.name, COUNT(o))
            FROM Order o
            WHERE o.status = com.sparta.coffee.domain.order.entity.OrderStatus.COMPLETED
              AND o.orderedAt >= :from
            GROUP BY o.menu.id, o.menu.name
            ORDER BY COUNT(o) DESC, o.menu.id ASC
            """)
    List<PopularMenuRow> findPopularMenus(@Param("from") LocalDateTime from, Pageable pageable);
}
