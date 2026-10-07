package com.sparta.coffee.domain.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparta.coffee.domain.order.dto.response.PopularMenuListResponse;
import com.sparta.coffee.domain.order.dto.response.PopularMenuResponse;
import com.sparta.coffee.domain.order.dto.response.PopularMenuRow;
import com.sparta.coffee.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 인기 메뉴 조회.
 *
 * 정확성의 기준은 orders 테이블이다. Redis 카운터만으로 집계하면 빠르지만 장애·재시작 시
 * 카운트가 어긋났을 때 복구할 근거가 없다. 그래서 집계는 DB 에서 하고 결과만 캐시한다.
 *
 * 로컬 캐시가 아니라 Redis 를 쓴 이유는 인스턴스가 여러 대일 때 서버마다 다른 순위를
 * 보여주는 상황을 막기 위해서다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PopularMenuService {

    private static final String CACHE_KEY = "popular:menus:top3";
    private static final Duration CACHE_TTL = Duration.ofSeconds(60);
    private static final int POPULAR_DAYS = 7;
    private static final int POPULAR_LIMIT = 3;

    private final OrderRepository orderRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public PopularMenuListResponse getPopularMenus() {
        PopularMenuListResponse cached = readCache();
        if (cached != null) {
            return cached;
        }

        PopularMenuListResponse response = aggregate();
        writeCache(response);

        return response;
    }

    @Transactional(readOnly = true)
    public PopularMenuListResponse aggregate() {
        LocalDateTime from = LocalDate.now().minusDays(POPULAR_DAYS - 1L).atStartOfDay();

        List<PopularMenuRow> rows =
                orderRepository.findPopularMenus(from, PageRequest.of(0, POPULAR_LIMIT));

        List<PopularMenuResponse> menus = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            menus.add(PopularMenuResponse.of(i + 1, rows.get(i)));
        }

        return PopularMenuListResponse.of(from.toLocalDate(), LocalDate.now(), menus);
    }

    // 캐시는 부가 기능이므로, Redis 에 문제가 생겨도 조회 자체는 DB 로 계속 동작해야 한다
    private PopularMenuListResponse readCache() {
        try {
            String cached = redisTemplate.opsForValue().get(CACHE_KEY);
            if (cached == null) {
                return null;
            }

            return objectMapper.readValue(cached, PopularMenuListResponse.class);
        } catch (Exception e) {
            log.warn("인기 메뉴 캐시 조회 실패. DB 집계로 대체한다.", e);

            return null;
        }
    }

    private void writeCache(PopularMenuListResponse response) {
        try {
            redisTemplate.opsForValue()
                    .set(CACHE_KEY, objectMapper.writeValueAsString(response), CACHE_TTL);
        } catch (Exception e) {
            log.warn("인기 메뉴 캐시 저장 실패", e);
        }
    }
}
