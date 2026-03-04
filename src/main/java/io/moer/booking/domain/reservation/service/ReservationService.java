package io.moer.booking.domain.reservation.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.customer.Customer;
import io.moer.booking.domain.customer.repository.CustomerRepository;
import io.moer.booking.domain.customer.service.CustomerHistoryService;
import io.moer.booking.domain.customer.service.CustomerService;
import io.moer.booking.domain.notification.NotificationType;
import io.moer.booking.domain.notification.dto.SseEventData;
import io.moer.booking.domain.notification.service.NotificationService;
import io.moer.booking.domain.notification.service.SseEmitterService;
import io.moer.booking.domain.notificationlog.dto.NotificationSender;
import io.moer.booking.domain.holiday.SpecialHoliday;
import io.moer.booking.domain.holiday.repository.SpecialHolidayRepository;
import io.moer.booking.domain.reservation.Reservation;
import io.moer.booking.domain.reservation.ReservationStatus;
import io.moer.booking.domain.reservation.dto.*;
import io.moer.booking.domain.reservation.repository.ReservationRepository;
import io.moer.booking.domain.service.Service;
import io.moer.booking.domain.service.repository.ServiceRepository;
import io.moer.booking.domain.staff.Staff;
import io.moer.booking.domain.staff.StaffSchedule;
import io.moer.booking.domain.staff.repository.StaffRepository;
import io.moer.booking.domain.staff.repository.StaffScheduleRepository;
import io.moer.booking.domain.business.BusinessSettings;
import io.moer.booking.domain.business.repository.BusinessSettingsRepository;
import io.moer.booking.domain.subscription.service.UsageLimitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 예약 서비스
 */
