package io.moer.booking.domain.bookmark.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.bookmark.dto.BookmarkResponse;
import io.moer.booking.domain.bookmark.service.CustomerBookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Customer Bookmark", description = "고객 즐겨찾기 API")
public class CustomerBookmarkController {

    private final CustomerBookmarkService bookmarkService;

    @PostMapping("/{businessId}")
    @Operation(summary = "즐겨찾기 추가", description = "매장을 즐겨찾기에 추가합니다")
    public ResponseEntity<ApiResponse<Void>> addBookmark(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId) {
        bookmarkService.addBookmark(userDetails.getUserId(), businessId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success());
    }

    @DeleteMapping("/{businessId}")
    @Operation(summary = "즐겨찾기 해제", description = "매장을 즐겨찾기에서 해제합니다")
    public ResponseEntity<ApiResponse<Void>> removeBookmark(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId) {
        bookmarkService.removeBookmark(userDetails.getUserId(), businessId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping
    @Operation(summary = "즐겨찾기 목록", description = "즐겨찾기한 매장 목록을 조회합니다")
    public ApiResponse<List<BookmarkResponse>> getBookmarks(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<BookmarkResponse> response = bookmarkService.getBookmarks(userDetails.getUserId());
        return ApiResponse.success(response);
    }

    @GetMapping("/{businessId}/check")
    @Operation(summary = "즐겨찾기 여부 확인", description = "특정 매장의 즐겨찾기 여부를 확인합니다")
    public ApiResponse<Map<String, Boolean>> checkBookmark(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long businessId) {
        Map<String, Boolean> response = bookmarkService.checkBookmark(userDetails.getUserId(), businessId);
        return ApiResponse.success(response);
    }
}
