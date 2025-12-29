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
import io.moer.booking.domain.holiday.SpecialHoliday;
import io.moer.booking.domain.holiday.repository.SpecialHolidayRepository;
import io.moer.booking.domain.reservation.Reservation;
import io.moer.booking.domain.reservation.ReservationStatus;
import io.moer.booking.domain.reservation.dto.ReservationCreateRequest;
import io.moer.booking.domain.reservation.dto.ReservationResponse;
import io.moer.booking.domain.reservation.dto.ReservationSearchCondition;
import io.moer.booking.domain.reservation.dto.ReservationUpdateRequest;
import io.moer.booking.domain.reservation.repository.ReservationRepository;
import io.moer.booking.domain.service.Service;
import io.moer.booking.domain.service.repository.ServiceRepository;
import io.moer.booking.domain.staff.Staff;
import io.moer.booking.domain.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BusinessRepository businessRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;  // 추가
    private final StaffRepository staffRepository;
    private final ServiceRepository serviceRepository;
    private final SpecialHolidayRepository specialHolidayRepository;
    private final CustomerHistoryService customerHistoryService;

    /**
     * 예약 생성 (Customer 자동 생성 지원)
     */
    @Transactional
    public ReservationResponse createReservation(Long businessId, ReservationCreateRequest request) {
        // 1. Business 존재 확인
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND));

        // 2. Customer 조회 또는 자동 생성
        Customer customer = resolveCustomer(businessId, request);

        // 3. Staff 존재 확인 (선택 시)
        if (request.getStaffId() != null) {
            if (!staffRepository.existsById(request.getStaffId())) {
                throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND, "직원을 찾을 수 없습니다");
            }
        }

        // 4. Service 조회 및 검증
        List<Service> services = request.getServiceIds().stream()
                .map(serviceId -> serviceRepository.findById(serviceId)
                        .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND,
                                "서비스를 찾을 수 없습니다: " + serviceId)))
                .collect(Collectors.toList());

        // 5. 총 시간 및 가격 계산
        int totalDuration = services.stream()
                .mapToInt(Service::getDuration)
                .sum();

        int totalPrice = services.stream()
                .mapToInt(Service::getPrice)
                .sum();

        List<String> serviceNames = services.stream()
                .map(Service::getName)
                .collect(Collectors.toList());

        // 6. 종료 시간 계산
        LocalTime endTime = request.getStartTime().plusMinutes(totalDuration);

        // 7. 예약 가능 여부 검증
        validateReservation(businessId, request.getStaffId(), request.getReservationDate(),
                request.getStartTime(), endTime, null);

        // 8. 예약 번호 생성
        String reservationNumber = generateReservationNumber(request.getReservationDate());

        // 9. Reservation 엔티티 생성
        Map<String, Object> notificationSent = new HashMap<>();
        notificationSent.put("confirmed", false);
        notificationSent.put("reminder", false);
        notificationSent.put("completed", false);

        Reservation reservation = Reservation.builder()
                .businessId(businessId)
                .customerId(customer.getId())  // 자동 생성된 또는 기존 Customer ID
                .staffId(request.getStaffId())
                .reservationNumber(reservationNumber)
                .reservationDate(request.getReservationDate())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .serviceIds(request.getServiceIds())
                .serviceNames(serviceNames)
                .totalDuration(totalDuration)
                .totalPrice(totalPrice)
                .status(ReservationStatus.PENDING)
                .customerRequest(request.getCustomerRequest())
                .notificationSent(notificationSent)
                .build();

        // 10. 저장
        reservationRepository.save(reservation);

        log.info("Reservation created: id={}, businessId={}, customerId={} ({}), date={}, time={}",
                reservation.getId(), businessId, customer.getId(),
                customer.getName(), request.getReservationDate(), request.getStartTime());

        return ReservationResponse.from(reservation);
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
        return customerService.findOrCreateCustomer(
                businessId,
                request.getCustomerName(),
                request.getCustomerPhone()
        );
    }

    /**
     * 예약 가능 여부 검증
     */
    private void validateReservation(Long businessId, Long staffId, LocalDate date,
                                     LocalTime startTime, LocalTime endTime, Long excludeId) {
        // 1. 휴무일 확인
        Optional<SpecialHoliday> holiday = specialHolidayRepository.findByBusinessIdAndDate(businessId, date);
        if (holiday.isPresent() && holiday.get().getIsClosed()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "해당 날짜는 휴무일입니다: " + date);
        }

        // 2. 과거 날짜 확인
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "과거 날짜는 예약할 수 없습니다");
        }

        // 3. 시간 겹침 확인
        List<Reservation> overlapping = reservationRepository.findOverlappingReservations(
                businessId, staffId, date, startTime, endTime, excludeId);

        if (!overlapping.isEmpty()) {
            throw new BusinessException(ErrorCode.RESERVATION_TIME_CONFLICT,
                    "해당 시간에 이미 예약이 있습니다");
        }
    }

    /**
     * 예약 번호 생성
     * 형식: RES-YYYYMMDD-0001
     */
    private String generateReservationNumber(LocalDate date) {
        Long sequence = reservationRepository.getNextReservationNumber();
        String dateStr = date.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return String.format("RES-%s-%04d", dateStr, sequence);
    }

    /**
     * 예약 단건 조회
     */
    public ReservationResponse getReservation(Long businessId, Long reservationId) {
        if (!reservationRepository.existsByBusinessIdAndId(businessId, reservationId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        return ReservationResponse.from(reservation);
    }

    /**
     * 예약 번호로 조회
     */
    public ReservationResponse getReservationByNumber(String reservationNumber) {
        Reservation reservation = reservationRepository.findByReservationNumber(reservationNumber)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND,
                        "예약을 찾을 수 없습니다: " + reservationNumber));

        return ReservationResponse.from(reservation);
    }

    /**
     * Business의 전체 예약 조회
     */
    public List<ReservationResponse> getReservationsByBusiness(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return reservationRepository.findByBusinessId(businessId).stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 날짜별 예약 조회
     */
    public List<ReservationResponse> getReservationsByDate(Long businessId, LocalDate date) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return reservationRepository.findByBusinessIdAndDate(businessId, date).stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Customer의 예약 조회
     */
    public List<ReservationResponse> getReservationsByCustomer(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND, "고객을 찾을 수 없습니다");
        }

        return reservationRepository.findByCustomerId(customerId).stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Staff의 예약 조회
     */
    public List<ReservationResponse> getReservationsByStaff(Long staffId) {
        if (!staffRepository.existsById(staffId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND, "직원을 찾을 수 없습니다");
        }

        return reservationRepository.findByStaffId(staffId).stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 조건별 예약 검색
     */
    public List<ReservationResponse> searchReservations(ReservationSearchCondition condition) {
        if (!businessRepository.existsById(condition.getBusinessId())) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return reservationRepository.findByCondition(condition).stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }

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
        if (request.getStaffId() != null && !staffRepository.existsById(request.getStaffId())) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND, "직원을 찾을 수 없습니다");
        }

        // 날짜/시간 변경 시 재계산
        LocalDate newDate = request.getReservationDate() != null ?
                request.getReservationDate() : reservation.getReservationDate();
        LocalTime newStartTime = request.getStartTime() != null ?
                request.getStartTime() : reservation.getStartTime();

        List<Long> newServiceIds = request.getServiceIds() != null ?
                request.getServiceIds() : reservation.getServiceIds();

        // 서비스 변경 시 시간/가격 재계산
        int totalDuration = reservation.getTotalDuration();
        int totalPrice = reservation.getTotalPrice();
        List<String> serviceNames = reservation.getServiceNames();

        if (request.getServiceIds() != null) {
            List<Service> services = request.getServiceIds().stream()
                    .map(serviceId -> serviceRepository.findById(serviceId)
                            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND,
                                    "서비스를 찾을 수 없습니다: " + serviceId)))
                    .collect(Collectors.toList());

            totalDuration = services.stream().mapToInt(Service::getDuration).sum();
            totalPrice = services.stream().mapToInt(Service::getPrice).sum();
            serviceNames = services.stream().map(Service::getName).collect(Collectors.toList());
        }

        LocalTime newEndTime = newStartTime.plusMinutes(totalDuration);

        // 시간 겹침 검증
        if (request.getReservationDate() != null || request.getStartTime() != null ||
                request.getServiceIds() != null) {
            Long staffId = request.getStaffId() != null ? request.getStaffId() : reservation.getStaffId();
            validateReservation(businessId, staffId, newDate, newStartTime, newEndTime, reservationId);
        }

        // 수정
        Reservation updatedReservation = Reservation.builder()
                .id(reservation.getId())
                .staffId(request.getStaffId() != null ? request.getStaffId() : reservation.getStaffId())
                .reservationDate(newDate)
                .startTime(newStartTime)
                .endTime(newEndTime)
                .serviceIds(newServiceIds)
                .serviceNames(serviceNames)
                .totalDuration(totalDuration)
                .totalPrice(totalPrice)
                .customerRequest(request.getCustomerRequest() != null ?
                        request.getCustomerRequest() : reservation.getCustomerRequest())
                .adminMemo(request.getAdminMemo() != null ?
                        request.getAdminMemo() : reservation.getAdminMemo())
                .build();

        reservationRepository.update(updatedReservation);

        log.info("Reservation updated: id={}, businessId={}", reservationId, businessId);

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

        // TODO: 카카오톡 알림 발송

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

        log.info("Reservation completed and history created: id={}, businessId={}",
                reservationId, businessId);

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
                    "대기 또는 확정 상태의 예약만 취소할 수 있습니다");
        }

        reservation.cancel(reason);

        Reservation updatedReservation = Reservation.builder()
                .id(reservation.getId())
                .status(ReservationStatus.CANCELLED)
                .cancelledAt(reservation.getCancelledAt())
                .cancelReason(reservation.getCancelReason())
                .build();

        reservationRepository.update(updatedReservation);

        // TODO: 카카오톡 알림 발송

        log.info("Reservation cancelled: id={}, businessId={}, reason={}",
                reservationId, businessId, reason);

        return getReservation(businessId, reservationId);
    }

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
}