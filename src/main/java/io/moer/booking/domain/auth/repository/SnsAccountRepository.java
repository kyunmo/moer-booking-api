package io.moer.booking.domain.auth.repository;

import io.moer.booking.domain.auth.SnsAccount;
import io.moer.booking.domain.auth.SnsProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * SNS 계정 Repository
 */
@Mapper
public interface SnsAccountRepository {

    /**
     * SNS 계정 저장
     */
    void save(SnsAccount snsAccount);

    /**
     * 제공자와 제공자 사용자 ID로 조회
     */
    Optional<SnsAccount> findByProviderAndProviderUserId(
            @Param("provider") SnsProvider provider,
            @Param("providerUserId") String providerUserId
    );

    /**
     * 사용자 ID로 모든 연동된 SNS 계정 조회
     */
    List<SnsAccount> findByUserId(@Param("userId") Long userId);

    /**
     * 이메일로 조회
     */
    List<SnsAccount> findByEmail(@Param("email") String email);

    /**
     * SNS 계정 삭제
     */
    void delete(@Param("id") Long id);

    /**
     * 사용자 ID로 모든 SNS 계정 삭제
     */
    void deleteByUserId(@Param("userId") Long userId);
}
