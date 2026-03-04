package io.moer.booking.domain.business.repository;

import io.moer.booking.domain.business.BusinessSettings;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Mapper
public interface BusinessSettingsRepository {

    // 생성
    void save(BusinessSettings settings);

    // 조회
    Optional<BusinessSettings> findByBusinessId(Long businessId);

    // 수정
    void update(BusinessSettings settings);

    // 삭제
    void deleteByBusinessId(Long businessId);

    // 카카오 알림톡 설정
    void updateKakaoAlimtalkSettings(@Param("businessId") Long businessId,
                                      @Param("kakaoEnabled") String kakaoEnabled,
                                      @Param("kakaoChannelId") String kakaoChannelId,
                                      @Param("kakaoSenderId") String kakaoSenderId,
                                      @Param("kakaoAlimtalkTriggers") Map<String, Object> kakaoAlimtalkTriggers,
                                      @Param("kakaoVerifiedAt") LocalDateTime kakaoVerifiedAt);

    // 온보딩
    void updateOnboardingStep(@Param("businessId") Long businessId,
                              @Param("column") String column,
                              @Param("value") String value);

    void completeOnboarding(@Param("businessId") Long businessId);

    void skipOnboarding(@Param("businessId") Long businessId);
}