package io.moer.booking.domain.review.service;

import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.customer.Customer;
import io.moer.booking.domain.customer.repository.CustomerRepository;
import io.moer.booking.domain.notification.dto.SseEventData;
import io.moer.booking.domain.notification.service.SseEmitterService;
import io.moer.booking.domain.reservation.Reservation;
import io.moer.booking.domain.reservation.ReservationStatus;
import io.moer.booking.domain.reservation.repository.ReservationRepository;
import io.moer.booking.domain.review.Review;
import io.moer.booking.domain.review.ReviewImage;
import io.moer.booking.domain.review.ReviewStatus;
import io.moer.booking.domain.review.dto.*;
import io.moer.booking.domain.review.repository.ReviewImageRepository;
import io.moer.booking.domain.review.repository.ReviewRepository;
import io.moer.booking.domain.staff.Staff;
import io.moer.booking.domain.staff.repository.StaffRepository;
import io.moer.booking.common.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import io.moer.booking.common.dto.PageResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 리뷰 서비스
 * 리뷰 생성, 조회, 답변, 삭제 및 통계 관리
 */
@Slf4j
@Service("reviewServiceBean")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final BusinessRepository businessRepository;
    private final ReservationRepository reservationRepository;
    private final StaffRepository staffRepository;
    private final CustomerRepository customerRepository;
    private final FileStorageService fileStorageService;
    private final SseEmitterService sseEmitterService;

    // ========================================
    // 1. 리뷰 작성 (Public)
    // ========================================

    /**
     * 리뷰 작성
     * - 예약번호 + 전화번호로 본인 확인
     * - 예약 COMPLETED 상태 확인
     * - 중복 리뷰 확인
     * - 리뷰 생성 후 매장 평점 통계 갱신
     *
     * @param slug    매장 슬러그 (Public API에서 전달, 실제 검증은 reservationNumber 기반)
     * @param request 리뷰 작성 요청
     * @return 작성된 리뷰 응답
     */
    @Transactional
    public ReviewResponse createReview(String slug, ReviewCreateRequest request) {
        // 1. 예약 조회
        Reservation reservation = reservationRepository.findByReservationNumber(request.getReservationNumber())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.RESERVATION_NOT_FOUND,
                        "예약을 찾을 수 없습니다: " + request.getReservationNumber()));

        // 2. 전화번호로 본인 확인
        Customer customer = customerRepository.findById(reservation.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND));

        if (!request.getPhone().equals(customer.getPhone())) {
            throw new BusinessException(ErrorCode.REVIEW_RESERVATION_MISMATCH,
                    "예약 정보가 일치하지 않습니다");
        }

        // 3. 예약 상태 COMPLETED 확인
        if (reservation.getStatus() != ReservationStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_COMPLETED,
                    "완료된 예약만 리뷰를 작성할 수 있습니다");
        }

        // 4. 중복 리뷰 확인
        if (reviewRepository.existsByReservationId(reservation.getId())) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS,
                    "이미 리뷰가 작성된 예약입니다");
        }

        // 5. Review 생성
        Long staffId = request.getStaffId() != null ? request.getStaffId() : reservation.getStaffId();

        Review review = Review.builder()
                .businessId(reservation.getBusinessId())
                .reservationId(reservation.getId())
                .customerId(reservation.getCustomerId())
                .staffId(staffId)
                .customerName(customer.getName())
                .customerPhone(customer.getPhone())
                .rating(request.getRating())
                .content(request.getContent())
                .status(ReviewStatus.ACTIVE)
                .build();

        reviewRepository.save(review);

        log.info("Review created: id={}, businessId={}, reservationId={}, rating={}",
                review.getId(), review.getBusinessId(), review.getReservationId(), review.getRating());

        // 6. 매장 평점 통계 갱신
        updateBusinessRatingStats(reservation.getBusinessId());

        // 7. SSE 실시간 이벤트 발송
        String serviceName = getServiceNameFromReservation(reservation);
        String staffName = getStaffName(staffId);
        String contentPreview = review.getContent() != null && review.getContent().length() > 50
                ? review.getContent().substring(0, 50) + "..."
                : review.getContent();

        sseEmitterService.sendEventToBusinessOwner(reservation.getBusinessId(), "REVIEW_CREATED", SseEventData.builder()
                .type("REVIEW_CREATED")
                .referenceId(review.getId())
                .customerName(customer.getName())
                .serviceName(serviceName)
                .rating(review.getRating())
                .contentPreview(contentPreview)
                .message("새 리뷰가 등록되었습니다.")
                .createdAt(LocalDateTime.now())
                .build());

        // 8. 응답 생성
        return ReviewResponse.from(review, serviceName, staffName);
    }

    // ========================================
    // 1-2. 리뷰 작성 (로그인 고객)
    // ========================================

    /**
     * 로그인 고객 리뷰 작성
     * - JWT 인증된 userId + reservationNumber로 본인 예약 확인
     * - 전화번호 검증 불필요
     * - 예약 COMPLETED 상태 확인
     * - 중복 리뷰 확인
     * - 리뷰 생성 후 매장 평점 통계 갱신
     *
     * @param slug   매장 슬러그
     * @param userId 인증된 사용자 ID
     * @param request 리뷰 작성 요청
     * @return 작성된 리뷰 응답
     */
    @Transactional
    public ReviewResponse createReviewByCustomer(String slug, Long userId, CustomerReviewCreateRequest request) {
        // 1. 매장 조회
        Business business = businessRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND,
                        "매장을 찾을 수 없습니다: " + slug));

        // 2. 예약 조회 (userId + reservationNumber로 본인 예약 확인)
        Reservation reservation = reservationRepository.findByUserIdAndReservationNumber(userId, request.getReservationNumber())
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_REVIEW_UNAUTHORIZED,
                        "본인의 예약에 대해서만 리뷰를 작성할 수 있습니다"));

        // 3. 해당 매장의 예약인지 확인
        if (!reservation.getBusinessId().equals(business.getId())) {
            throw new BusinessException(ErrorCode.REVIEW_RESERVATION_MISMATCH,
                    "해당 매장의 예약이 아닙니다");
        }

        // 4. 완료된 예약인지 확인
        if (!reservation.isCompleted()) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_COMPLETED,
                    "완료된 예약만 리뷰를 작성할 수 있습니다");
        }

        // 5. 중복 리뷰 확인
        if (reviewRepository.existsByReservationId(reservation.getId())) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS,
                    "이미 리뷰가 작성된 예약입니다");
        }

        // 6. 고객 정보 조회
        Customer customer = customerRepository.findById(reservation.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND));

        // 7. 리뷰 생성
        Long staffId = request.getStaffId() != null ? request.getStaffId() : reservation.getStaffId();

        Review review = Review.builder()
                .businessId(business.getId())
                .reservationId(reservation.getId())
                .customerId(customer.getId())
                .staffId(staffId)
                .customerName(customer.getName())
                .customerPhone(customer.getPhone())
                .rating(request.getRating())
                .content(request.getContent())
                .status(ReviewStatus.ACTIVE)
                .build();

        reviewRepository.save(review);

        log.info("Customer review created: userId={}, reservationNumber={}, rating={}",
                userId, request.getReservationNumber(), request.getRating());

        // 8. 매장 평점 통계 갱신
        updateBusinessRatingStats(business.getId());

        // 9. SSE 실시간 이벤트 발송
        String serviceName = getServiceNameFromReservation(reservation);
        String staffName = getStaffName(staffId);
        String contentPreview = review.getContent() != null && review.getContent().length() > 50
                ? review.getContent().substring(0, 50) + "..."
                : review.getContent();

        sseEmitterService.sendEventToBusinessOwner(business.getId(), "REVIEW_CREATED", SseEventData.builder()
                .type("REVIEW_CREATED")
                .referenceId(review.getId())
                .customerName(customer.getName())
                .serviceName(serviceName)
                .rating(review.getRating())
                .contentPreview(contentPreview)
                .message("새 리뷰가 등록되었습니다.")
                .createdAt(LocalDateTime.now())
                .build());

        // 10. 응답 생성
        return ReviewResponse.from(review, serviceName, staffName, business.getName(), business.getSlug());
    }

    // ========================================
    // 1-2-1. 리뷰 작성 + 이미지 동시 업로드 (로그인 고객)
    // ========================================

    /**
     * 로그인 고객 리뷰 작성 + 이미지 동시 업로드
     * - 기존 createReviewByCustomer() 로직을 재사용하여 리뷰 생성
     * - 이미지가 있으면 FileStorageService로 저장 후 ReviewImage 레코드 생성
     * - 리뷰당 최대 5개 이미지 제한
     *
     * @param slug   매장 슬러그
     * @param userId 인증된 사용자 ID
     * @param request 리뷰 작성 요청
     * @param images 업로드 이미지 파일 목록 (nullable)
     * @return 작성된 리뷰 응답 (이미지 URL 포함)
     */
    @Transactional
    public ReviewResponse createReviewWithImages(String slug, Long userId,
                                                  CustomerReviewCreateRequest request,
                                                  List<MultipartFile> images) {
        // 1. 이미지 개수 제한 검증
        if (images != null && images.size() > 5) {
            throw new BusinessException(ErrorCode.REVIEW_IMAGE_LIMIT_EXCEEDED,
                    "리뷰당 최대 5개의 이미지만 등록할 수 있습니다");
        }

        // 2. 기존 로직으로 리뷰 생성
        ReviewResponse response = createReviewByCustomer(slug, userId, request);

        // 3. 이미지 저장
        if (images != null && !images.isEmpty()) {
            List<String> imageUrls = new ArrayList<>();
            int sortOrder = 0;

            for (MultipartFile file : images) {
                if (file.isEmpty()) {
                    continue;
                }

                // FileStorageService로 파일 저장
                String imageUrl = fileStorageService.store(file, "reviews/" + response.getId());

                // ReviewImage 레코드 생성
                ReviewImage image = ReviewImage.builder()
                        .reviewId(response.getId())
                        .imageUrl(imageUrl)
                        .thumbnailUrl(imageUrl) // TODO: 실제 구현에서는 리사이즈 후 별도 URL 생성
                        .originalFilename(file.getOriginalFilename())
                        .fileSize((int) file.getSize())
                        .sortOrder(sortOrder++)
                        .build();

                reviewImageRepository.save(image);
                imageUrls.add(imageUrl);

                log.info("Review image saved: reviewId={}, imageId={}, filename={}",
                        response.getId(), image.getId(), file.getOriginalFilename());
            }

            // 4. 응답에 이미지 URL 포함하여 재구성
            if (!imageUrls.isEmpty()) {
                response = ReviewResponse.builder()
                        .id(response.getId())
                        .businessName(response.getBusinessName())
                        .businessSlug(response.getBusinessSlug())
                        .customerName(response.getCustomerName())
                        .rating(response.getRating())
                        .content(response.getContent())
                        .serviceName(response.getServiceName())
                        .staffName(response.getStaffName())
                        .images(imageUrls)
                        .createdAt(response.getCreatedAt())
                        .reply(response.getReply())
                        .build();
            }
        }

        return response;
    }

    // ========================================
    // 1-3. 내 리뷰 목록 조회 (로그인 고객)
    // ========================================

    /**
     * 로그인 고객의 리뷰 목록 조회
     * userId 기반으로 본인이 작성한 리뷰를 조회합니다.
     * business 정보(businessName, businessSlug)가 포함됩니다.
     *
     * @param userId 로그인 사용자 ID
     * @param page   페이지 번호 (1부터 시작)
     * @param size   페이지당 개수
     * @return 리뷰 목록 (페이징)
     */
    public PageResponse<ReviewResponse> getMyReviews(Long userId, int page, int size) {
        int offset = (page - 1) * size;

        List<Map<String, Object>> rows = reviewRepository.findByCustomerUserId(userId, offset, size);
        int totalElements = reviewRepository.countByCustomerUserId(userId);

        List<ReviewResponse> content = rows.stream()
                .map(this::mapRowToReviewResponse)
                .collect(Collectors.toList());

        return PageResponse.of(content, page, size, totalElements);
    }

    /**
     * DB 결과 Row → ReviewResponse 변환 (내 리뷰 목록용)
     */
    private ReviewResponse mapRowToReviewResponse(Map<String, Object> row) {
        Long reservationId = row.get("reservationId") != null
                ? ((Number) row.get("reservationId")).longValue() : null;
        Long staffId = row.get("staffId") != null
                ? ((Number) row.get("staffId")).longValue() : null;

        String serviceName = getServiceNameFromReservation(reservationId);
        String staffName = getStaffName(staffId);
        String businessName = (String) row.get("businessName");
        String businessSlug = (String) row.get("businessSlug");
        String customerName = (String) row.get("customerName");

        String replyContent = (String) row.get("replyContent");
        LocalDateTime replyCreatedAt = row.get("replyCreatedAt") != null
                ? ((java.sql.Timestamp) row.get("replyCreatedAt")).toLocalDateTime() : null;
        ReviewResponse.ReplyInfo replyInfo = null;
        if (replyContent != null && !replyContent.isBlank()) {
            replyInfo = new ReviewResponse.ReplyInfo(replyContent, replyCreatedAt);
        }

        // images는 JSONB → String으로 올 수 있음 (raw JSON)
        List<String> images = parseImages(row.get("images"));

        return ReviewResponse.builder()
                .id(((Number) row.get("id")).longValue())
                .businessName(businessName)
                .businessSlug(businessSlug)
                .customerName(ReviewResponse.maskName(customerName))
                .rating(((Number) row.get("rating")).intValue())
                .content((String) row.get("content"))
                .serviceName(serviceName)
                .staffName(staffName)
                .images(images)
                .createdAt(row.get("createdAt") != null
                        ? ((java.sql.Timestamp) row.get("createdAt")).toLocalDateTime() : null)
                .reply(replyInfo)
                .build();
    }

    /**
     * JSONB images 필드 파싱
     */
    @SuppressWarnings("unchecked")
    private List<String> parseImages(Object imagesObj) {
        if (imagesObj == null) {
            return List.of();
        }
        if (imagesObj instanceof List) {
            return (List<String>) imagesObj;
        }
        if (imagesObj instanceof String jsonStr) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(jsonStr, mapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (Exception e) {
                return List.of();
            }
        }
        return List.of();
    }

    // ========================================
    // 2. Public 리뷰 목록 조회
    // ========================================

    /**
     * 고객용 리뷰 목록 조회 (Public)
     * ACTIVE 상태만 조회, 고객명 마스킹 처리
     *
     * @return 리뷰 목록 + 통계 정보 (Map)
     */
    public Map<String, Object> getPublicReviews(Long businessId, Integer rating, Long staffId,
                                                 String sortBy, int page, int size) {
        int offset = (page - 1) * size;

        // 리뷰 목록 조회
        List<Review> reviews = reviewRepository.findPublicByBusinessId(
                businessId, rating, staffId, sortBy, offset, size);
        int totalCount = reviewRepository.countPublicByBusinessId(businessId, rating, staffId);

        // 각 리뷰에 서비스명/스태프명 매핑
        List<ReviewResponse> responseList = reviews.stream()
                .map(review -> {
                    String serviceName = getServiceNameFromReservation(review.getReservationId());
                    String sName = getStaffName(review.getStaffId());
                    return ReviewResponse.from(review, serviceName, sName);
                })
                .collect(Collectors.toList());

        // 평점 통계
        Double avgRating = reviewRepository.getAverageRatingByBusinessId(businessId);
        List<Map<String, Object>> ratingDist = reviewRepository.getRatingDistribution(businessId);

        // 별점 분포를 {1: count, 2: count, ...} Map으로 변환
        Map<Integer, Integer> ratingDistMap = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDistMap.put(i, 0);
        }
        for (Map<String, Object> entry : ratingDist) {
            Integer ratingKey = ((Number) entry.get("rating")).intValue();
            Integer count = ((Number) entry.get("count")).intValue();
            ratingDistMap.put(ratingKey, count);
        }

        int totalReviewCount = reviewRepository.countActiveByBusinessId(businessId);

        Map<String, Object> result = new HashMap<>();
        result.put("items", responseList);
        result.put("totalCount", totalCount);
        result.put("page", page);
        result.put("size", size);
        result.put("totalPages", (int) Math.ceil((double) totalCount / size));
        result.put("averageRating", avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0);
        result.put("totalReviewCount", totalReviewCount);
        result.put("ratingDistribution", ratingDistMap);

        return result;
    }

    // ========================================
    // 3. Admin 리뷰 목록 조회
    // ========================================

    /**
     * 관리자용 리뷰 목록 조회 (Admin)
     * 전체 상태 조회 가능, 고객 전체 정보 포함
     *
     * @return 리뷰 목록 + 통계 정보 (Map)
     */
    public Map<String, Object> getAdminReviews(Long businessId, ReviewSearchCondition condition) {
        // 리뷰 목록 조회
        List<Review> reviews = reviewRepository.findByBusinessId(condition);
        int totalCount = reviewRepository.countByBusinessId(condition);

        // 각 리뷰에 서비스명/스태프명 매핑
        List<ReviewAdminResponse> responseList = reviews.stream()
                .map(review -> {
                    String serviceName = getServiceNameFromReservation(review.getReservationId());
                    String staffName = getStaffName(review.getStaffId());
                    return ReviewAdminResponse.from(review, serviceName, staffName);
                })
                .collect(Collectors.toList());

        // 통계 정보
        Double avgRating = reviewRepository.getAverageRatingByBusinessId(businessId);
        int totalReviews = reviewRepository.countActiveByBusinessId(businessId);
        int unrepliedCount = reviewRepository.countUnrepliedByBusinessId(businessId);
        int thisMonthCount = reviewRepository.countThisMonthByBusinessId(businessId);

        List<Map<String, Object>> ratingDist = reviewRepository.getRatingDistribution(businessId);
        Map<Integer, Integer> ratingDistMap = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            ratingDistMap.put(i, 0);
        }
        for (Map<String, Object> entry : ratingDist) {
            Integer ratingKey = ((Number) entry.get("rating")).intValue();
            Integer count = ((Number) entry.get("count")).intValue();
            ratingDistMap.put(ratingKey, count);
        }

        ReviewStatsResponse stats = ReviewStatsResponse.builder()
                .averageRating(avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0)
                .totalReviews(totalReviews)
                .unrepliedCount(unrepliedCount)
                .thisMonthCount(thisMonthCount)
                .ratingDistribution(ratingDistMap)
                .build();

        Map<String, Object> result = new HashMap<>();
        result.put("items", responseList);
        result.put("totalCount", totalCount);
        result.put("page", condition.getPage());
        result.put("size", condition.getSize());
        result.put("totalPages", (int) Math.ceil((double) totalCount / condition.getSize()));
        result.put("stats", stats);

        return result;
    }

    // ========================================
    // 4. 리뷰 답변 등록 (Admin)
    // ========================================

    /**
     * 리뷰에 사장님 답변 등록
     *
     * @param businessId 매장 ID (권한 확인용)
     * @param reviewId   리뷰 ID
     * @param request    답변 요청
     */
    @Transactional
    public void replyToReview(Long businessId, Long reviewId, ReviewReplyRequest request) {
        // 1. 리뷰 존재 확인
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.REVIEW_NOT_FOUND,
                        "리뷰를 찾을 수 없습니다: " + reviewId));

        // 2. 매장 소속 확인
        if (!review.getBusinessId().equals(businessId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "해당 매장의 리뷰가 아닙니다");
        }

        // 3. 이미 답변 확인
        if (review.isReplied()) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_REPLIED,
                    "이미 답변이 등록된 리뷰입니다");
        }

        // 4. 답변 등록
        reviewRepository.updateReply(reviewId, request.getContent(), LocalDateTime.now());

        log.info("Review reply created: reviewId={}, businessId={}", reviewId, businessId);
    }

    // ========================================
    // 5. 리뷰 삭제 (Admin, 소프트 삭제)
    // ========================================

    /**
     * 리뷰 소프트 삭제
     *
     * @param businessId 매장 ID (권한 확인용)
     * @param reviewId   리뷰 ID
     * @param request    삭제 요청 (사유, 선택)
     */
    @Transactional
    public void deleteReview(Long businessId, Long reviewId, ReviewDeleteRequest request) {
        // 1. 리뷰 존재 확인
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.REVIEW_NOT_FOUND,
                        "리뷰를 찾을 수 없습니다: " + reviewId));

        // 2. 매장 소속 확인
        if (!review.getBusinessId().equals(businessId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "해당 매장의 리뷰가 아닙니다");
        }

        // 3. 이미 삭제 확인
        if (review.isDeleted()) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_DELETED,
                    "이미 삭제된 리뷰입니다");
        }

        // 4. 상태 변경 -> DELETED
        String deleteReason = (request != null && request.getReason() != null)
                ? request.getReason() : null;
        reviewRepository.updateStatus(reviewId, ReviewStatus.DELETED.name(), deleteReason);

        log.info("Review deleted: reviewId={}, businessId={}, reason={}",
                reviewId, businessId, deleteReason);

        // 5. 매장 평점 통계 재계산
        updateBusinessRatingStats(businessId);
    }

    // ========================================
    // 6. 고객 리뷰 수정 (본인 확인)
    // ========================================

    /**
     * 고객 리뷰 수정
     * - JWT 인증된 userId로 리뷰 소유자 확인
     * - 별점, 내용 부분 수정 지원 (COALESCE)
     * - 별점 변경 시 매장 통계 갱신
     *
     * @param reviewId 리뷰 ID
     * @param userId   인증된 사용자 ID
     * @param request  수정 요청
     * @return 수정된 리뷰 응답
     */
    @Transactional
    public ReviewResponse updateReview(Long reviewId, Long userId, ReviewUpdateRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.REVIEW_NOT_FOUND,
                        "리뷰를 찾을 수 없습니다: " + reviewId));

        // 본인 확인: userId와 연결된 customer인지 확인
        if (!isReviewOwner(review, userId)) {
            throw new BusinessException(ErrorCode.REVIEW_UPDATE_DENIED,
                    "본인의 리뷰만 수정할 수 있습니다");
        }

        if (review.isDeleted()) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_DELETED,
                    "이미 삭제된 리뷰입니다");
        }

        reviewRepository.updateContent(reviewId, request.getRating(), request.getContent());

        // 별점 변경 시 매장 통계 갱신
        if (request.getRating() != null) {
            updateBusinessRatingStats(review.getBusinessId());
        }

        Review updatedReview = reviewRepository.findById(reviewId).orElseThrow();
        String serviceName = getServiceNameFromReservation(updatedReview.getReservationId());
        String staffName = getStaffName(updatedReview.getStaffId());

        log.info("Review updated by customer: reviewId={}, userId={}", reviewId, userId);

        return ReviewResponse.from(updatedReview, serviceName, staffName);
    }

    // ========================================
    // 7. 고객 리뷰 삭제 (본인 확인, soft delete)
    // ========================================

    /**
     * 고객 리뷰 삭제 (소프트 삭제)
     * - JWT 인증된 userId로 리뷰 소유자 확인
     * - 상태를 DELETED로 변경
     * - 매장 평점 통계 재계산
     *
     * @param reviewId 리뷰 ID
     * @param userId   인증된 사용자 ID
     */
    @Transactional
    public void deleteReviewByCustomer(Long reviewId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.REVIEW_NOT_FOUND,
                        "리뷰를 찾을 수 없습니다: " + reviewId));

        if (!isReviewOwner(review, userId)) {
            throw new BusinessException(ErrorCode.REVIEW_UPDATE_DENIED,
                    "본인의 리뷰만 삭제할 수 있습니다");
        }

        if (review.isDeleted()) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_DELETED,
                    "이미 삭제된 리뷰입니다");
        }

        reviewRepository.updateStatus(reviewId, ReviewStatus.DELETED.name(), "고객 본인 삭제");
        updateBusinessRatingStats(review.getBusinessId());

        log.info("Customer deleted review: reviewId={}, userId={}", reviewId, userId);
    }

    // ========================================
    // 8. 리뷰 이미지 업로드
    // ========================================

    /**
     * 리뷰 이미지 업로드
     * - 리뷰당 최대 5개 이미지 제한
     * - 본인 리뷰 확인
     *
     * @param reviewId         리뷰 ID
     * @param userId           인증된 사용자 ID
     * @param imageUrl         이미지 URL
     * @param thumbnailUrl     썸네일 URL
     * @param originalFilename 원본 파일명
     * @param fileSize         파일 크기 (bytes)
     * @return 생성된 이미지 응답
     */
    @Transactional
    public ReviewImageResponse addReviewImage(Long reviewId, Long userId,
                                               String imageUrl, String thumbnailUrl,
                                               String originalFilename, Integer fileSize) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.REVIEW_NOT_FOUND,
                        "리뷰를 찾을 수 없습니다: " + reviewId));

        if (!isReviewOwner(review, userId)) {
            throw new BusinessException(ErrorCode.REVIEW_UPDATE_DENIED,
                    "본인의 리뷰만 수정할 수 있습니다");
        }

        int imageCount = reviewImageRepository.countByReviewId(reviewId);
        if (imageCount >= 5) {
            throw new BusinessException(ErrorCode.REVIEW_IMAGE_LIMIT_EXCEEDED,
                    "리뷰당 최대 5개의 이미지만 등록할 수 있습니다");
        }

        ReviewImage image = ReviewImage.builder()
                .reviewId(reviewId)
                .imageUrl(imageUrl)
                .thumbnailUrl(thumbnailUrl)
                .originalFilename(originalFilename)
                .fileSize(fileSize)
                .sortOrder(imageCount)
                .build();

        reviewImageRepository.save(image);

        log.info("Review image added: reviewId={}, imageId={}", reviewId, image.getId());
        return ReviewImageResponse.from(image);
    }

    // ========================================
    // 9. 리뷰 이미지 삭제
    // ========================================

    /**
     * 리뷰 이미지 삭제
     * - 본인 리뷰 확인
     * - 이미지가 해당 리뷰에 속하는지 확인
     *
     * @param reviewId 리뷰 ID
     * @param imageId  이미지 ID
     * @param userId   인증된 사용자 ID
     */
    @Transactional
    public void deleteReviewImage(Long reviewId, Long imageId, Long userId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.REVIEW_NOT_FOUND,
                        "리뷰를 찾을 수 없습니다: " + reviewId));

        if (!isReviewOwner(review, userId)) {
            throw new BusinessException(ErrorCode.REVIEW_UPDATE_DENIED,
                    "본인의 리뷰만 수정할 수 있습니다");
        }

        ReviewImage image = reviewImageRepository.findById(imageId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.REVIEW_IMAGE_NOT_FOUND,
                        "리뷰 이미지를 찾을 수 없습니다: " + imageId));

        if (!image.getReviewId().equals(reviewId)) {
            throw new BusinessException(ErrorCode.REVIEW_IMAGE_NOT_FOUND,
                    "해당 리뷰의 이미지가 아닙니다");
        }

        reviewImageRepository.deleteById(imageId);
        log.info("Review image deleted: reviewId={}, imageId={}", reviewId, imageId);
    }

    // ========================================
    // Private Helpers
    // ========================================

    /**
     * 리뷰 소유자 확인
     * - review.customerId -> customer.userId == userId 인지 확인
     */
    private boolean isReviewOwner(Review review, Long userId) {
        if (review.getCustomerId() == null) return false;
        return customerRepository.findById(review.getCustomerId())
                .map(c -> c.getUserId() != null && c.getUserId().equals(userId))
                .orElse(false);
    }

    /**
     * 매장 평점 통계 갱신
     * - reviews 테이블에서 ACTIVE 상태의 평균 평점 및 리뷰 수를 계산
     * - businesses 테이블의 average_rating, review_count 필드를 업데이트
     */
    private void updateBusinessRatingStats(Long businessId) {
        Double avgRating = reviewRepository.getAverageRatingByBusinessId(businessId);
        int reviewCount = reviewRepository.countActiveByBusinessId(businessId);

        // 소수점 1자리로 반올림
        Double roundedRating = avgRating != null ? Math.round(avgRating * 10) / 10.0 : 0.0;

        businessRepository.updateRatingStats(businessId, roundedRating, reviewCount);

        log.debug("Business rating stats updated: businessId={}, avgRating={}, reviewCount={}",
                businessId, roundedRating, reviewCount);
    }

    /**
     * 예약 ID에서 서비스명 추출
     */
    private String getServiceNameFromReservation(Long reservationId) {
        if (reservationId == null) {
            return null;
        }
        return reservationRepository.findById(reservationId)
                .map(this::getServiceNameFromReservation)
                .orElse(null);
    }

    /**
     * 예약 엔티티에서 첫 번째 서비스명 추출
     */
    private String getServiceNameFromReservation(Reservation reservation) {
        List<String> serviceNames = reservation.getServiceNames();
        if (serviceNames == null || serviceNames.isEmpty()) {
            return null;
        }
        if (serviceNames.size() == 1) {
            return serviceNames.get(0);
        }
        // 2개 이상이면 첫 번째 + 외 N건 형태
        return serviceNames.get(0) + " 외 " + (serviceNames.size() - 1) + "건";
    }

    /**
     * 스태프 이름 조회
     */
    private String getStaffName(Long staffId) {
        if (staffId == null) {
            return null;
        }
        return staffRepository.findById(staffId)
                .map(Staff::getName)
                .orElse(null);
    }
}
