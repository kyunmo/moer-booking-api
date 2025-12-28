package io.moer.booking.domain.staff.service;

import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.staff.Portfolio;
import io.moer.booking.domain.staff.dto.PortfolioCreateRequest;
import io.moer.booking.domain.staff.dto.PortfolioResponse;
import io.moer.booking.domain.staff.repository.PortfolioRepository;
import io.moer.booking.domain.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final StaffRepository staffRepository;

    /**
     * Portfolio 생성
     */
    @Transactional
    public PortfolioResponse createPortfolio(Long staffId, PortfolioCreateRequest request) {
        // Staff 존재 확인 및 businessId 가져오기
        io.moer.booking.domain.staff.Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        // Portfolio 엔티티 생성
        Portfolio portfolio = Portfolio.builder()
                .staffId(staffId)
                .businessId(staff.getBusinessId())
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .tags(request.getTags())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isVisible(true)
                .build();

        // 저장
        portfolioRepository.save(portfolio);

        log.info("Portfolio created: id={}, staffId={}", portfolio.getId(), staffId);

        return PortfolioResponse.from(portfolio);
    }

    /**
     * Staff의 Portfolio 목록 조회
     */
    public List<PortfolioResponse> getPortfoliosByStaff(Long staffId) {
        // Staff 존재 확인
        if (!staffRepository.existsById(staffId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        return portfolioRepository.findByStaffId(staffId).stream()
                .map(PortfolioResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Staff의 공개 Portfolio 목록 조회 (고객용)
     */
    public List<PortfolioResponse> getVisiblePortfoliosByStaff(Long staffId) {
        // Staff 존재 확인
        if (!staffRepository.existsById(staffId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        return portfolioRepository.findByStaffIdAndVisible(staffId, true).stream()
                .map(PortfolioResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Business의 전체 Portfolio 목록 조회
     */
    public List<PortfolioResponse> getPortfoliosByBusiness(Long businessId) {
        return portfolioRepository.findByBusinessId(businessId).stream()
                .map(PortfolioResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Portfolio 공개/비공개 전환
     */
    @Transactional
    public PortfolioResponse togglePortfolioVisibility(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        Portfolio updated = Portfolio.builder()
                .id(portfolio.getId())
                .isVisible(!portfolio.getIsVisible())
                .build();

        portfolioRepository.update(updated);

        log.info("Portfolio visibility toggled: id={}, isVisible={}",
                portfolioId, !portfolio.getIsVisible());

        // 재조회
        return PortfolioResponse.from(
                portfolioRepository.findById(portfolioId)
                        .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND))
        );
    }

    /**
     * Portfolio 삭제
     */
    @Transactional
    public void deletePortfolio(Long portfolioId) {
        // 존재 확인
        if (!portfolioRepository.findById(portfolioId).isPresent()) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        portfolioRepository.delete(portfolioId);

        log.info("Portfolio deleted: id={}", portfolioId);
    }
}