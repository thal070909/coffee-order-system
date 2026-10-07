package com.sparta.coffee.domain.point.service;

import com.sparta.coffee.common.exception.BusinessException;
import com.sparta.coffee.common.exception.ErrorCode;
import com.sparta.coffee.domain.point.dto.response.PointChargeResponse;
import com.sparta.coffee.domain.point.entity.PointAccount;
import com.sparta.coffee.domain.point.entity.PointHistory;
import com.sparta.coffee.domain.point.repository.PointAccountRepository;
import com.sparta.coffee.domain.point.repository.PointHistoryRepository;
import com.sparta.coffee.domain.user.entity.User;
import com.sparta.coffee.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserRepository userRepository;
    private final PointAccountRepository pointAccountRepository;
    private final PointHistoryRepository pointHistoryRepository;

    /**
     * 포인트 충전.
     * 동시 충전 시 한쪽 충전이 덮이지 않도록 잔액 행에 비관적 락을 건다.
     */
    @Transactional
    public PointChargeResponse charge(Long userId, long amount) {
        User user = getUser(userId);
        PointAccount account = getAccountForUpdate(user);

        account.charge(amount);
        pointHistoryRepository.save(PointHistory.ofCharge(user, amount, account.getBalance()));

        return PointChargeResponse.of(userId, amount, account.getBalance());
    }

    @Transactional(readOnly = true)
    public long getBalance(Long userId) {
        return pointAccountRepository.findByUserId(userId)
                .map(PointAccount::getBalance)
                .orElse(0L);
    }

    private PointAccount getAccountForUpdate(User user) {
        return pointAccountRepository.findByUserIdForUpdate(user.getId())
                .orElseGet(() -> pointAccountRepository.save(PointAccount.of(user)));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
