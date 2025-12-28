package io.moer.booking.domain.user.repository;

import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.dto.UserSearchCondition;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UserRepository {

    // 생성
    void save(User user);

    // 조회
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    List<User> findAll(UserSearchCondition condition);
    long countAll(UserSearchCondition condition);

    // 수정
    void update(User user);

    // 삭제
    void delete(Long id);

    // 중복 체크
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("id") Long id);
}