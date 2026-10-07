package com.sparta.coffee.common.config;

import com.sparta.coffee.domain.menu.entity.Menu;
import com.sparta.coffee.domain.menu.repository.MenuRepository;
import com.sparta.coffee.domain.point.entity.PointAccount;
import com.sparta.coffee.domain.point.repository.PointAccountRepository;
import com.sparta.coffee.domain.user.entity.User;
import com.sparta.coffee.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 로컬 확인용 초기 데이터.
 * 이미 데이터가 있으면 아무것도 하지 않으므로 재기동해도 중복 생성되지 않는다.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MenuRepository menuRepository;
    private final PointAccountRepository pointAccountRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (menuRepository.count() > 0) {
            return;
        }

        menuRepository.saveAll(List.of(
                Menu.of("아메리카노", 4000),
                Menu.of("카페라떼", 4500),
                Menu.of("바닐라라떼", 5000),
                Menu.of("콜드브루", 4800),
                Menu.of("자몽에이드", 5500)
        ));

        List.of("테스트유저A", "테스트유저B").forEach(name -> {
            User user = userRepository.save(User.of(name));
            pointAccountRepository.save(PointAccount.of(user));
        });

        log.info("초기 데이터 생성 완료 (메뉴 5건, 사용자 2명)");
    }
}
