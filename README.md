# 커피 주문 시스템

다수 서버·다수 인스턴스 환경에서 포인트 결제의 정합성을 보장하는 커피 주문 시스템

---

## 실행 방법

```bash
./gradlew bootRun
```

| 항목 | 값 |
| --- | --- |
| Java | 17 |
| Spring Boot | 3.4.1 |
| DB | MySQL 8.0 |
| Cache | Redis |

**사전 준비**

```sql
CREATE DATABASE coffee;
```

```
# 환경변수
DB_USERNAME=root
DB_PASSWORD=비밀번호
```

Redis는 `localhost:6379` 로 접속합니다 (환경변수 `REDIS_HOST`, `REDIS_PORT` 로 변경 가능).

서버를 처음 띄우면 메뉴 5건과 사용자 2명이 자동으로 생성됩니다.

---

## 1. 설계의 의도

이 과제의 기능 자체는 단순합니다. 메뉴 조회, 포인트 충전, 주문, 인기 메뉴 조회 네 가지뿐입니다.
난이도는 **"다수 인스턴스 환경에서도 정확해야 한다"** 는 제약에서 나옵니다.

그래서 모든 설계 판단을 아래 세 질문 기준으로 내렸습니다.

| 질문 | 적용된 곳 |
| --- | --- |
| 서버가 여러 대여도 성립하는가 | 락 전략, 멱등성 보장 위치, 캐시 위치 |
| 같은 순간 요청이 겹쳐도 깨지지 않는가 | 포인트 차감·충전, 중복 주문 |
| 일부가 실패해도 데이터가 어긋나지 않는가 | 주문-결제 원자성, 외부 전송 분리 |

---

## 2. ERD

```
Table users {
  user_id bigint [primary key]
  name varchar(30) [not null]
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table point_accounts {
  point_account_id bigint [primary key]
  user_id bigint [not null, unique, note: '회원당 1개']
  balance bigint [not null, default: 0, note: '현재 잔액(P)']
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table point_histories {
  point_history_id bigint [primary key]
  user_id bigint [not null]
  type varchar(20) [not null, note: 'CHARGE / USE']
  amount bigint [not null, note: '변동액(양수)']
  balance_after bigint [not null, note: '변동 후 잔액']
  order_id bigint [null, note: 'USE 인 경우 연결된 주문']
  created_at timestamp [not null]
  updated_at timestamp [not null]

  indexes {
    (user_id, created_at) [name: 'idx_point_histories_user']
  }
}

Table menus {
  menu_id bigint [primary key]
  name varchar(50) [not null]
  price bigint [not null]
  sale_status varchar(20) [not null, note: 'ON_SALE / SOLD_OUT']
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table orders {
  order_id bigint [primary key]
  user_id bigint [not null]
  menu_id bigint [not null]
  quantity int [not null]
  unit_price bigint [not null, note: '주문 시점 가격 스냅샷']
  total_price bigint [not null]
  status varchar(20) [not null, note: 'COMPLETED / CANCELED']
  idempotency_key varchar(100) [not null, unique, note: '중복 주문 차단']
  ordered_at timestamp [not null]

  indexes {
    (ordered_at, menu_id) [name: 'idx_orders_ordered_at_menu']
    (user_id, ordered_at) [name: 'idx_orders_user']
  }
}

Ref: users.user_id - point_accounts.user_id
Ref: users.user_id < point_histories.user_id
Ref: users.user_id < orders.user_id
Ref: menus.menu_id < orders.menu_id
Ref: orders.order_id < point_histories.order_id
```

### 테이블 설계 근거

**포인트 잔액을 `users` 가 아니라 `point_accounts` 로 분리한 이유**

주문할 때마다 잔액 행에 락을 겁니다. 잔액이 `users` 에 있으면 **이름 조회 같은 무관한 작업까지 같은 행을 건드리게 됩니다.**
계정 정보와 잔액은 변경 빈도가 완전히 다르므로 분리했습니다. 락 범위가 잔액 행으로 좁혀집니다.

**`point_histories` 를 둔 이유**

잔액만 있으면 "왜 이 금액이 됐는지"를 추적할 수 없습니다.
`balance_after` 를 함께 기록해 **특정 시점의 잔액을 재구성**할 수 있게 했습니다. 정합성 문제가 생겼을 때 어디서 어긋났는지 찾는 근거가 됩니다.

**`orders.unit_price` 를 따로 저장한 이유**

메뉴 가격은 바뀝니다. `menus` 를 조인해서 금액을 계산하면 **과거 주문의 결제 금액이 소급해서 달라집니다.**
주문 시점 가격을 스냅샷으로 남겨 과거 데이터를 고정합니다.

