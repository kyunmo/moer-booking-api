package io.moer.booking.domain.inquiry.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.domain.inquiry.dto.InquiryCreateRequest;
import io.moer.booking.domain.inquiry.dto.InquiryResponse;
import io.moer.booking.domain.inquiry.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 문의하기 Controller
 * Public API - 인증 불필요
 */
@RestController
@RequestMapping("/api/public/inquiries")
@RequiredArgsConstructor
@Tag(name = "Inquiry", description = "문의하기 API (비인증)")
public class InquiryController {

    private final InquiryService inquiryService;

    /**
     * 문의 생성
     * POST /api/public/inquiries
     */
    @PostMapping
    @Operation(
            summary = "문의하기",
            description = "문의를 접수합니다. 인증이 필요하지 않습니다. IP당 시간당 5건으로 제한됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "문의 접수 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력값"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "요청 횟수 초과")
    })
    public ResponseEntity<ApiResponse<InquiryResponse>> createInquiry(
            @Valid @RequestBody InquiryCreateRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);
        InquiryResponse response = inquiryService.createInquiry(request, ipAddress);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 클라이언트 IP 주소 추출
     * X-Forwarded-For 헤더 지원 (프록시/로드밸런서 뒤에서 동작)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // X-Forwarded-For: client, proxy1, proxy2 -> 첫 번째가 실제 클라이언트 IP
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }
}
