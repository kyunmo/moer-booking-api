package io.moer.booking.domain.staff.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.common.storage.FileStorageService;
import io.moer.booking.domain.staff.Portfolio;
import io.moer.booking.domain.staff.Staff;
import io.moer.booking.domain.staff.dto.PortfolioCreateRequest;
import io.moer.booking.domain.staff.dto.PortfolioResponse;
import io.moer.booking.domain.staff.repository.PortfolioRepository;
import io.moer.booking.domain.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 포트폴리오 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final StaffRepository staffRepository;
    private final FileStorageService fileStorageService;

    /**
     * 포트폴리오 생성 (이미지 파일 업로드 방식)
     */
    @Transactional
    public PortfolioResponse createPortfolioWithImage(Long businessId, Long staffId,
            MultipartFile image, String title, String description, String serviceCategory) {
        // Staff 존재 & business 확인
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND));

        if (!staff.getBusinessId().equals(businessId)) {
            throw new BusinessException(ErrorCode.STAFF_ACCESS_DENIED,
                    "해당 직원에 접근 권한이 없습니다");
        }

        if (image == null || image.isEmpty()) {
            throw new BusinessException(ErrorCode.PORTFOLIO_IMAGE_REQUIRED);
        }

        String imageUrl = fileStorageService.store(image, "portfolios");

        Portfolio portfolio = Portfolio.builder()
                .staffId(staffId)
                .title(title)
                .description(description)
                .imageUrl(imageUrl)
                .serviceCategory(serviceCategory)
                .sortOrder(0)
                .isVisible("Y")
                .build();

        portfolioRepository.save(portfolio);

        log.info("Portfolio created with image: id={}, staffId={}, imageUrl={}",
                portfolio.getId(), staffId, imageUrl);

        Portfolio saved = portfolioRepository.findById(portfolio.getId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.PORTFOLIO_NOT_FOUND));

        return PortfolioResponse.from(saved);
    }

    /**
     * 포트폴리오 생성 (JSON body 방식)
     */
    @Transactional
    public PortfolioResponse createPortfolio(Long staffId, PortfolioCreateRequest request) {
        // Staff 존재 확인
        if (!staffRepository.existsById(staffId)) {
            throw new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND);
        }

        // tags List<String> → String 변환
        String tagsString = Portfolio.tagsToString(request.getTags());

        Portfolio portfolio = Portfolio.builder()
                .staffId(staffId)
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .tags(tagsString)
                .isVisible("Y")
                .build();

        portfolioRepository.save(portfolio);

        log.info("Portfolio created: id={}, staffId={}", portfolio.getId(), staffId);

        // 생성된 Portfolio 다시 조회 (businessId 포함)
        Portfolio saved = portfolioRepository.findById(portfolio.getId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.PORTFOLIO_NOT_FOUND));

        return PortfolioResponse.from(saved);
    }

    /**
     * Staff의 전체 Portfolio 조회 (관리자용)
     */
    public List<PortfolioResponse> getPortfoliosByStaff(Long staffId) {
        return portfolioRepository.findByStaffId(staffId).stream()
                .map(PortfolioResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Staff의 전체 Portfolio 조회 (businessId 검증 포함)
     */
    public List<PortfolioResponse> getPortfoliosByStaffWithAuth(Long businessId, Long staffId) {
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND));

        if (!staff.getBusinessId().equals(businessId)) {
            throw new BusinessException(ErrorCode.STAFF_ACCESS_DENIED,
                    "해당 직원에 접근 권한이 없습니다");
        }

        return portfolioRepository.findByStaffId(staffId).stream()
                .map(PortfolioResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Staff의 공개 Portfolio 조회 (고객용)
     */
    public List<PortfolioResponse> getVisiblePortfoliosByStaff(Long staffId) {
        return portfolioRepository.findVisibleByStaffId(staffId).stream()
                .map(PortfolioResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Business의 전체 Portfolio 조회
     */
    public List<PortfolioResponse> getPortfoliosByBusiness(Long businessId) {
        return portfolioRepository.findByBusinessId(businessId).stream()
                .map(PortfolioResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 포트폴리오 공개/비공개 전환
     */
    @Transactional
    public PortfolioResponse togglePortfolioVisibility(Long portfolioId) {
        if (!portfolioRepository.existsById(portfolioId)) {
            throw new EntityNotFoundException(ErrorCode.PORTFOLIO_NOT_FOUND);
        }

        portfolioRepository.toggleVisibility(portfolioId);

        log.info("Portfolio visibility toggled: id={}", portfolioId);

        // 다시 조회
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.PORTFOLIO_NOT_FOUND));

        return PortfolioResponse.from(portfolio);
    }

    /**
     * 포트폴리오 삭제 (businessId 검증 포함)
     */
    @Transactional
    public void deletePortfolioWithAuth(Long businessId, Long staffId, Long portfolioId) {
        // Staff 존재 & business 확인
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND));

        if (!staff.getBusinessId().equals(businessId)) {
            throw new BusinessException(ErrorCode.STAFF_ACCESS_DENIED,
                    "해당 직원에 접근 권한이 없습니다");
        }

        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 포트폴리오가 해당 스태프 소유인지 확인
        if (!portfolio.getStaffId().equals(staffId)) {
            throw new BusinessException(ErrorCode.STAFF_ACCESS_DENIED,
                    "해당 포트폴리오에 접근 권한이 없습니다");
        }

        // 파일 삭제
        if (portfolio.getImageUrl() != null) {
            fileStorageService.delete(portfolio.getImageUrl());
        }

        portfolioRepository.delete(portfolioId);

        log.info("Portfolio deleted with file cleanup: id={}, staffId={}", portfolioId, staffId);
    }

    /**
     * 포트폴리오 삭제
     */
    @Transactional
    public void deletePortfolio(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 파일 삭제
        if (portfolio.getImageUrl() != null) {
            fileStorageService.delete(portfolio.getImageUrl());
        }

        portfolioRepository.delete(portfolioId);

        log.info("Portfolio deleted: id={}", portfolioId);
    }
}