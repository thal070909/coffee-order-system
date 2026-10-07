package com.sparta.coffee.domain.point.repository;

import com.sparta.coffee.domain.point.entity.PointAccount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PointAccountRepository extends JpaRepository<PointAccount, Long> {

    Optional<PointAccount> findByUserId(Long userId);

    /**
     * 포인트 잔액을 변경하기 전에 해당 행을 잠근다 (SELECT ... FOR UPDATE).
     * DB 가 보장하는 락이라 애플리케이션 인스턴스가 여러 대여도 동일하게 동작한다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PointAccount p WHERE p.user.id = :userId")
    Optional<PointAccount> findByUserIdForUpdate(@Param("userId") Long userId);
}