@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BusinessRepository businessRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final StaffRepository staffRepository;
    private final StaffScheduleRepository staffScheduleRepository;
    private final ServiceRepository serviceRepository;
    private final SpecialHolidayRepository specialHolidayRepository;
    private final BusinessSettingsRepository businessSettingsRepository;
    private final CustomerHistoryService customerHistoryService;
    private final UsageLimitService usageLimitService;
    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;
    private final NotificationSender notificationSender;
    private final io.moer.booking.domain.business.service.OnboardingService onboardingService;

    // ========================================
    // 생성
    // ========================================

    /**
     * 예약 생성 (Customer 자동 생성 지원)
     */
    @Transactional
    public ReservationResponse createReservation(Long businessId, ReservationCreateRequest request) {
        // 1. Business 존재 확인
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        // 2. 예약 수 제한 체크
        usageLimitService.checkCanCreateReservation(businessId);

        // 3. Customer 조회 또는 자동 생성
        Customer customer = resolveCustomer(businessId, request);

        // 3. Staff 존재 확인 (선택 시)
        if (request.getStaffId() != null) {
            Staff staff = staffRepository.findById(request.getStaffId())
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND, "직원을 찾을 수 없습니다"));

            // Business 일치 확인
            if (!staff.getBusinessId().equals(businessId)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "다른 매장의 직원입니다");
            }
        }

        // 4. Service 조회 및 검증
        List<Service> services = request.getServiceIds().stream()
                .map(serviceId -> serviceRepository.findById(serviceId)
                        .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND,
                                "서비스를 찾을 수 없습니다: " + serviceId)))
                .collect(Collectors.toList());

        // Business 일치 확인
        services.forEach(service -> {
            if (!service.getBusinessId().equals(businessId)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        "다른 매장의 서비스입니다: " + service.getName());
            }
        });

        // 5. 총 시간 및 가격 계산
        int totalDuration = services.stream()
                .mapToInt(Service::getDuration)
                .sum();

        int totalPrice = services.stream()
                .mapToInt(Service::getPrice)
                .sum();

        // 6. 종료 시간 계산
        LocalTime endTime = request.getStartTime().plusMinutes(totalDuration);

        // 7. 예약 가능 여부 검증
        validateReservation(businessId, request.getStaffId(), request.getReservationDate(),
                request.getStartTime(), endTime, null);

        // 8. 예약 번호 생성
        String reservationNumber = generateReservationNumber(request.getReservationDate());

        // 9. services JSONB 데이터 생성
        List<Map<String, Object>> servicesJsonb = services.stream()
                .map(service -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", service.getId());
                    map.put("name", service.getName());
                    map.put("price", service.getPrice());
                    map.put("duration", service.getDuration());
                    return map;
                })
                .collect(Collectors.toList());

        // 10. 자동 확정 여부 확인
        ReservationStatus initialStatus = ReservationStatus.PENDING;
        BusinessSettings settings = businessSettingsRepository.findByBusinessId(businessId).orElse(null);
        if (settings != null && settings.hasAutoConfirm()) {
            initialStatus = ReservationStatus.CONFIRMED;
            log.info("Auto-confirm enabled for business: {}", businessId);
        }

        // 11. Reservation 엔티티 생성
        Reservation reservation = Reservation.builder()
                .businessId(businessId)
                .customerId(customer.getId())
                .staffId(request.getStaffId())
                .reservationNumber(reservationNumber)
                .reservationDate(request.getReservationDate())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .services(servicesJsonb)
                .totalDuration(totalDuration)
                .totalPrice(totalPrice)
                .status(initialStatus)
                .customerMemo(request.getCustomerMemo())
                .staffMemo(null)
                .build();

        // 11. 저장
        reservationRepository.save(reservation);

        // 12. 예약 수 증가
        usageLimitService.incrementReservationCount(businessId);

        log.info("Reservation created: id={}, businessId={}, customerId={} ({}), date={}, time={}",
                reservation.getId(), businessId, customer.getId(),
                customer.getName(), request.getReservationDate(), request.getStartTime());

        // 13. 알림 생성 (매장 OWNER에게)
        sendReservationNotification(business, reservation, customer.getName(), NotificationType.RESERVATION_NEW);

        // 13-1. SSE 실시간 이벤트 발송
        String sseServiceName = services.stream().map(Service::getName).collect(Collectors.joining(", "));
        sseEmitterService.sendEventToBusinessOwner(businessId, "RESERVATION_CREATED", SseEventData.builder()
                .type("RESERVATION_CREATED")
                .referenceId(reservation.getId())
                .reservationNumber(reservation.getReservationNumber())
                .customerName(customer.getName())
                .serviceName(sseServiceName)
                .startTime(reservation.getReservationDate() + " " + reservation.getStartTime())
                .message("새 예약이 들어왔습니다.")
                .createdAt(LocalDateTime.now())
                .build());

        // 13-2. 외부 알림 로그 기록 (고객에게)
        String serviceName = services.stream().map(Service::getName).collect(Collectors.joining(", "));
        sendExternalNotificationLog(business, reservation, customer.getPhone(), customer.getName(),
                serviceName, "created");

        // 14. 온보딩 스텝 자동 완료
        onboardingService.markStepComplete(businessId, "reservation");

        return getReservation(businessId, reservation.getId());
    }

    /**
     * Customer 확인 로직
     *
     * Case 1: customerId가 있으면 → 기존 고객 사용 (관리자가 직접 선택)
     * Case 2: customerId가 없으면 → 이름/전화번호로 조회 또는 자동 생성
     */
    private Customer resolveCustomer(Long businessId, ReservationCreateRequest request) {
        // Case 1: customerId가 있으면 기존 고객 사용
        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND,
                            "고객을 찾을 수 없습니다"));

            // Business가 일치하는지 확인
            if (!customer.getBusinessId().equals(businessId)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        "다른 매장의 고객입니다");
            }

            log.info("Using existing customer: id={}, name={}", customer.getId(), customer.getName());
            return customer;
        }

        // Case 2: 이름/전화번호로 조회 또는 자동 생성
        // 검증: customerId가 없으면 이름과 전화번호 필수
        if (request.getCustomerName() == null || request.getCustomerName().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "고객 ID 또는 고객 이름은 필수입니다");
        }
        if (request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "고객 ID 또는 고객 전화번호는 필수입니다");
        }

        return customerService.findOrCreateCustomer(
                businessId,
                request.getCustomerName(),
                request.getCustomerPhone()
        );
    }

    /**
     * 예약 번호 생성
     * 형식: YYMMDD-RANDOM4 (예: 250115-A3B9)
     */
    private String generateReservationNumber(LocalDate date) {
        String datePrefix = date.format(DateTimeFormatter.ofPattern("yyMMdd"));
        String randomSuffix = generateRandomString(4);
        String reservationNumber = datePrefix + "-" + randomSuffix;

        // 중복 체크 (만약 중복이면 재생성)
        int attempts = 0;
        while (reservationRepository.existsByReservationNumber(reservationNumber) && attempts < 10) {
            randomSuffix = generateRandomString(4);
            reservationNumber = datePrefix + "-" + randomSuffix;
            attempts++;
        }

        return reservationNumber;
    }

    /**
     * 랜덤 문자열 생성 (영문 대문자 + 숫자)
     */
    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ========================================
    // 조회
    // ========================================

    /**
     * 예약 단건 조회
     */
    public ReservationResponse getReservation(Long businessId, Long reservationId) {
        if (!reservationRepository.existsByBusinessIdAndId(businessId, reservationId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND,
                    "예약을 찾을 수 없습니다");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND,
                        "예약을 찾을 수 없습니다"));

        return enrichReservationResponse(reservation);
    }

    /**
     * 예약번호로 조회
     */
    public ReservationResponse getReservationByNumber(String reservationNumber) {
        Reservation reservation = reservationRepository.findByReservationNumber(reservationNumber)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND,
                        "예약을 찾을 수 없습니다: " + reservationNumber));

        return enrichReservationResponse(reservation);
    }

    /**
     * Business의 전체 예약 조회
     */
    public List<ReservationResponse> getReservationsByBusiness(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        List<Reservation> reservations = reservationRepository.findByBusinessId(businessId);
        return reservations.stream()
                .map(this::enrichReservationResponse)
                .collect(Collectors.toList());
    }

    /**
     * 날짜별 예약 조회
     */
    public List<ReservationResponse> getReservationsByDate(Long businessId, LocalDate date) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        List<Reservation> reservations = reservationRepository.findByBusinessIdAndDate(businessId, date);
        return reservations.stream()
                .map(this::enrichReservationResponse)
                .collect(Collectors.toList());
    }

    /**
     * 기간별 예약 조회
     */
    public List<ReservationResponse> getReservationsByDateRange(Long businessId,
                                                                LocalDate startDate,
                                                                LocalDate endDate) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        List<Reservation> reservations = reservationRepository.findByBusinessIdAndDateRange(
                businessId, startDate, endDate);

        return reservations.stream()
                .map(this::enrichReservationResponse)
                .collect(Collectors.toList());
    }


    /**
     * Customer의 예약 조회
     */
    public List<ReservationResponse> getReservationsByCustomer(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND, "고객을 찾을 수 없습니다");
        }

        List<Reservation> reservations = reservationRepository.findByCustomerId(customerId);
        return reservations.stream()
                .map(this::enrichReservationResponse)
                .collect(Collectors.toList());
    }

    /**
     * Staff의 예약 조회
     */
    public List<ReservationResponse> getReservationsByStaff(Long staffId) {
        if (!staffRepository.existsById(staffId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND, "직원을 찾을 수 없습니다");
        }

        List<Reservation> reservations = reservationRepository.findByStaffId(staffId);
        return reservations.stream()
                .map(this::enrichReservationResponse)
                .collect(Collectors.toList());
    }

    /**
     * 조건별 예약 검색
     */
    public List<ReservationResponse> searchReservations(ReservationSearchCondition condition) {
        List<Reservation> reservations = reservationRepository.search(condition);
        return reservations.stream()
                .map(this::enrichReservationResponse)
                .collect(Collectors.toList());
    }

    // ========================================
    // 수정
    // ========================================

    /**
     * 예약 수정
     */
    @Transactional
    public ReservationResponse updateReservation(Long businessId, Long reservationId,
                                                 ReservationUpdateRequest request) {
        if (!reservationRepository.existsByBusinessIdAndId(businessId, reservationId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        // 취소/완료된 예약은 수정 불가
        if (reservation.getStatus() == ReservationStatus.CANCELLED ||
                reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "취소되거나 완료된 예약은 수정할 수 없습니다");
        }

        // Staff 변경 시 존재 확인
        Long newStaffId = request.getStaffId() != null ? request.getStaffId() : reservation.getStaffId();
        if (request.getStaffId() != null) {
            Staff staff = staffRepository.findById(request.getStaffId())
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND, "직원을 찾을 수 없습니다"));

            if (!staff.getBusinessId().equals(businessId)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "다른 매장의 직원입니다");
            }
        }

        // 날짜/시간 변경 시 재계산
        LocalDate newDate = request.getReservationDate() != null ?
                request.getReservationDate() : reservation.getReservationDate();
        LocalTime newStartTime = request.getStartTime() != null ?
                request.getStartTime() : reservation.getStartTime();

        // 서비스 변경 시 시간/가격 재계산
        int totalDuration = reservation.getTotalDuration();
        int totalPrice = reservation.getTotalPrice();
        List<Map<String, Object>> servicesJsonb = reservation.getServices();

        if (request.getServiceIds() != null && !request.getServiceIds().isEmpty()) {
            List<Service> services = request.getServiceIds().stream()
                    .map(serviceId -> serviceRepository.findById(serviceId)
                            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND,
                                    "서비스를 찾을 수 없습니다: " + serviceId)))
                    .collect(Collectors.toList());

            // Business 일치 확인
            services.forEach(service -> {
                if (!service.getBusinessId().equals(businessId)) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                            "다른 매장의 서비스입니다: " + service.getName());
                }
            });

            totalDuration = services.stream().mapToInt(Service::getDuration).sum();
            totalPrice = services.stream().mapToInt(Service::getPrice).sum();

            servicesJsonb = services.stream()
                    .map(service -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("id", service.getId());
                        map.put("name", service.getName());
                        map.put("price", service.getPrice());
                        map.put("duration", service.getDuration());
                        return map;
                    })
                    .collect(Collectors.toList());
        }

        LocalTime newEndTime = newStartTime.plusMinutes(totalDuration);

        // 시간 겹침 검증 (날짜/시간/서비스가 변경된 경우만)
        if (request.getReservationDate() != null || request.getStartTime() != null ||
                request.getServiceIds() != null || request.getStaffId() != null) {
            validateReservation(businessId, newStaffId, newDate, newStartTime, newEndTime, reservationId);
        }

        // 수정
        Reservation updatedReservation = Reservation.builder()
                .id(reservation.getId())
                .businessId(reservation.getBusinessId())
                .customerId(reservation.getCustomerId())
                .staffId(newStaffId)
                .reservationNumber(reservation.getReservationNumber())
                .reservationDate(newDate)
                .startTime(newStartTime)
                .endTime(newEndTime)
                .services(servicesJsonb)
                .totalDuration(totalDuration)
                .totalPrice(totalPrice)
                .status(reservation.getStatus())
                .customerMemo(request.getCustomerMemo() != null ?
                        request.getCustomerMemo() : reservation.getCustomerMemo())
                .staffMemo(request.getStaffMemo() != null ?
                        request.getStaffMemo() : reservation.getStaffMemo())
                .cancelledAt(reservation.getCancelledAt())
                .cancelReason(reservation.getCancelReason())
                .createdAt(reservation.getCreatedAt())
                .build();

        reservationRepository.update(updatedReservation);

        log.info("Reservation updated: id={}, businessId={}", reservationId, businessId);

        return getReservation(businessId, reservationId);
    }

    // ========================================
    // 일정 변경 (Reschedule)
    // ========================================

    /**
     * 예약 일정 변경 (날짜/시간/직원 변경)
     * - 취소/완료 상태의 예약은 변경 불가
     * - 기존 검증 로직(휴무일, 근무시간, 시간 충돌) 재사용
     * - 시간 충돌 검증 시 자기 자신은 제외
     */
    @Transactional
    public RescheduleResponse reschedule(Long businessId, Long reservationId, RescheduleRequest request) {
        // 1. 예약 존재 확인
        if (!reservationRepository.existsByBusinessIdAndId(businessId, reservationId)) {
            throw new EntityNotFoundException(ErrorCode.RESERVATION_NOT_FOUND,
                    "예약을 찾을 수 없습니다: " + reservationId);
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.RESERVATION_NOT_FOUND,
                        "예약을 찾을 수 없습니다: " + reservationId));

        // 2. 상태 확인 (CANCELLED, COMPLETED이면 변경 불가)
        if (reservation.getStatus() == ReservationStatus.CANCELLED ||
                reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.RESCHEDULE_INVALID_STATUS,
                    "취소/완료된 예약은 일정을 변경할 수 없습니다");
        }

        // 3. 이전 일정 정보 저장 (응답용)
        LocalDate previousDate = reservation.getReservationDate();
        LocalTime previousStartTime = reservation.getStartTime();

        // 4. staffId 결정 (요청에 없으면 기존 유지)
        Long staffId = request.getStaffId() != null ? request.getStaffId() : reservation.getStaffId();

        // Staff 변경 시 존재 및 매장 소속 확인
        if (request.getStaffId() != null) {
            Staff staff = staffRepository.findById(request.getStaffId())
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND,
                            "직원을 찾을 수 없습니다: " + request.getStaffId()));

            if (!staff.getBusinessId().equals(businessId)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "다른 매장의 직원입니다");
            }
        }

        // 5. endTime 결정 (요청에 없으면 기존 소요시간 유지)
        LocalTime newEndTime;
        if (request.getNewEndTime() != null) {
            newEndTime = request.getNewEndTime();
        } else {
            // 기존 duration 계산하여 새 시작 시간에 적용
            long durationMinutes = java.time.Duration.between(
                    reservation.getStartTime(), reservation.getEndTime()).toMinutes();
            newEndTime = request.getNewStartTime().plusMinutes(durationMinutes);
        }

        // 6. 예약 가능 여부 검증 (휴무일, 근무시간, 시간 충돌 - 자기 자신 제외)
        validateReservation(businessId, staffId, request.getNewDate(),
                request.getNewStartTime(), newEndTime, reservationId);

        // 7. 예약 일정 업데이트
        reservationRepository.updateSchedule(reservationId,
                request.getNewDate(), request.getNewStartTime(), newEndTime, staffId);

        log.info("Reservation rescheduled: id={}, businessId={}, {} {} -> {} {}-{}",
                reservationId, businessId, previousDate, previousStartTime,
                request.getNewDate(), request.getNewStartTime(), newEndTime);

        // 8. 알림 생성 (매장 관리자에게)
        Business business = businessRepository.findById(businessId).orElse(null);
        Customer customer = customerRepository.findById(reservation.getCustomerId()).orElse(null);
        String customerName = customer != null ? customer.getName() : null;
        if (business != null) {
            sendReservationNotification(business, reservation, customerName,
                    NotificationType.RESERVATION_CONFIRMED);
        }

        // 9. 직원명 조회
        String staffName = null;
        if (staffId != null) {
            Staff staff = staffRepository.findById(staffId).orElse(null);
            if (staff != null) {
                staffName = staff.getName();
            }
        }

        // 10. 서비스명 조합
        String serviceName = reservation.getServiceNames() != null
                ? String.join(", ", reservation.getServiceNames()) : null;

        // 11. 업데이트된 예약 다시 조회하여 updatedAt 반영
        Reservation updatedReservation = reservationRepository.findById(reservationId).orElse(reservation);

        // 12. RescheduleResponse 반환
        return RescheduleResponse.builder()
                .id(reservationId)
                .reservationNumber(reservation.getReservationNumber())
                .customerName(customerName)
                .serviceName(serviceName)
                .staffName(staffName)
                .previousDate(previousDate)
                .previousStartTime(previousStartTime)
                .newDate(request.getNewDate())
                .newStartTime(request.getNewStartTime())
                .newEndTime(newEndTime)
                .status(reservation.getStatus().name())
                .updatedAt(updatedReservation.getUpdatedAt())
                .build();
    }

    // ========================================
    // 상태 변경
    // ========================================

    /**
     * 예약 상태 변경 (통합)
     */
    @Transactional
    public ReservationResponse updateReservationStatus(Long businessId, Long reservationId, ReservationStatus newStatus) {
        if (!reservationRepository.existsByBusinessIdAndId(businessId, reservationId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        // 상태별 검증
        switch (newStatus) {
            case CONFIRMED:
                if (!reservation.canConfirm()) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                            "대기 상태의 예약만 확정할 수 있습니다");
                }
                break;
            case COMPLETED:
                if (!reservation.canComplete()) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                            "확정된 예약만 완료할 수 있습니다");
                }
                break;
            case CANCELLED:
                if (!reservation.canCancel()) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                            "이미 완료되거나 취소된 예약은 취소할 수 없습니다");
                }
                break;
            case NO_SHOW:
                if (!reservation.canMarkAsNoShow()) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                            "확정된 예약만 노쇼 처리할 수 있습니다");
                }
                break;
        }

        reservationRepository.updateStatus(reservationId, newStatus);

        log.info("Reservation status updated: id={}, businessId={}, status={}",
                reservationId, businessId, newStatus);

        return getReservation(businessId, reservationId);
    }

    /**
     * 예약 확정
     */
    @Transactional
    public ReservationResponse confirmReservation(Long businessId, Long reservationId) {
        if (!reservationRepository.existsByBusinessIdAndId(businessId, reservationId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        if (!reservation.canConfirm()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "대기 상태의 예약만 확정할 수 있습니다");
        }

        reservationRepository.updateStatus(reservationId, ReservationStatus.CONFIRMED);

        // 알림 생성
        Business business = businessRepository.findById(businessId).orElse(null);
        Customer customer = customerRepository.findById(reservation.getCustomerId()).orElse(null);
        String customerName = customer != null ? customer.getName() : null;
        if (business != null) {
            sendReservationNotification(business, reservation, customerName, NotificationType.RESERVATION_CONFIRMED);

            // 외부 알림 로그 기록 (고객에게)
            if (customer != null) {
                sendExternalNotificationLog(business, reservation, customer.getPhone(), customer.getName(),
                        reservation.getServiceNames() != null ? String.join(", ", reservation.getServiceNames()) : "",
                        "confirmed");
            }
        }

        log.info("Reservation confirmed: id={}, businessId={}", reservationId, businessId);

        return getReservation(businessId, reservationId);
    }

    /**
     * 예약 완료
     */
    @Transactional
    public ReservationResponse completeReservation(Long businessId, Long reservationId) {
        if (!reservationRepository.existsByBusinessIdAndId(businessId, reservationId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        if (!reservation.canComplete()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "확정된 예약만 완료할 수 있습니다");
        }

        // 예약 상태 변경
        reservationRepository.updateStatus(reservationId, ReservationStatus.COMPLETED);

        // Customer 통계 업데이트 (visitCount +1, totalSpent +금액, lastVisitDate, tags 자동 업데이트)
        customerService.updateVisitStats(
                reservation.getCustomerId(),
                reservation.getTotalPrice(),
                reservation.getReservationDate()
        );

        // CustomerHistory 자동 생성
        customerHistoryService.createHistoryFromReservation(
                businessId,
                reservation.getCustomerId(),
                reservationId,
                reservation.getStaffId(),
                reservation.getReservationDate(),
                reservation.getServiceIds(),
                reservation.getServiceNames(),
                reservation.getTotalPrice()
        );

        // 알림 생성
        Business completedBusiness = businessRepository.findById(businessId).orElse(null);
        Customer completedCustomer = customerRepository.findById(reservation.getCustomerId()).orElse(null);
        String completedCustomerName = completedCustomer != null ? completedCustomer.getName() : null;
        if (completedBusiness != null) {
            sendReservationNotification(completedBusiness, reservation, completedCustomerName, NotificationType.RESERVATION_COMPLETED);

            // 외부 알림 로그 기록 - 리뷰 요청 (고객에게)
            if (completedCustomer != null) {
                sendExternalNotificationLog(completedBusiness, reservation, completedCustomer.getPhone(),
                        completedCustomer.getName(), null, "review_request");
            }
        }

        log.info("Reservation completed, customer stats updated, and history created: id={}, businessId={}, customerId={}",
                reservationId, businessId, reservation.getCustomerId());

        return getReservation(businessId, reservationId);
    }

    /**
     * 예약 취소
     */
    @Transactional
    public ReservationResponse cancelReservation(Long businessId, Long reservationId, String reason) {
        if (!reservationRepository.existsByBusinessIdAndId(businessId, reservationId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        if (!reservation.canCancel()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "이미 취소된 예약은 다시 취소할 수 없습니다");
        }

        // COMPLETED 상태였다면 고객 통계 롤백 필요
        boolean wasCompleted = reservation.isCompleted();

        // 취소 정보 업데이트
        Reservation cancelledReservation = Reservation.builder()
                .id(reservation.getId())
                .status(ReservationStatus.CANCELLED)
                .cancelledAt(LocalDateTime.now())
                .cancelReason(reason)
                .build();

        reservationRepository.updateCancellation(cancelledReservation);

        // COMPLETED 상태였던 예약 취소 시 고객 통계 롤백
        if (wasCompleted) {
            customerService.rollbackVisitStats(
                    reservation.getCustomerId(),
                    reservation.getTotalPrice()
            );

            log.info("Customer visit stats rolled back due to completed reservation cancellation: " +
                            "reservationId={}, customerId={}, amount={}",
                    reservationId, reservation.getCustomerId(), reservation.getTotalPrice());
        }

        // 예약 수 감소
        usageLimitService.decrementReservationCount(businessId);

        // 알림 생성
        Business cancelledBusiness = businessRepository.findById(businessId).orElse(null);
        Customer cancelledCustomer = customerRepository.findById(reservation.getCustomerId()).orElse(null);
        String cancelledCustomerName = cancelledCustomer != null ? cancelledCustomer.getName() : null;
        if (cancelledBusiness != null) {
            sendReservationNotification(cancelledBusiness, reservation, cancelledCustomerName, NotificationType.RESERVATION_CANCELLED);

            // SSE 실시간 이벤트 발송
            sseEmitterService.sendEventToBusinessOwner(businessId, "RESERVATION_CANCELLED", SseEventData.builder()
                    .type("RESERVATION_CANCELLED")
                    .referenceId(reservation.getId())
                    .reservationNumber(reservation.getReservationNumber())
                    .customerName(cancelledCustomerName)
                    .startTime(reservation.getReservationDate() + " " + reservation.getStartTime())
                    .reason(reason)
                    .message("예약이 취소되었습니다.")
                    .createdAt(LocalDateTime.now())
                    .build());

            // 외부 알림 로그 기록 (고객에게)
            if (cancelledCustomer != null) {
                sendExternalNotificationLog(cancelledBusiness, reservation, cancelledCustomer.getPhone(),
                        cancelledCustomer.getName(),
                        reservation.getServiceNames() != null ? String.join(", ", reservation.getServiceNames()) : "",
                        "cancelled");
            }
        }

        log.info("Reservation cancelled: id={}, businessId={}, wasCompleted={}, reason={}",
                reservationId, businessId, wasCompleted, reason);

        return getReservation(businessId, reservationId);
    }

    /**
     * 노쇼 처리
     */
    @Transactional
    public ReservationResponse markAsNoShow(Long businessId, Long reservationId) {
        if (!reservationRepository.existsByBusinessIdAndId(businessId, reservationId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        if (!reservation.canMarkAsNoShow()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "확정된 예약만 노쇼 처리할 수 있습니다");
        }

        reservationRepository.updateStatus(reservationId, ReservationStatus.NO_SHOW);

        // 알림 생성
        Business noShowBusiness = businessRepository.findById(businessId).orElse(null);
        String noShowCustomerName = getCustomerName(reservation.getCustomerId());
        if (noShowBusiness != null) {
            sendReservationNotification(noShowBusiness, reservation, noShowCustomerName, NotificationType.RESERVATION_NO_SHOW);
        }

        log.info("Reservation marked as no-show: id={}, businessId={}", reservationId, businessId);

        return getReservation(businessId, reservationId);
    }

    // ========================================
    // 삭제
    // ========================================

    /**
     * 예약 삭제
     */
    @Transactional
    public void deleteReservation(Long businessId, Long reservationId) {
        if (!reservationRepository.existsByBusinessIdAndId(businessId, reservationId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        reservationRepository.delete(reservationId);

        log.info("Reservation deleted: id={}, businessId={}", reservationId, businessId);
    }

    // ========================================
    // 가용성 확인
    // ========================================

    /**
     * 직원 가용 시간 확인
     * FE에서 예약 생성/수정 전 호출하여 시간 충돌 여부 사전 확인
     */
    public AvailabilityResponse checkAvailability(Long businessId, Long staffId, LocalDate date,
                                                   LocalTime startTime, LocalTime endTime,
                                                   Long excludeReservationId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        if (staffId == null) {
            return AvailabilityResponse.builder()
                    .available(true)
                    .conflicts(List.of())
                    .build();
        }

        List<Reservation> conflicting = reservationRepository.findConflictingReservations(
                staffId, date, startTime, endTime);

        // 수정 시 현재 예약 제외
        if (excludeReservationId != null) {
            conflicting = conflicting.stream()
                    .filter(r -> !r.getId().equals(excludeReservationId))
                    .collect(Collectors.toList());
        }

        if (conflicting.isEmpty()) {
            return AvailabilityResponse.builder()
                    .available(true)
                    .conflicts(List.of())
                    .build();
        }

        List<AvailabilityResponse.ConflictInfo> conflicts = conflicting.stream()
                .map(r -> AvailabilityResponse.ConflictInfo.builder()
                        .reservationId(r.getId())
                        .customerName(getCustomerName(r.getCustomerId()))
                        .startTime(r.getStartTime())
                        .endTime(r.getEndTime())
                        .serviceName(r.getServiceNames() != null ?
                                String.join(", ", r.getServiceNames()) : null)
                        .build())
                .collect(Collectors.toList());

        return AvailabilityResponse.builder()
                .available(false)
                .conflicts(conflicts)
                .build();
    }

    // ========================================
    // 일괄 상태 변경
    // ========================================

    /**
     * 다중 예약 일괄 상태 변경
     */
    @Transactional
    public BulkStatusChangeResponse bulkUpdateStatus(Long businessId, BulkStatusChangeRequest request) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        List<Long> successIds = new ArrayList<>();
        List<BulkStatusChangeResponse.FailedItem> failedItems = new ArrayList<>();

        for (Long reservationId : request.getReservationIds()) {
            try {
                // 매장 소속 확인
                if (!reservationRepository.existsByBusinessIdAndId(businessId, reservationId)) {
                    failedItems.add(BulkStatusChangeResponse.FailedItem.builder()
                            .reservationId(reservationId)
                            .reason("예약을 찾을 수 없습니다")
                            .build());
                    continue;
                }

                Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
                if (reservation == null) {
                    failedItems.add(BulkStatusChangeResponse.FailedItem.builder()
                            .reservationId(reservationId)
                            .reason("예약을 찾을 수 없습니다")
                            .build());
                    continue;
                }

                // 상태 전이 유효성 검증
                String validationError = validateStatusTransition(reservation, request.getStatus());
                if (validationError != null) {
                    failedItems.add(BulkStatusChangeResponse.FailedItem.builder()
                            .reservationId(reservationId)
                            .reason(validationError)
                            .build());
                    continue;
                }

                // 상태별 처리
                switch (request.getStatus()) {
                    case CONFIRMED -> {
                        reservationRepository.updateStatus(reservationId, ReservationStatus.CONFIRMED);
                    }
                    case COMPLETED -> {
                        reservationRepository.updateStatus(reservationId, ReservationStatus.COMPLETED);
                        customerService.updateVisitStats(
                                reservation.getCustomerId(),
                                reservation.getTotalPrice(),
                                reservation.getReservationDate()
                        );
                        customerHistoryService.createHistoryFromReservation(
                                businessId, reservation.getCustomerId(), reservationId,
                                reservation.getStaffId(), reservation.getReservationDate(),
                                reservation.getServiceIds(), reservation.getServiceNames(),
                                reservation.getTotalPrice()
                        );
                    }
                    case CANCELLED -> {
                        Reservation cancelledReservation = Reservation.builder()
                                .id(reservationId)
                                .status(ReservationStatus.CANCELLED)
                                .cancelledAt(LocalDateTime.now())
                                .cancelReason("일괄 취소")
                                .build();
                        reservationRepository.updateCancellation(cancelledReservation);
                        usageLimitService.decrementReservationCount(businessId);
                    }
                    default -> {
                        failedItems.add(BulkStatusChangeResponse.FailedItem.builder()
                                .reservationId(reservationId)
                                .reason("일괄 변경이 지원되지 않는 상태입니다: " + request.getStatus())
                                .build());
                        continue;
                    }
                }

                successIds.add(reservationId);

            } catch (Exception e) {
                log.warn("Bulk status change failed for reservation {}: {}", reservationId, e.getMessage());
                failedItems.add(BulkStatusChangeResponse.FailedItem.builder()
                        .reservationId(reservationId)
                        .reason(e.getMessage())
                        .build());
            }
        }

        log.info("Bulk status change completed: businessId={}, status={}, success={}, failed={}",
                businessId, request.getStatus(), successIds.size(), failedItems.size());

        return BulkStatusChangeResponse.builder()
                .success(successIds)
                .failed(failedItems)
                .build();
    }

    /**
     * 상태 전이 유효성 검증 (일괄 변경용)
     * 유효하면 null 반환, 유효하지 않으면 에러 메시지 반환
     */
    private String validateStatusTransition(Reservation reservation, ReservationStatus newStatus) {
        return switch (newStatus) {
            case CONFIRMED -> reservation.canConfirm() ? null : "대기 상태의 예약만 확정할 수 있습니다 (현재: " + reservation.getStatus() + ")";
            case COMPLETED -> reservation.canComplete() ? null : "확정된 예약만 완료할 수 있습니다 (현재: " + reservation.getStatus() + ")";
            case CANCELLED -> reservation.canCancel() ? null : "이미 취소되거나 완료된 예약입니다 (현재: " + reservation.getStatus() + ")";
            default -> "일괄 변경이 지원되지 않는 상태입니다: " + newStatus;
        };
    }

    // ========================================
    // 검증
    // ========================================

    /**
     * 예약 가능 여부 검증
     */
    private void validateReservation(Long businessId, Long staffId, LocalDate date,
                                     LocalTime startTime, LocalTime endTime, Long excludeId) {
        // 1. 휴무일 확인
        Optional<SpecialHoliday> holiday = specialHolidayRepository.findByBusinessIdAndDate(businessId, date);
        if (holiday.isPresent()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "해당 날짜는 휴무일입니다: " + date +
                            (holiday.get().getReason() != null ? " (" + holiday.get().getReason() + ")" : ""));
        }

        // 2. 과거 날짜 확인
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "과거 날짜는 예약할 수 없습니다");
        }

        // 3. 스태프 근무 스케줄 확인 (staffId가 있는 경우)
        if (staffId != null) {
            int dayOfWeek = date.getDayOfWeek().getValue(); // ISO-8601: 1=월 ~ 7=일
            Optional<StaffSchedule> schedule = staffScheduleRepository.findByStaffIdAndDayOfWeek(staffId, dayOfWeek);

            if (schedule.isPresent()) {
                StaffSchedule ss = schedule.get();
                if (!ss.isWorkingDay()) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                            "해당 직원의 휴무일입니다: " + date.getDayOfWeek());
                }

                // 근무 시간 범위 체크
                if (startTime.isBefore(ss.getStartTime()) || endTime.isAfter(ss.getEndTime())) {
                    throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                            String.format("해당 직원의 근무시간(%s~%s)을 벗어납니다",
                                    ss.getStartTime(), ss.getEndTime()));
                }

                // 휴식 시간 겹침 체크
                if (ss.hasBreakTime()) {
                    if (startTime.isBefore(ss.getBreakEndTime()) && endTime.isAfter(ss.getBreakStartTime())) {
                        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                                String.format("해당 직원의 휴식시간(%s~%s)과 겹칩니다",
                                        ss.getBreakStartTime(), ss.getBreakEndTime()));
                    }
                }
            }
            // 스케줄이 없으면 제한 없이 예약 가능 (아직 스케줄 미설정)
        }

        // 4. 시간 충돌 확인 (staffId가 있는 경우만)
        if (staffId != null) {
            List<Reservation> conflicting = reservationRepository.findConflictingReservations(
                    staffId, date, startTime, endTime);

            // excludeId가 있으면 해당 예약은 제외 (수정 시)
            if (excludeId != null) {
                conflicting = conflicting.stream()
                        .filter(r -> !r.getId().equals(excludeId))
                        .collect(Collectors.toList());
            }

            if (!conflicting.isEmpty()) {
                Reservation conflict = conflicting.get(0);
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        String.format("해당 시간에 이미 예약이 있습니다 (%s %s-%s)",
                                conflict.getReservationDate(),
                                conflict.getStartTime(),
                                conflict.getEndTime()));
            }
        }
    }

    // ========================================
    // 알림 헬퍼
    // ========================================

    /**
     * 예약 관련 알림 생성
     */
    private void sendReservationNotification(Business business, Reservation reservation,
                                              String customerName, NotificationType type) {
        try {
            String title = switch (type) {
                case RESERVATION_NEW -> "새 예약이 등록되었습니다";
                case RESERVATION_CONFIRMED -> "예약이 확정되었습니다";
                case RESERVATION_CANCELLED -> "예약이 취소되었습니다";
                case RESERVATION_COMPLETED -> "예약이 완료되었습니다";
                case RESERVATION_NO_SHOW -> "고객 노쇼가 처리되었습니다";
                default -> "예약 상태가 변경되었습니다";
            };

            String message = String.format("%s 고객님 - %s %s",
                    customerName != null ? customerName : "미확인",
                    reservation.getReservationDate(),
                    reservation.getStartTime() != null ? reservation.getStartTime().toString() : "");

            notificationService.createNotification(
                    business.getOwnerId(),
                    business.getId(),
                    type,
                    title,
                    message,
                    "/shop-admin/reservations/list",
                    "Reservation",
                    reservation.getId()
            );
        } catch (Exception e) {
            log.warn("Failed to create notification: {}", e.getMessage());
        }
    }

    /**
     * 외부 알림 로그 기록 (고객 대상 카카오/SMS)
     */
    private void sendExternalNotificationLog(Business business, Reservation reservation,
                                              String recipientPhone, String recipientName,
                                              String serviceName, String type) {
        try {
            Map<String, String> params = Map.of(
                    "businessName", business.getName(),
                    "date", reservation.getReservationDate().toString(),
                    "time", reservation.getStartTime() != null ? reservation.getStartTime().toString() : "",
                    "serviceName", serviceName != null ? serviceName : ""
            );

            switch (type) {
                case "created" -> notificationSender.sendReservationCreated(
                        business.getId(), reservation.getId(), recipientPhone, recipientName, params);
                case "confirmed" -> notificationSender.sendReservationConfirmed(
                        business.getId(), reservation.getId(), recipientPhone, recipientName, params);
                case "cancelled" -> notificationSender.sendReservationCancelled(
                        business.getId(), reservation.getId(), recipientPhone, recipientName, params);
                case "review_request" -> notificationSender.sendReviewRequest(
                        business.getId(), reservation.getId(), recipientPhone, recipientName, params);
            }
        } catch (Exception e) {
            log.warn("Failed to send external notification log: {}", e.getMessage());
        }
    }

    /**
     * 고객 이름 조회 헬퍼
     */
    private String getCustomerName(Long customerId) {
        if (customerId == null) return null;
        return customerRepository.findById(customerId)
                .map(Customer::getName)
                .orElse(null);
    }

    /**
     * Reservation → ReservationResponse 변환 (고객명, 직원명 포함)
     */
    private ReservationResponse enrichReservationResponse(Reservation reservation) {
        // Customer 조회
        String customerName = null;
        String customerPhone = null;
        if (reservation.getCustomerId() != null) {
            Customer customer = customerRepository.findById(reservation.getCustomerId())
                    .orElse(null);
            if (customer != null) {
                customerName = customer.getName();
                customerPhone = customer.getPhone();
            }
        }

        // Staff 조회
        String staffName = null;
        if (reservation.getStaffId() != null) {
            Staff staff = staffRepository.findById(reservation.getStaffId())
                    .orElse(null);
            if (staff != null) {
                staffName = staff.getName();
            }
        }

        // Response 생성
        return ReservationResponse.builder()
                .id(reservation.getId())
                .businessId(reservation.getBusinessId())
                .customerId(reservation.getCustomerId())
                .staffId(reservation.getStaffId())
                .customerName(customerName)
                .customerPhone(customerPhone)
                .staffName(staffName)
                .reservationNumber(reservation.getReservationNumber())
                .reservationDate(reservation.getReservationDate())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .serviceIds(reservation.getServiceIds())
                .serviceNames(reservation.getServiceNames())
                .totalDuration(reservation.getTotalDuration())
                .totalPrice(reservation.getTotalPrice())
                .status(reservation.getStatus())
                .customerMemo(reservation.getCustomerMemo())
                .staffMemo(reservation.getStaffMemo())
                .cancelledAt(reservation.getCancelledAt())
                .cancelReason(reservation.getCancelReason())
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}