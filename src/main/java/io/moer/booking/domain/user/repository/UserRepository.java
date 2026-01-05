package io.moer.booking.domain.user.repository;

import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.UserStatus;
import io.moer.booking.domain.user.dto.UserSearchCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface UserRepository {

    List<User> findAll();

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    void save(User user);

    void update(User user);

    void updateStatus(@Param("id") Long id, @Param("status") UserStatus status);

    void updateLastLoginAt(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    List<User> search(UserSearchCondition condition);

    long countSearch(UserSearchCondition condition);

    void updateBusinessId(@Param("userId") Long userId, @Param("businessId") Long businessId);
}