package io.moer.booking.domain.booking.service;

import io.moer.booking.common.dto.PageResponse;
import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.booking.dto.CustomerReservationCancelRequest;
import io.moer.booking.domain.booking.dto.CustomerReservationCreateRequest;
import io.moer.booking.domain.booking.dto.CustomerReservationListResponse;
import io.moer.booking.domain.booking.dto.PublicReservationCreateRequest;
import io.moer.booking.domain.booking.dto.PublicReservationResponse;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.customer.Customer;
import io.moer.booking.domain.customer.repository.CustomerRepository;
import io.moer.booking.domain.reservation.Reservation;
import io.moer.booking.domain.reservation.ReservationStatus;
import io.moer.booking.domain.reservation.repository.ReservationRepository;
import io.moer.booking.domain.review.repository.ReviewRepository;
import io.moer.booking.domain.staff.Staff;
import io.moer.booking.domain.staff.repository.StaffRepository;
import io.moer.booking.domain.user.User;
import io.moer.booking.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 로그인 고객용 예약 서비스
 * 기존 PublicBookingService의 예약 생성 로직을 재사용하며,
 * userId 기반의 본인 확인과 고객 자동 연결 기능을 추가합니다.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerBookingService {

    private final PublicBookingService publicBookingService;
    private final ReservationRepository reservationRepository;
    private final BusinessRepository businessRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    // ========================================
    // 예약 생성
    // ========================================

    /**
     * 로그인 고객 예약 생성
     * User 정보에서 이름/전화번호/이메일을 자동으로 가져와
     * PublicBookingService의 예약 생성 로직을 재사용합니다.
     *
     * @param slug   매장 슬러그
     * @param userId 로그인 사용자 ID
     * @param request 예약 생성 요청
     * @return 예약 생성 결과
     */
    @Transactional
    public PublicReservationResponse createReservation(String slug, Long userId,
                                                       CustomerReservationCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 전화번호 필수 확인
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new BusinessException(ErrorCode.CUSTOMER_PHONE_REQUIRED,
                    "예약을 위해 전화번호 등록이 필요합니다");
        }

        // 매장 조회
        Business business = businessRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND,
                        "매장을 찾을 수 없습니다: " + slug));

        // 매장별 고객 레코드 생성/연결
        findOrCreateCustomer(business.getId(), userId, user);

        // PublicBookingService의 기존 예약 생성 로직을 재사용
        PublicReservationCreateRequest publicRequest = createPublicRequest(request, user);
        PublicReservationResponse response = publicBookingService.createReservation(slug, publicRequest);

        // 생성된 예약에 user_id 연결
        Reservation reservation = reservationRepository.findByReservationNumber(response.getReservationNumber())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.RESERVATION_NOT_FOUND));
        reservationRepository.updateUserId(reservation.getId(), userId);

        log.info("Customer reservation created: userId={}, reservationNumber={}",
                userId, response.getReservationNumber());

        return response;
    }

    // ========================================
    // 내 예약 목록 조회
    // ========================================

    /**
     * 로그인 고객의 예약 목록을 조회합니다.
     *
     * @param userId 로그인 사용자 ID
     * @param status 예약 상태 필터 (선택)
     * @param page   페이지 번호 (1부터 시작)
     * @param size   페이지당 개수
     * @return 예약 목록 (페이징)
     */
    public PageResponse<CustomerReservationListResponse> getMyReservations(
            Long userId, String status, int page, int size) {
        int offset = (page - 1) * size;
        List<Reservation> reservations = reservationRepository.findByUserId(userId, status, offset, size);
        int totalElements = reservationRepository.countByUserId(userId, status);

        List<CustomerReservationListResponse> content = reservations.stream()
                .map(this::toListResponse)
                .toList();

        return PageResponse.of(content, page, size, totalElements);
    }

    // ========================================
    // 예약 상세 조회
    // ========================================

    /**
     * 로그인 고객의 예약 상세 정보를 조회합니다.
     * userId로 본인 확인이 이루어지므로 별도의 전화번호 검증이 불필요합니다.
     *
     * @param userId            로그인 사용자 ID
     * @param reservationNumber 예약 번호
     * @return 예약 상세 정보
     */
    public CustomerReservationListResponse getReservation(Long userId, String reservationNumber) {
        Reservation reservation = reservationRepository.findByUserIdAndReservationNumber(userId, reservationNumber)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.CUSTOMER_RESERVATION_NOT_FOUND,
                        "고객의 예약을 찾을 수 없습니다: " + reservationNumber));
        return toListResponse(reservation);
    }

    // ========================================
    // 예약 취소
    // ========================================

    /**
     * 로그인 고객의 예약을 취소합니다.
     * userId로 본인 확인이 이루어지므로 전화번호 검증이 불필요합니다.
     *
     * @param userId            로그인 사용자 ID
     * @param reservationNumber 예약 번호
     * @param request           취소 요청 (사유 포함, 선택)
     */
    @Transactional
    public void cancelReservation(Long userId, String reservationNumber,
                                  CustomerReservationCancelRequest request) {
        Reservation reservation = reservationRepository.findByUserIdAndReservationNumber(userId, reservationNumber)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.CUSTOMER_RESERVATION_NOT_FOUND,
                        "고객의 예약을 찾을 수 없습니다: " + reservationNumber));

        if (!reservation.canCancel()) {
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CANCELLED,
                    "이미 취소되었거나 노쇼 처리된 예약입니다");
        }

        // 시간 기반 취소 검증 (예약 2시간 전까지만 취소 가능)
        LocalDateTime reservationStart = reservation.getReservationDate()
                .atTime(reservation.getStartTime());
        LocalDateTime cancelDeadline = reservationStart.minusHours(2);
        if (LocalDateTime.now().isAfter(cancelDeadline)) {
            throw new BusinessException(ErrorCode.RESERVATION_CANCEL_DEADLINE_PASSED,
                    "예약 2시간 전까지만 취소 가능합니다");
        }

        // 취소 처리 (userId 기반 본인 확인 완료, 전화번호 검증 불필요)
        Reservation cancelledReservation = Reservation.builder()
                .id(reservation.getId())
                .status(ReservationStatus.CANCELLED)
                .cancelledAt(LocalDateTime.now())
                .cancelReason(request != null ? request.getReason() : null)
                .build();
        reservationRepository.updateCancellation(cancelledReservation);

        log.info("Customer reservation cancelled: userId={}, reservationNumber={}",
                userId, reservationNumber);
    }

    // ========================================
    // Private 헬퍼
    // ========================================

    /**
     * 매장별 고객 레코드를 찾거나 새로 생성합니다.
     * 1. userId+businessId로 이미 연결된 고객이 있는지 확인
     * 2. 전화번호로 기존 고객이 있으면 userId를 연결
     * 3. 둘 다 없으면 신규 고객 생성
     */
    private Customer findOrCreateCustomer(Long businessId, Long userId, User user) {
        // 이미 연결된 고객이 있는지 확인
        Optional<Customer> existing = customerRepository.findByUserIdAndBusinessId(userId, businessId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 전화번호로 기존 고객 찾기
        Optional<Customer> byPhone = customerRepository.findByBusinessIdAndPhone(businessId, user.getPhone());
        if (byPhone.isPresent()) {
            // 기존 고객에 userId 연결
            customerRepository.updateUserId(byPhone.get().getId(), userId);
            return byPhone.get();
        }

        // 신규 고객 생성
        Customer newCustomer = Customer.builder()
                .businessId(businessId)
                .userId(userId)
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .visitCount(0)
                .totalSpent(0)
                .build();
        customerRepository.save(newCustomer);
        return newCustomer;
    }

    /**
     * CustomerReservationCreateRequest를 PublicReservationCreateRequest로 변환합니다.
     * User 정보에서 이름/전화번호/이메일을 자동으로 채웁니다.
     */
    private PublicReservationCreateRequest createPublicRequest(
            CustomerReservationCreateRequest request, User user) {
        return new PublicReservationCreateRequest(
                request.getServiceIds(),
                request.getStaffId(),
                request.getReservationDate(),
                request.getStartTime(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                request.getCustomerRequest()
        );
    }

    /**
     * Reservation 엔티티를 CustomerReservationListResponse로 변환합니다.
     * Business, Staff, Review 정보를 부가적으로 조회합니다.
     */
    private CustomerReservationListResponse toListResponse(Reservation reservation) {
        // Business 정보 조회
        Business business = businessRepository.findById(reservation.getBusinessId())
                .orElse(null);
        Long businessId = business != null ? business.getId() : reservation.getBusinessId();
        String businessName = business != null ? business.getName() : null;
        String businessSlug = business != null ? business.getSlug() : null;
        String businessProfileImageUrl = business != null ? business.getProfileImageUrl() : null;

        // Staff 이름 조회
        String staffName = null;
        if (reservation.getStaffId() != null) {
            staffName = staffRepository.findById(reservation.getStaffId())
                    .map(Staff::getName)
                    .orElse(null);
        }

        // 리뷰 존재 여부 확인
        boolean hasReview = reviewRepository.existsByReservationId(reservation.getId());

        return CustomerReservationListResponse.from(
                reservation, businessId, businessName, businessSlug, businessProfileImageUrl, staffName, hasReview);
    }
}
