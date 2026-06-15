package com.dokdok.user.repository;

import com.dokdok.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findById(Long userId);

    Optional<User> findByKakaoId(Long id);

    Optional<User> findByNickname(String nickname);

    Optional<User> findByUserEmail(String userEmail);
}