package io.moer.booking.domain.broadcast.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.broadcast.dto.BroadcastCreateRequest;
import io.moer.booking.domain.broadcast.dto.BroadcastResponse;
import io.moer.booking.domain.broadcast.service.BroadcastService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin/broadcasts")
@RequiredArgsConstructor
@Tag(name = "Broadcast", description = "전체 공지 방송 API (슈퍼 관리자 전용)")
public class BroadcastController {

    private final BroadcastService broadcastService;

    @PostMapping
    @Operation(summary = "공지 발송", description = "전체/특정 대상에게 공지를 발송합니다")
    public ResponseEntity<ApiResponse<BroadcastResponse>> sendBroadcast(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody BroadcastCreateRequest request) {
        BroadcastResponse response = broadcastService.createAndSendBroadcast(
                userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "발송 내역 조회", description = "공지 발송 내역을 조회합니다")
    public ApiResponse<List<BroadcastResponse>> getBroadcasts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<BroadcastResponse> response = broadcastService.getBroadcasts(page, size);
        return ApiResponse.success(response);
    }
}