**`idempotency_key` 를 `orders` 에 둔 이유**

중복 주문 차단을 애플리케이션 로직이 아니라 **DB 유니크 제약으로 보장**합니다.
다수 인스턴스에서 동시에 같은 키로 들어와도 DB 가 하나만 통과시킵니다. 애플리케이션 레벨 체크는 인스턴스가 여러 대면 뚫립니다.

---

## 3. API 명세

### 3-1. 커피 메뉴 목록 조회

```
GET /api/menus
```

**Response 200**

```json
{
  "code": "SUCCESS",
  "message": "메뉴 목록 조회에 성공했습니다.",
  "data": {
    "menus": [
      { "menuId": 1, "name": "아메리카노", "price": 4000, "saleStatus": "ON_SALE" },
      { "menuId": 2, "name": "카페라떼", "price": 4500, "saleStatus": "ON_SALE" }
    ]
  }
}
```

---

### 3-2. 포인트 충전

```
POST /api/users/{userId}/points/charge
Content-Type: application/json
```

```json
{ "amount": 10000 }
```

**Response 200**

```json
{
  "code": "SUCCESS",
  "message": "포인트 충전에 성공했습니다.",
  "data": { "userId": 1, "chargedAmount": 10000, "balance": 15000 }
}
```

**예외**

| 상황 | HTTP | code |
| --- | --- | --- |
| 충전 금액이 0 이하 | 400 | `POINT_001` |
| 1회 충전 한도(1,000,000P) 초과 | 400 | `POINT_002` |
| 보유 한도(10,000,000P) 초과 | 400 | `POINT_003` |
| 존재하지 않는 사용자 | 404 | `USER_001` |

---

### 3-3. 커피 주문 / 결제

```
POST /api/orders
Content-Type: application/json
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

```json
{ "userId": 1, "menuId": 2, "quantity": 2 }
```

**Response 201**

```json
{
  "code": "SUCCESS",
  "message": "주문 및 결제가 완료되었습니다.",
  "data": {
    "orderId": 1001,
    "menuId": 2,
    "menuName": "카페라떼",
    "quantity": 2,
    "totalPrice": 9000,
    "balanceAfter": 6000,
    "orderedAt": "2026-10-07T14:00:00"
  }
}
```

**예외**

| 상황 | HTTP | code |
| --- | --- | --- |
| 포인트 잔액 부족 | 400 | `POINT_004` |
| 품절 메뉴 | 400 | `MENU_002` |
| 수량이 0 이하 | 400 | `ORDER_001` |
| `Idempotency-Key` 헤더 누락 | 400 | `ORDER_002` |
| 존재하지 않는 메뉴 | 404 | `MENU_001` |
| 존재하지 않는 사용자 | 404 | `USER_001` |

**중복 요청 처리**: 같은 `Idempotency-Key` 로 재요청이 오면 **새 주문을 만들지 않고 기존 주문 결과를 그대로 반환**합니다.

---

### 3-4. 인기 메뉴 목록 조회

```
GET /api/menus/popular
```

최근 7일간 주문 횟수 상위 3개를 반환합니다.

**Response 200**

```json
{
  "code": "SUCCESS",
  "message": "인기 메뉴 조회에 성공했습니다.",
  "data": {
    "periodFrom": "2026-10-01",
    "periodTo": "2026-10-07",
    "menus": [
      { "rank": 1, "menuId": 2, "name": "카페라떼", "orderCount": 152 },
      { "rank": 2, "menuId": 1, "name": "아메리카노", "orderCount": 98 },
      { "rank": 3, "menuId": 3, "name": "바닐라라떼", "orderCount": 47 }
    ]
  }
}
```

---

## 4. 문제 해결 전략

### 4-1. 포인트 동시성 — 비관적 락 선택

**문제**

같은 사용자가 동시에 두 건을 주문하면 두 요청이 모두 같은 잔액을 읽고 각자 차감합니다. 결과적으로 **한 번만 차감되거나 잔액이 음수**가 됩니다.

```
잔액 5,000원
요청 A: 5,000 읽음 → 4,000 차감 → 1,000 저장
요청 B: 5,000 읽음 → 4,500 차감 →   500 저장   ← A의 차감이 사라짐
```

**검토한 선택지**

| 방식 | 장점 | 이 과제에서의 문제 |
| --- | --- | --- |
| `synchronized` | 가장 단순 | **인스턴스가 여러 대면 무의미** — JVM 단위라 서버 간에는 효력 없음 |
| 낙관적 락 (`@Version`) | 락 대기 없음 | 같은 사용자의 연속 주문은 충돌 확률이 높아 재시도가 반복됨 |
| **비관적 락 (`SELECT FOR UPDATE`)** | DB 가 보장 → 인스턴스 수 무관 | 락 대기 발생 |
| Redis 분산 락 | DB 외부 로직까지 보호 | 인프라 추가, 락 해제 실패·TTL 만료 처리 필요 |

**선택: 비관적 락**

근거는 세 가지입니다.

**첫째, 경합 단위가 사용자 한 명입니다.** 이 시스템에는 재고 개념이 없어서(커피 메뉴는 수량 제한 없음) 서로 다른 사용자끼리는 경합하지 않습니다. 락은 `point_accounts` 의 **해당 사용자 행 하나**에만 걸리므로 전체 처리량에 미치는 영향이 제한적입니다.

**둘째, 같은 사용자의 연속 요청은 충돌 확률이 높습니다.** 따닥 클릭이나 재시도가 전형적인 시나리오입니다. 낙관적 락은 충돌이 잦으면 재시도 비용이 오히려 커집니다.

**셋째, DB 락이면 인스턴스 수와 무관합니다.** Redis 분산 락도 가능하지만, 보호해야 할 범위가 전부 DB 트랜잭션 안(잔액 읽기 → 차감 → 주문 생성)이라 외부 시스템을 하나 더 두는 이점이 없습니다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM PointAccount p WHERE p.user.id = :userId")
Optional<PointAccount> findByUserIdForUpdate(@Param("userId") Long userId);
```

