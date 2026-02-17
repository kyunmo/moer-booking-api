package io.moer.booking.domain.user.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.common.security.CustomUserDetails;
import io.moer.booking.domain.user.dto.CustomerProfileResponse;
import io.moer.booking.domain.user.dto.CustomerProfileUpdateRequest;
import io.moer.booking.domain.user.service.CustomerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/profile")
@RequiredArgsConstructor
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        CustomerProfileResponse response = customerProfileService.getProfile(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CustomerProfileUpdateRequest request) {
        CustomerProfileResponse response = customerProfileService.updateProfile(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
