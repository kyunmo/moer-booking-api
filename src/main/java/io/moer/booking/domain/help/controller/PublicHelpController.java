package io.moer.booking.domain.help.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.domain.help.dto.HelpArticleResponse;
import io.moer.booking.domain.help.dto.HelpListResponse;
import io.moer.booking.domain.help.service.HelpArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 도움말 Public Controller
 * 인증 불필요 (/api/public/**)
 */
@RestController
@RequestMapping("/api/public/help")
@RequiredArgsConstructor
@Tag(name = "Help (Public)", description = "인앱 도움말 조회 API (비인증)")
public class PublicHelpController {

    private final HelpArticleService helpArticleService;

    /**
     * 도움말 목록 조회
     * GET /api/public/help?category=reservation&keyword=예약&lang=ko
     */
    @GetMapping
    @Operation(
            summary = "도움말 목록 조회",
            description = "카테고리, 키워드, 언어로 도움말을 검색합니다. 공개된 도움말만 반환됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<ApiResponse<HelpListResponse>> getHelpList(
            @Parameter(description = "카테고리 필터", example = "reservation")
            @RequestParam(required = false) String category,
            @Parameter(description = "키워드 검색", example = "예약")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "언어 코드", example = "ko")
            @RequestParam(required = false, defaultValue = "ko") String lang) {

        HelpListResponse response = helpArticleService.getHelpList(category, keyword, lang);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 도움말 단건 조회
     * GET /api/public/help/{id}
     */
    @GetMapping("/{id}")
    @Operation(
            summary = "도움말 상세 조회",
            description = "도움말 ID로 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "도움말 없음")
    })
    public ResponseEntity<ApiResponse<HelpArticleResponse>> getHelpArticle(
            @PathVariable Long id) {

        HelpArticleResponse response = helpArticleService.getHelpArticle(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
