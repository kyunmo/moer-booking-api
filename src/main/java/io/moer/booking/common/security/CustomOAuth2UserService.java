package io.moer.booking.common.security;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.auth.SnsAccount;
import io.moer.booking.domain.auth.SnsProvider;
import io.moer.booking.domain.auth.dto.SnsUserInfo;
import io.moer.booking.domain.auth.repository.SnsAccountRepository;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.BusinessStatus;
import io.moer.booking.domain.business.BusinessType;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.UserRole;
import io.moer.booking.domain.user.UserStatus;
import io.moer.booking.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * OAuth2 사용자 정보 로드 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final SnsAccountRepository snsAccountRepository;
    private final BusinessRepository businessRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        try {
            // SNS별 사용자 정보 추출
            SnsUserInfo snsUserInfo = extractUserInfo(registrationId, oauth2User);

            // 사용자 찾기 또는 생성
            User user = findOrCreateUser(snsUserInfo);

            // SNS 계정 저장/업데이트
            saveSnsAccount(user, snsUserInfo);

            log.info("OAuth2 login successful: provider={}, userId={}, email={}",
                    snsUserInfo.getProvider(), user.getId(), user.getEmail());

            return new CustomOAuth2User(user, oauth2User.getAttributes());

        } catch (Exception e) {
            log.error("OAuth2 authentication failed: registrationId={}, error={}",
                    registrationId, e.getMessage(), e);
            throw new OAuth2AuthenticationException(e.getMessage());
        }
    }

    /**
     * SNS별 사용자 정보 추출
     */
    private SnsUserInfo extractUserInfo(String registrationId, OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();

        return switch (registrationId.toLowerCase()) {
            case "google" -> extractGoogleUserInfo(attributes);
            case "naver" -> extractNaverUserInfo(attributes);
            case "kakao" -> extractKakaoUserInfo(attributes);
            default -> throw new BusinessException(
                    ErrorCode.OAUTH2_PROVIDER_NOT_FOUND,
                    "지원하지 않는 SNS 제공자입니다: " + registrationId
            );
        };
    }

    /**
     * 구글 사용자 정보 추출
     */
    private SnsUserInfo extractGoogleUserInfo(Map<String, Object> attributes) {
        return SnsUserInfo.builder()
                .provider(SnsProvider.GOOGLE)
                .providerUserId((String) attributes.get("sub"))
                .email((String) attributes.get("email"))
                .name((String) attributes.get("name"))
                .profileImageUrl((String) attributes.get("picture"))
                .build();
    }

    /**
     * 네이버 사용자 정보 추출
     */
    @SuppressWarnings("unchecked")
    private SnsUserInfo extractNaverUserInfo(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) {
            throw new BusinessException(
                    ErrorCode.OAUTH2_AUTHENTICATION_FAILED,
                    "네이버에서 사용자 정보를 가져올 수 없습니다"
            );
        }

        return SnsUserInfo.builder()
                .provider(SnsProvider.NAVER)
                .providerUserId((String) response.get("id"))
                .email((String) response.get("email"))
                .name((String) response.get("name"))
                .profileImageUrl((String) response.get("profile_image"))
                .build();
    }

    /**
     * 카카오 사용자 정보 추출
     */
    @SuppressWarnings("unchecked")
    private SnsUserInfo extractKakaoUserInfo(Map<String, Object> attributes) {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        if (kakaoAccount == null || profile == null) {
            throw new BusinessException(
                    ErrorCode.OAUTH2_AUTHENTICATION_FAILED,
                    "카카오에서 사용자 정보를 가져올 수 없습니다"
            );
        }

        return SnsUserInfo.builder()
                .provider(SnsProvider.KAKAO)
                .providerUserId(String.valueOf(attributes.get("id")))
                .email((String) kakaoAccount.get("email"))
                .name((String) profile.get("nickname"))
                .profileImageUrl((String) profile.get("profile_image_url"))
                .build();
    }

    /**
     * 사용자 찾기 또는 생성
     */
    private User findOrCreateUser(SnsUserInfo snsInfo) {
        // 1. SNS 계정으로 이미 연동된 사용자 찾기
        Optional<SnsAccount> existingSns = snsAccountRepository
                .findByProviderAndProviderUserId(snsInfo.getProvider(), snsInfo.getProviderUserId());
        if (existingSns.isPresent()) {
            return userRepository.findById(existingSns.get().getUserId())
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
        }

        // 2. 이메일로 기존 사용자 찾기
        if (snsInfo.getEmail() != null) {
            Optional<User> existingUser = userRepository.findByEmail(snsInfo.getEmail());
            if (existingUser.isPresent()) {
                log.info("Linking SNS account to existing user: email={}, provider={}",
                        snsInfo.getEmail(), snsInfo.getProvider());
                return existingUser.get();
            }
        }

        // 3. 신규 사용자 생성
        return createNewUser(snsInfo);
    }

    /**
     * 신규 사용자 생성 (SNS 로그인)
     */
    private User createNewUser(SnsUserInfo snsInfo) {
        if (snsInfo.getEmail() == null) {
            throw new BusinessException(
                    ErrorCode.OAUTH2_EMAIL_NOT_PROVIDED,
                    "SNS에서 이메일 정보를 제공하지 않았습니다"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        // User 생성 (30일 체험판 자동 설정)
        User newUser = User.builder()
                .email(snsInfo.getEmail())
                .password(passwordEncoder.encode(UUID.randomUUID().toString())) // 임시 비밀번호
                .name(snsInfo.getName())
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .emailVerified("Y")  // SNS 로그인은 이메일 인증 완료로 간주
                .trialStartedAt(now)
                .trialExpiresAt(now.plusDays(30))
                .isPremium("N")
                .build();
        userRepository.save(newUser);

        // 기본 매장 생성
        Business business = Business.builder()
                .ownerId(newUser.getId())
                .name(snsInfo.getName() + "님의 매장")
                .businessType(BusinessType.BEAUTY_SHOP)  // 기본값
                .status(BusinessStatus.ACTIVE)
                .build();
        businessRepository.save(business);

        // User에 businessId 업데이트
        userRepository.updateBusinessId(newUser.getId(), business.getId());
        newUser.updateBusinessId(business.getId());

        log.info("New user created via SNS login: userId={}, email={}, provider={}",
                newUser.getId(), newUser.getEmail(), snsInfo.getProvider());

        return newUser;
    }

    /**
     * SNS 계정 저장/업데이트
     */
    private void saveSnsAccount(User user, SnsUserInfo snsInfo) {
        Optional<SnsAccount> existing = snsAccountRepository
                .findByProviderAndProviderUserId(snsInfo.getProvider(), snsInfo.getProviderUserId());

        if (existing.isEmpty()) {
            SnsAccount snsAccount = SnsAccount.builder()
                    .userId(user.getId())
                    .provider(snsInfo.getProvider())
                    .providerUserId(snsInfo.getProviderUserId())
                    .email(snsInfo.getEmail())
                    .name(snsInfo.getName())
                    .profileImageUrl(snsInfo.getProfileImageUrl())
                    .build();
            snsAccountRepository.save(snsAccount);

            log.info("SNS account linked: userId={}, provider={}", user.getId(), snsInfo.getProvider());
        }
    }
}