**락 범위 최소화**: 락을 잡은 트랜잭션 안에서는 **외부 호출을 하지 않습니다.** 주문 내역 전송은 커밋 이후로 분리했습니다(4-3 참고).

**데드락 방지**: 한 트랜잭션에서 잡는 행은 `point_accounts` 한 건뿐입니다. 여러 행에 락을 거는 경로가 없으므로 순환 대기가 발생하지 않습니다.

**잔액 검증을 엔티티 안에 둔 이유**: 검증과 차감을 한 메서드로 묶어, 검증 없이 잔액이 바뀌는 경로를 만들지 않았습니다.

```java
public void use(long amount) {
    if (this.balance < amount) {
        throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
    }
    this.balance -= amount;
}
```

---

### 4-2. 중복 주문 방지 — 멱등키

**문제**

네트워크 타임아웃으로 클라이언트가 재시도하면 주문이 두 번 생깁니다. 포인트도 두 번 차감됩니다.

**전략**

클라이언트가 `Idempotency-Key` 헤더로 요청마다 고유 키를 보내고, 서버는 그 키를 `orders` 테이블에 **유니크 제약**으로 저장합니다.

```
1. 같은 키의 주문이 이미 있으면 → 기존 결과 반환 (재처리 안 함)
2. 없으면 주문 생성 시도
3. 동시에 같은 키가 들어와 유니크 제약 위반 → 기존 주문을 다시 읽어 반환
```

**애플리케이션에서 `exists` 체크만 하지 않는 이유**: 인스턴스가 여러 대면 두 서버가 동시에 "없음"을 확인하고 둘 다 INSERT 를 시도할 수 있습니다. **최종 방어선은 DB 제약조건이어야 합니다.**

**3번을 `OrderFacade` 로 분리한 이유**: 제약 위반이 발생하면 그 트랜잭션은 롤백됩니다(차감된 포인트도 함께 되돌아갑니다). 같은 트랜잭션 안에서 복구를 시도할 수 없으므로, 트랜잭션 **밖에서** 기존 주문을 다시 읽습니다.

```java
public OrderResponse order(Long userId, Long menuId, int quantity, String idempotencyKey) {
    try {
        return orderService.order(userId, menuId, quantity, idempotencyKey);
    } catch (DataIntegrityViolationException e) {
        return orderService.findByIdempotencyKey(userId, idempotencyKey);
    }
}
```

**`saveAndFlush` 를 쓴 이유**: `save` 만 쓰면 제약 위반이 커밋 시점에야 드러나 서비스 메서드 밖에서 예외가 발생합니다. 트랜잭션 안에서 바로 드러나게 해 흐름을 명확히 했습니다.

---

### 4-3. 주문 내역 실시간 전송 — 트랜잭션 분리

**문제**

데이터 수집 플랫폼 전송을 주문 트랜잭션 안에서 호출하면 두 가지 문제가 생깁니다.

- 외부 API 가 느리면 **락을 잡은 채로 대기** → 커넥션 풀 고갈
- 외부 API 가 실패하면 **주문까지 롤백** → 결제는 성공했는데 주문이 사라짐

