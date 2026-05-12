package io.moer.booking.domain.inquiry.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.common.util.HtmlSanitizer;
import io.moer.booking.common.util.MaskingUtils;
import io.moer.booking.domain.inquiry.Inquiry;
import io.moer.booking.domain.inquiry.InquiryStatus;
import io.moer.booking.domain.inquiry.dto.InquiryCreateRequest;
import io.moer.booking.domain.inquiry.dto.InquiryResponse;
import io.moer.booking.domain.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 문의 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;

    /** IP당 최대 문의 건수 (1시간 기준) */
    private static final int RATE_LIMIT_MAX_REQUESTS = 5;

    /** Rate Limit 시간 범위 (시간) */
    private static final int RATE_LIMIT_HOURS = 1;

    /**
     * 문의 생성
     *
     * @param request   문의 생성 요청
     * @param ipAddress 문의자 IP 주소
     * @return 문의 응답
     */
    @Transactional
    public InquiryResponse createInquiry(InquiryCreateRequest request, String ipAddress) {
        // SECURITY (P1-7): PII 로그 마스킹
        log.info("Creating inquiry: name={}, email={}, type={}, ip={}",
                MaskingUtils.maskName(request.getName()),
                MaskingUtils.maskEmail(request.getEmail()),
                request.getType(), ipAddress);

        // 1. Rate Limit 체크
        checkRateLimit(ipAddress);

        // 2. Inquiry 엔티티 생성
        // SECURITY (P1-5): 사용자 입력 텍스트는 HTML 태그 제거 후 저장 (XSS 방어)
        Inquiry inquiry = Inquiry.builder()
                .name(HtmlSanitizer.plainText(request.getName()))
                .email(request.getEmail())
                .phone(request.getPhone())
                .type(request.getType())
                .content(HtmlSanitizer.plainText(request.getContent()))
                .status(InquiryStatus.PENDING)
                .ipAddress(ipAddress)
                .build();

        // 3. 저장
        inquiryRepository.save(inquiry);

        log.info("Inquiry created: id={}, type={}", inquiry.getId(), request.getType());

        // 4. TODO: 관리자 이메일 알림 발송
        // emailService.sendAdminNotification(inquiry);
        log.info("[TODO] Admin notification email should be sent for inquiry id={}", inquiry.getId());

        // 5. TODO: 문의자에게 접수 확인 이메일 발송
        // emailService.sendInquiryConfirmation(inquiry);
        log.info("[TODO] Confirmation email should be sent to {} for inquiry id={}",
                MaskingUtils.maskEmail(request.getEmail()), inquiry.getId());

        // 6. 응답 반환
        return InquiryResponse.from(inquiry);
    }

    /**
     * Rate Limit 체크
     * 동일 IP에서 최근 1시간 내 5건 이상 문의 시 예외 발생
     */
    private void checkRateLimit(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return; // IP를 알 수 없는 경우 제한하지 않음
        }

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(RATE_LIMIT_HOURS);
        int recentCount = inquiryRepository.countByIpAddressAndCreatedAtAfter(ipAddress, oneHourAgo);

        if (recentCount >= RATE_LIMIT_MAX_REQUESTS) {
            log.warn("Rate limit exceeded: ip={}, count={}", ipAddress, recentCount);
            throw new BusinessException(
                    ErrorCode.INQUIRY_RATE_LIMIT_EXCEEDED,
                    "IP: " + ipAddress + ", 최근 1시간 내 " + recentCount + "건"
            );
        }
    }
}
