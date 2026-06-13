package com.project.modules.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.project.modules.user.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("select u from User u where :keyword is null or lower(u.username) like lower(concat('%',:keyword,'%')) or lower(u.fullName) like lower(concat('%',:keyword,'%')) or lower(u.email) like lower(concat('%',:keyword,'%'))")
    Page<User> search(String keyword, Pageable pageable);
}