**전략: 트랜잭션이 커밋된 뒤에 비동기로 전송합니다.**

```
[트랜잭션]
  잔액 차감 → 주문 생성 → 이벤트 발행
[커밋]
       ↓
[AFTER_COMMIT · 별도 스레드]
  외부 전송 → 실패 시 로그 기록
```

```java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleOrderCompleted(OrderCompletedEvent event) {
    try {
        dataPlatformClient.send(event);
    } catch (Exception e) {
        log.error("주문 내역 전송 실패: orderId={}", event.orderId(), e);
    }
}
```

**`@Async` 만으로는 안 되는 이유**: `@Async` 는 커밋 전에 실행될 수 있습니다. 그러면 전송 스레드가 **아직 커밋되지 않은 주문**을 조회하려다 실패합니다.
**`@TransactionalEventListener` 만으로도 부족한 이유**: 커밋 이후에 호출되긴 하지만 호출 스레드에서 동기로 실행되어, 외부 API 지연이 그대로 응답 지연이 됩니다.
두 가지를 함께 써야 "커밋 이후 + 별도 스레드"가 됩니다.

**전송 실패가 주문 실패가 아닌 이유**: 사용자 입장에서 결제는 이미 끝났습니다. 집계 전송 실패로 주문을 취소하는 건 더 나쁜 결과입니다. 그래서 전송 예외는 삼키고 로그만 남깁니다.

**남은 한계 — 이벤트 유실 가능성**

이 구조에서는 커밋 직후 전송 전에 **서버가 종료되면 그 이벤트가 사라집니다.** 재전송할 근거가 어디에도 남지 않기 때문입니다.

실제 서비스라면 **Outbox 패턴**이 필요합니다. 주문과 같은 트랜잭션으로 `outbox` 테이블에 전송 대기 레코드를 남기고, 전송 성공 시 상태를 변경하며, 스케줄러가 미전송 건을 재시도하는 방식입니다. 그러면 주문이 커밋된 이상 전송 기록도 반드시 남아 유실되지 않습니다.

이번 과제에서는 **구현 범위와 일정을 고려해 적용하지 않았습니다.** 다만 집계 데이터의 정확성이 중요해지는 시점에는 반드시 보완해야 할 지점이라고 판단합니다.

---

### 4-4. 인기 메뉴 — DB 집계 + Redis 캐시

**요구사항**: 최근 7일, 상위 3개, **주문 횟수가 정확해야 함**

**전략**

정확성의 기준은 `orders` 테이블입니다. Redis 카운터만으로 집계하면 빠르지만, 장애나 재시작 시 카운트가 어긋나면 **복구할 근거가 없습니다.**

그래서 **집계는 DB 에서, 결과만 Redis 에 캐시**합니다.

```java
@Query("""
        SELECT new com.sparta.coffee.domain.order.dto.response.PopularMenuRow(
                   o.menu.id, o.menu.name, COUNT(o))
        FROM Order o
        WHERE o.status = OrderStatus.COMPLETED
          AND o.orderedAt >= :from
        GROUP BY o.menu.id, o.menu.name
        ORDER BY COUNT(o) DESC, o.menu.id ASC
        """)
List<PopularMenuRow> findPopularMenus(@Param("from") LocalDateTime from, Pageable pageable);
```

```
요청 → Redis 조회 → 있으면 반환
                  → 없으면 DB 집계 → Redis 저장(TTL 60초) → 반환
```

**로컬 캐시가 아니라 Redis 인 이유**: 인스턴스가 여러 대면 로컬 캐시는 서버마다 값이 달라집니다. 같은 시점에 서버 A 는 라떼 1위, 서버 B 는 아메리카노 1위를 보여주는 상황이 생깁니다.

**TTL 60초의 근거**: 인기 메뉴는 초 단위 정확도가 필요한 데이터가 아닙니다. 1분 지연을 허용하는 대신 집계 쿼리 부하를 크게 줄입니다.

**2차 정렬 기준을 둔 이유**: 주문 횟수가 같을 때 순위가 조회할 때마다 바뀌면 사용자에게 혼란을 줍니다. `menu.id` 로 정렬해 결과를 고정했습니다.

**캐시 실패가 조회 실패가 아닌 이유**: 캐시는 부가 기능이므로 Redis 에 장애가 생겨도 DB 집계로 조회 자체는 계속 동작하도록 예외를 처리했습니다.

**인덱스**: `(ordered_at, menu_id)` 복합 인덱스로 기간 필터 후 메뉴별 집계가 인덱스를 타게 했습니다.

---

