package com.sparta.coffee.domain.user.repository;

import com.sparta.coffee.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
