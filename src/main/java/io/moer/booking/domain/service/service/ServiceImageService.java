package io.moer.booking.domain.service.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.common.storage.FileStorageService;
import io.moer.booking.domain.service.ServiceImage;
import io.moer.booking.domain.service.dto.ImageSortRequest;
import io.moer.booking.domain.service.dto.ServiceImageResponse;
import io.moer.booking.domain.service.repository.ServiceImageRepository;
import io.moer.booking.domain.service.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 서비스 이미지 관리 Service
 */
@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServiceImageService {

    private static final int MAX_IMAGES_PER_SERVICE = 3;

    private final ServiceImageRepository serviceImageRepository;
    private final ServiceRepository serviceRepository;
    private final FileStorageService fileStorageService;

    /**
     * 서비스 이미지 업로드
     */
    @Transactional
    public ServiceImageResponse uploadImage(Long businessId, Long serviceId, MultipartFile file, String caption) {
        // 1. 서비스 존재 확인
        if (!serviceRepository.existsByBusinessIdAndId(businessId, serviceId)) {
            throw new EntityNotFoundException(ErrorCode.SERVICE_NOT_FOUND);
        }

        // 2. 이미지 수 확인 (최대 3장)
        int currentCount = serviceImageRepository.countByServiceId(serviceId);
        if (currentCount >= MAX_IMAGES_PER_SERVICE) {
            throw new BusinessException(ErrorCode.SERVICE_IMAGE_LIMIT_EXCEEDED);
        }

        // 3. 파일 저장
        String imageUrl = fileStorageService.store(file, "services/" + serviceId);

        // 4. 엔티티 생성 및 저장
        ServiceImage image = ServiceImage.builder()
                .serviceId(serviceId)
                .businessId(businessId)
                .imageUrl(imageUrl)
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .sortOrder(currentCount)
                .caption(caption)
                .build();

        serviceImageRepository.save(image);

        log.info("Service image uploaded: id={}, serviceId={}, businessId={}", image.getId(), serviceId, businessId);

        return ServiceImageResponse.from(image);
    }

    /**
     * 서비스 이미지 목록 조회
     */
    public List<ServiceImageResponse> getImages(Long serviceId) {
        return serviceImageRepository.findByServiceId(serviceId).stream()
                .map(ServiceImageResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 서비스 이미지 삭제
     */
    @Transactional
    public void deleteImage(Long businessId, Long serviceId, Long imageId) {
        ServiceImage image = serviceImageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.SERVICE_IMAGE_NOT_FOUND,
                        "이미지를 찾을 수 없습니다: " + imageId
                ));

        // 파일 삭제
        fileStorageService.delete(image.getImageUrl());

        // DB 삭제
        serviceImageRepository.deleteById(imageId);

        log.info("Service image deleted: id={}, serviceId={}, businessId={}", imageId, serviceId, businessId);
    }

    /**
     * 서비스 이미지 정렬 순서 변경
     */
    @Transactional
    public List<ServiceImageResponse> updateSortOrder(Long businessId, Long serviceId, ImageSortRequest request) {
        // 1. 서비스의 실제 이미지 목록 조회
        List<ServiceImage> existingImages = serviceImageRepository.findByServiceId(serviceId);
        Set<Long> existingImageIds = existingImages.stream()
                .map(ServiceImage::getId)
                .collect(Collectors.toSet());

        // 2. 요청된 이미지 ID와 비교
        Set<Long> requestImageIds = request.getImageOrders().stream()
                .map(ImageSortRequest.ImageOrder::getImageId)
                .collect(Collectors.toSet());

        if (!existingImageIds.equals(requestImageIds)) {
            throw new BusinessException(ErrorCode.SERVICE_IMAGE_ORDER_MISMATCH);
        }

        // 3. 정렬 순서 업데이트
        for (ImageSortRequest.ImageOrder order : request.getImageOrders()) {
            serviceImageRepository.updateSortOrder(order.getImageId(), order.getSortOrder());
        }

        log.info("Service image sort order updated: serviceId={}, businessId={}", serviceId, businessId);

        return getImages(serviceId);
    }
}
