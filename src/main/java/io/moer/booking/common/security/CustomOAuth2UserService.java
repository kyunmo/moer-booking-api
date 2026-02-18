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
import io.moer.booking.domain.business.SubscriptionPlan;
import io.moer.booking.domain.business.SubscriptionStatus;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.UserRole;
import io.moer.booking.domain.user.UserStatus;
import io.moer.booking.domain.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * OAuth2 사용자 정보 로드 서비스
 *
 * loginType 쿠키 기반으로 고객/관리자 분기:
 * - loginType=customer: CUSTOMER 역할, 매장 미생성, 체험판 미설정
 * - loginType=admin 또는 미지정: OWNER 역할, 매장 자동 생성, 30일 체험판
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

            // loginType 쿠키 읽기
            String loginType = getLoginTypeFromCookie();

            // 사용자 찾기 또는 생성
            User user = findOrCreateUser(snsUserInfo, loginType);

            // SNS 계정 저장/업데이트
            saveSnsAccount(user, snsUserInfo);

            log.info("OAuth2 login successful: provider={}, userId={}, email={}, loginType={}",
                    snsUserInfo.getProvider(), user.getId(), user.getEmail(), loginType);

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
     * 기존 사용자가 있으면 역할을 변경하지 않고 그대로 반환한다.
     * 단, CUSTOMER가 loginType=admin으로 접근하면 에러 처리한다.
     */
    private User findOrCreateUser(SnsUserInfo snsInfo, String loginType) {
        // 1. SNS 계정으로 이미 연동된 사용자 찾기
        Optional<SnsAccount> existingSns = snsAccountRepository
                .findByProviderAndProviderUserId(snsInfo.getProvider(), snsInfo.getProviderUserId());
        if (existingSns.isPresent()) {
            User user = userRepository.findById(existingSns.get().getUserId())
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
            validateRoleAndLoginType(user, loginType);
            return user;
        }

        // 2. 이메일로 기존 사용자 찾기
        if (snsInfo.getEmail() != null) {
            Optional<User> existingUser = userRepository.findByEmail(snsInfo.getEmail());
            if (existingUser.isPresent()) {
                validateRoleAndLoginType(existingUser.get(), loginType);
                log.info("Linking SNS account to existing user: email={}, provider={}",
                        snsInfo.getEmail(), snsInfo.getProvider());
                return existingUser.get();
            }
        }

        // 3. 신규 사용자 생성 (loginType에 따라 분기)
        return createNewUser(snsInfo, loginType);
    }

    /**
     * loginType과 사용자 역할의 호환성을 검증한다.
     * CUSTOMER 계정이 loginType=admin으로 접근하면 차단한다.
     */
    private void validateRoleAndLoginType(User user, String loginType) {
        boolean isAdminLogin = !"customer".equals(loginType);
        if (isAdminLogin && user.getRole() == UserRole.CUSTOMER) {
            log.warn("CUSTOMER user attempted admin login: userId={}, email={}",
                    user.getId(), user.getEmail());
            throw new BusinessException(ErrorCode.OAUTH2_ROLE_MISMATCH);
        }
    }

    /**
     * 신규 사용자 생성 (SNS 로그인)
     *
     * loginType에 따라 역할과 초기 설정이 달라진다:
     * - "customer": CUSTOMER 역할, 매장 미생성, 체험판 미설정
     * - 그 외 (null, "admin" 등): OWNER 역할, 매장 자동 생성, 30일 체험판
     */
    private User createNewUser(SnsUserInfo snsInfo, String loginType) {
        if (snsInfo.getEmail() == null) {
            throw new BusinessException(
                    ErrorCode.OAUTH2_EMAIL_NOT_PROVIDED,
                    "SNS에서 이메일 정보를 제공하지 않았습니다"
            );
        }

        if ("customer".equals(loginType)) {
            return createCustomerUser(snsInfo);
        } else {
            return createOwnerUser(snsInfo);
        }
    }

    /**
     * 고객(CUSTOMER) 사용자 생성
     * - CUSTOMER 역할
     * - 매장 미생성
     * - 체험판 미설정
     * - 마케팅 수신 동의: N
     */
    private User createCustomerUser(SnsUserInfo snsInfo) {
        User newUser = User.builder()
                .email(snsInfo.getEmail())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .name(snsInfo.getName())
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .emailVerified("Y")
                .marketingAgree("N")
                .isPremium("N")
                .build();
        userRepository.save(newUser);

        log.info("New CUSTOMER created via SNS login: userId={}, email={}, provider={}",
                newUser.getId(), newUser.getEmail(), snsInfo.getProvider());

        return newUser;
    }

    /**
     * 사장님(OWNER) 사용자 생성
     * - OWNER 역할
     * - 기본 매장 자동 생성
     * - 30일 체험판 자동 설정
     */
    private User createOwnerUser(SnsUserInfo snsInfo) {
        LocalDateTime now = LocalDateTime.now();

        // User 생성 (30일 체험판 자동 설정)
        User newUser = User.builder()
                .email(snsInfo.getEmail())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .name(snsInfo.getName())
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .emailVerified("Y")
                .trialStartedAt(now)
                .trialExpiresAt(now.plusDays(30))
                .isPremium("N")
                .build();
        userRepository.save(newUser);

        // 기본 매장 생성 (FREE 플랜 + TRIAL 상태)
        Business business = Business.builder()
                .ownerId(newUser.getId())
                .name(snsInfo.getName() + "님의 매장")
                .businessType(BusinessType.BEAUTY_SHOP)
                .status(BusinessStatus.ACTIVE)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .subscriptionStatus(SubscriptionStatus.TRIAL)
                .trialStartedAt(now)
                .trialEndsAt(now.plusDays(30))
                .currentStaffCount(0)
                .currentMonthReservationCount(0)
                .build();
        businessRepository.save(business);

        // User에 businessId 업데이트
        userRepository.updateBusinessId(newUser.getId(), business.getId());
        newUser.updateBusinessId(business.getId());

        log.info("New OWNER created via SNS login: userId={}, email={}, provider={}, businessId={}",
                newUser.getId(), newUser.getEmail(), snsInfo.getProvider(), business.getId());

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

    /**
     * moer_login_type 쿠키에서 loginType 값을 읽는다.
     *
     * @return loginType 값 (없으면 null)
     */
    private String getLoginTypeFromCookie() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                if (request.getCookies() != null) {
                    for (Cookie cookie : request.getCookies()) {
                        if ("moer_login_type".equals(cookie.getName())) {
                            return cookie.getValue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read loginType cookie: {}", e.getMessage());
        }
        return null;
    }
}
