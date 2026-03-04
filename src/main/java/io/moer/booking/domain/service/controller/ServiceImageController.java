package io.moer.booking.domain.service.controller;

import io.moer.booking.common.dto.ApiResponse;
import io.moer.booking.domain.service.dto.ImageSortRequest;
import io.moer.booking.domain.service.dto.ServiceImageResponse;
import io.moer.booking.domain.service.service.ServiceImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/businesses/{businessId}/services/{serviceId}/images")
@RequiredArgsConstructor
@Tag(name = "Service Image", description = "서비스 이미지 관리 API")
public class ServiceImageController {

    private final ServiceImageService serviceImageService;

    /**
     * 서비스 이미지 업로드
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "서비스 이미지 업로드", description = "서비스에 이미지를 업로드합니다. 최대 3장까지 등록 가능합니다.")
    public ResponseEntity<ApiResponse<ServiceImageResponse>> uploadImage(
            @PathVariable Long businessId,
            @PathVariable Long serviceId,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "caption", required = false) String caption) {

        ServiceImageResponse response = serviceImageService.uploadImage(businessId, serviceId, file, caption);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 서비스 이미지 목록 조회
     */
    @GetMapping
    @Operation(summary = "서비스 이미지 목록 조회", description = "서비스에 등록된 이미지 목록을 조회합니다.")
    public ApiResponse<List<ServiceImageResponse>> getImages(
            @PathVariable Long businessId,
            @PathVariable Long serviceId) {

        List<ServiceImageResponse> response = serviceImageService.getImages(serviceId);
        return ApiResponse.success(response);
    }

    /**
     * 서비스 이미지 삭제
     */
    @DeleteMapping("/{imageId}")
    @Operation(summary = "서비스 이미지 삭제", description = "서비스 이미지를 삭제합니다.")
    public ApiResponse<Void> deleteImage(
            @PathVariable Long businessId,
            @PathVariable Long serviceId,
            @PathVariable Long imageId) {

        serviceImageService.deleteImage(businessId, serviceId, imageId);
        return ApiResponse.success();
    }

    /**
     * 서비스 이미지 정렬 순서 변경
     */
    @PatchMapping("/sort")
    @Operation(summary = "서비스 이미지 정렬 순서 변경", description = "서비스 이미지의 정렬 순서를 변경합니다.")
    public ApiResponse<List<ServiceImageResponse>> updateSortOrder(
            @PathVariable Long businessId,
            @PathVariable Long serviceId,
            @Valid @RequestBody ImageSortRequest request) {

        List<ServiceImageResponse> response = serviceImageService.updateSortOrder(businessId, serviceId, request);
        return ApiResponse.success(response);
    }
}
