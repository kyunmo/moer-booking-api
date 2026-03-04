package io.moer.booking.domain.help.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.help.dto.HelpArticleCreateRequest;
import io.moer.booking.domain.help.dto.HelpArticleResponse;
import io.moer.booking.domain.help.dto.HelpListResponse;
import io.moer.booking.domain.help.service.HelpArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 도움말 SuperAdmin Controller
 * SUPER_ADMIN 권한 필요 (/api/superadmin/**)
 */
@RestController
@RequestMapping("/api/superadmin/help")
@RequiredArgsConstructor
@Tag(name = "Help (SuperAdmin)", description = "도움말 관리 API (슈퍼 관리자 전용)")
public class SuperAdminHelpController {

    private final HelpArticleService helpArticleService;

    /**
     * 도움말 생성
     * POST /api/superadmin/help
     */
    @PostMapping
    @Operation(
            summary = "도움말 생성",
            description = "새 도움말 콘텐츠를 생성합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력값"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ApiResponse<HelpArticleResponse>> createArticle(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody HelpArticleCreateRequest request) {

        if (!currentUser.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
        }

        HelpArticleResponse response = helpArticleService.createArticle(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 도움말 목록 조회 (비공개 포함)
     * GET /api/superadmin/help?category=reservation&keyword=예약&lang=ko
     */
    @GetMapping
    @Operation(
            summary = "도움말 목록 조회 (관리자)",
            description = "비공개 도움말을 포함하여 전체 목록을 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ResponseEntity<ApiResponse<HelpListResponse>> getHelpList(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Parameter(description = "카테고리 필터", example = "reservation")
            @RequestParam(required = false) String category,
            @Parameter(description = "키워드 검색", example = "예약")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "언어 코드", example = "ko")
            @RequestParam(required = false) String lang) {

        if (!currentUser.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
        }

        HelpListResponse response = helpArticleService.getHelpListForAdmin(category, keyword, lang);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 도움말 수정
     * PUT /api/superadmin/help/{id}
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "도움말 수정",
            description = "도움말 콘텐츠를 수정합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력값"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "도움말 없음")
    })
    public ResponseEntity<ApiResponse<HelpArticleResponse>> updateArticle(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id,
            @Valid @RequestBody HelpArticleCreateRequest request) {

        if (!currentUser.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
        }

        HelpArticleResponse response = helpArticleService.updateArticle(id, request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 도움말 삭제
     * DELETE /api/superadmin/help/{id}
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "도움말 삭제",
            description = "도움말 콘텐츠를 삭제합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "도움말 없음")
    })
    public ResponseEntity<ApiResponse<Void>> deleteArticle(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PathVariable Long id) {

        if (!currentUser.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.SUPER_ADMIN_REQUIRED);
        }

        helpArticleService.deleteArticle(id);

        return ResponseEntity.ok(ApiResponse.success());
    }
}