### 4-5. 다중 인스턴스 대응 요약

| 항목 | 단일 서버라면 | 다중 인스턴스 대응 |
| --- | --- | --- |
| 포인트 동시성 | `synchronized` | **DB 비관적 락** |
| 중복 주문 | 애플리케이션 중복 체크 | **DB 유니크 제약 (멱등키)** |
| 인기 메뉴 캐시 | 로컬 캐시(Caffeine) | **Redis 공유 캐시** |
| 주문 이벤트 전송 | 동기 호출 | **커밋 이후 비동기 전송** |

---

## 5. 테스트

### 실행 결과

```
BUILD SUCCESSFUL
```

| 분류 | 테스트 | 결과 |
| --- | --- | --- |
| 동시성 | 잔액 10,000P 로 4,000P 주문 동시 10건 → 2건 성공, 잔액 2,000P | 통과 |
| 동시성 | 1,000P 충전 동시 10건 → 누락 없이 10,000P | 통과 |
| 멱등성 | 같은 멱등키로 동시 10건 → 주문 1건만 생성 | 통과 |
| 단위 | 0 이하 금액 충전 거부 | 통과 |
| 단위 | 1회 충전 한도 초과 거부 | 통과 |
| 단위 | 잔액 부족 시 사용 거부 (잔액 변화 없음) | 통과 |
| 단위 | 충전·사용 시 잔액 반영 | 통과 |

### 로그로 확인한 동작

**비관적 락이 실제로 걸린다**

```sql
select pa1_0.point_account_id, pa1_0.balance, ...
from point_accounts pa1_0
where pa1_0.user_id=? for update
```

**동시 10건 중 2건만 주문이 생성된다**

```
[데이터 수집 플랫폼 전송] orderId=2, userId=3, menuId=6, paidAmount=4000
[데이터 수집 플랫폼 전송] orderId=3, userId=3, menuId=6, paidAmount=4000
```

**멱등키 중복 요청이 기존 주문으로 수렴한다**

```
멱등키 중복 요청 감지. 기존 주문을 반환한다: key=cff2aeae-...   (9회)
```

### 테스트 작성 시 유의한 점

동시성 테스트에는 `@Transactional` 을 붙이지 않았습니다. 테스트 메서드가 트랜잭션을 잡고 있으면 **락 동작을 검증할 수 없고**, 각 스레드가 커밋한 결과를 확인할 수도 없기 때문입니다. 대신 `@BeforeEach` 에서 데이터를 정리합니다.

```java
ExecutorService executor = Executors.newFixedThreadPool(threadCount);
CountDownLatch latch = new CountDownLatch(threadCount);

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
```

---

## 6. 기술 선택 이유

| 기술 | 이유 |
| --- | --- |
| Spring Boot, JPA | 트랜잭션 경계와 락 모드를 선언적으로 제어 |
| MySQL | `SELECT FOR UPDATE` 행 단위 락, 유니크 제약으로 멱등성 보장 |
| Redis | 인스턴스 간 공유 캐시 |
| Spring Event + `@Async` | 주문 트랜잭션과 외부 전송 분리 |
| JUnit5 + `ExecutorService` | 동시 요청 시나리오 검증 |

---

## 7. 패키지 구조

```
com.sparta.coffee
├── common
│   ├── config          DataSeeder
│   ├── entity          BaseEntity
│   ├── exception       ErrorCode, BusinessException, GlobalExceptionHandler
│   └── response        ApiResponse, ApiErrorResponse
└── domain
    ├── user            User
    ├── menu            Menu, SaleStatus
    ├── point           PointAccount, PointHistory, PointType
    └── order           Order, OrderStatus
                        OrderService, OrderFacade, PopularMenuService
                        OrderCompletedEvent, OrderEventListener, DataPlatformClient
```

도메인별로 `entity / repository / service / controller / dto` 를 묶었습니다.
공통 모듈은 도메인에 의존하지 않도록 분리해, 도메인이 늘어나도 공통 코드를 수정할 일이 없게 했습니다.

---

## 8. 개선 여지

| 항목 | 현재 | 개선 방향 |
| --- | --- | --- |
| 이벤트 유실 | 커밋 직후 서버 종료 시 전송 누락 | Outbox 테이블 + 재전송 스케줄러 |
| 캐시 갱신 | TTL 만료 시 여러 인스턴스가 동시 집계 | 분산 락으로 한 인스턴스만 갱신 |
| 인증 | `userId` 를 요청 본문으로 받음 | JWT 에서 추출해 위조 차단 |
| 주문 취소 | 미구현 | 포인트 환원 + 멱등성 고려 필요 |
