package io.moer.booking.domain.booking.service;

import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.booking.dto.PublicBusinessDetailResponse;
import io.moer.booking.domain.booking.dto.PublicBusinessListResponse;
import io.moer.booking.domain.booking.dto.PublicBusinessSearchCondition;
import io.moer.booking.domain.booking.dto.SlugCheckResponse;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.service.Service;
import io.moer.booking.domain.service.repository.ServiceRepository;
import io.moer.booking.domain.staff.Portfolio;
import io.moer.booking.domain.staff.Staff;
import io.moer.booking.domain.staff.repository.PortfolioRepository;
import io.moer.booking.domain.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 고객용 Public 매장 서비스
 * 인증 없이 접근 가능한 매장 검색/상세 조회 기능
 */
@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicBusinessService {

    private final BusinessRepository businessRepository;
    private final ServiceRepository serviceRepository;
    private final StaffRepository staffRepository;
    private final PortfolioRepository portfolioRepository;

    /**
     * 슬러그 예약어 목록
     * 시스템에서 사용하는 경로와 충돌을 방지하기 위한 예약어
     */
    private static final Set<String> RESERVED_SLUGS = Set.of(
            "admin", "api", "public", "login", "signup",
            "booking", "dashboard", "settings", "help",
            "support", "about", "contact", "terms", "privacy"
    );

    // ========================================
    // 매장 검색 (Public)
    // ========================================

    /**
     * 매장 검색 (페이징)
     *
     * @param condition 검색 조건 (keyword, businessType, sortBy, page, size)
     * @return 매장 목록 (페이징)
     */
    public PageResponse<PublicBusinessListResponse> searchBusinesses(PublicBusinessSearchCondition condition) {
        int page = condition.getPageOrDefault();
        int size = condition.getSizeOrDefault();
        int offset = condition.getOffset();
        String sortBy = condition.getSortByOrDefault();

        List<Business> businesses = businessRepository.searchPublic(
                condition.getKeyword(),
                condition.getBusinessType(),
                sortBy,
                size,
                offset
        );

        int totalElements = businessRepository.countSearchPublic(
                condition.getKeyword(),
                condition.getBusinessType()
        );

        List<PublicBusinessListResponse> content = businesses.stream()
                .map(PublicBusinessListResponse::from)
                .collect(Collectors.toList());

        return PageResponse.of(content, page, size, totalElements);
    }

    // ========================================
    // 매장 상세 조회 (Public)
    // ========================================

    /**
     * 슬러그로 매장 상세 조회
     * 서비스 목록, 스태프 목록, 포트폴리오 수 포함
     *
     * @param slug 매장 슬러그
     * @return 매장 상세 정보
     */
    public PublicBusinessDetailResponse getBusinessDetail(String slug) {
        // 1. 매장 조회
        Business business = businessRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.BUSINESS_NOT_FOUND,
                        "매장을 찾을 수 없습니다: " + slug
                ));

        // 2. 활성 매장인지 확인
        if (!business.isActive()) {
            throw new EntityNotFoundException(
                    ErrorCode.BUSINESS_NOT_FOUND,
                    "매장을 찾을 수 없습니다: " + slug
            );
        }

        // 3. 활성 서비스 목록 조회
        List<Service> services = serviceRepository.findActiveByBusinessId(business.getId());

        // 4. 활성 스태프 목록 조회
        List<Staff> staffs = staffRepository.findActiveByBusinessId(business.getId());

        // 5. 스태프별 포트폴리오 수 조회
        Map<Long, Integer> portfolioCounts = new HashMap<>();
        if (staffs != null && !staffs.isEmpty()) {
            for (Staff staff : staffs) {
                List<Portfolio> portfolios = portfolioRepository.findVisibleByStaffId(staff.getId());
                portfolioCounts.put(staff.getId(), portfolios != null ? portfolios.size() : 0);
            }
        }

        log.debug("Public business detail: slug={}, services={}, staffs={}",
                slug,
                services != null ? services.size() : 0,
                staffs != null ? staffs.size() : 0);

        return PublicBusinessDetailResponse.from(business, services, staffs, portfolioCounts);
    }

    // ========================================
    // 슬러그 관리
    // ========================================

    /**
     * 슬러그 사용 가능 여부 확인
     *
     * @param slug 확인할 슬러그
     * @return 사용 가능 여부 및 대안 제안
     */
    public SlugCheckResponse checkSlugAvailability(String slug) {
        // 1. 형식 검증
        if (!isValidSlugFormat(slug)) {
            return SlugCheckResponse.unavailable(
                    generateSuggestions(slug)
            );
        }

        // 2. 예약어 검증
        if (RESERVED_SLUGS.contains(slug.toLowerCase())) {
            return SlugCheckResponse.unavailable(
                    generateSuggestions(slug)
            );
        }

        // 3. 중복 검증
        if (businessRepository.existsBySlug(slug)) {
            return SlugCheckResponse.unavailable(
                    generateSuggestions(slug)
            );
        }

        return SlugCheckResponse.available();
    }

    /**
     * 슬러그 변경
     *
     * @param businessId 매장 ID
     * @param slug       새 슬러그
     */
    @Transactional
    public void updateSlug(Long businessId, String slug) {
        // 1. 매장 존재 확인
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.BUSINESS_NOT_FOUND,
                        "매장을 찾을 수 없습니다: " + businessId
                ));

        // 2. 형식 검증
        if (!isValidSlugFormat(slug)) {
            throw new BusinessException(ErrorCode.SLUG_INVALID_FORMAT,
                    "슬러그는 소문자, 숫자, 하이픈만 사용 가능하며, 시작과 끝은 소문자 또는 숫자여야 합니다");
        }

        // 3. 예약어 검증
        if (RESERVED_SLUGS.contains(slug.toLowerCase())) {
            throw new BusinessException(ErrorCode.SLUG_RESERVED_WORD,
                    "사용할 수 없는 슬러그입니다: " + slug);
        }

        // 4. 중복 검증 (자기 자신 제외)
        businessRepository.findBySlug(slug).ifPresent(existing -> {
            if (!existing.getId().equals(businessId)) {
                throw new BusinessException(ErrorCode.SLUG_ALREADY_EXISTS,
                        "이미 사용 중인 슬러그입니다: " + slug);
            }
        });

        // 5. 슬러그 업데이트
        businessRepository.updateSlug(businessId, slug);

        log.info("Business slug updated: businessId={}, slug={}", businessId, slug);
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    /**
     * 슬러그 형식 검증
     * 3~50자, 소문자/숫자/하이픈, 시작과 끝은 소문자 또는 숫자
     */
    private boolean isValidSlugFormat(String slug) {
        if (slug == null || slug.length() < 3 || slug.length() > 50) {
            return false;
        }
        return slug.matches("^[a-z0-9][a-z0-9-]*[a-z0-9]$");
    }

    /**
     * 대안 슬러그 생성
     * 사용 불가 시 숫자를 붙여 제안
     */
    private List<String> generateSuggestions(String baseSlug) {
        List<String> suggestions = new ArrayList<>();
        String cleaned = baseSlug.toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        if (cleaned.length() < 3) {
            cleaned = cleaned + "shop";
        }

        for (int i = 1; i <= 5; i++) {
            String candidate = cleaned + "-" + i;
            if (!businessRepository.existsBySlug(candidate) && !RESERVED_SLUGS.contains(candidate)) {
                suggestions.add(candidate);
                if (suggestions.size() >= 3) {
                    break;
                }
            }
        }

        // 랜덤 숫자로도 추가 제안
        if (suggestions.size() < 3) {
            int random = (int) (Math.random() * 900) + 100;
            String candidate = cleaned + "-" + random;
            if (!businessRepository.existsBySlug(candidate) && !RESERVED_SLUGS.contains(candidate)) {
                suggestions.add(candidate);
            }
        }

        return suggestions;
    }
}
