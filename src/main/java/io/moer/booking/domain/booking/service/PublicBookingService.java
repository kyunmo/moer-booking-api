package io.moer.booking.domain.booking.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.booking.dto.*;
import io.moer.booking.domain.holiday.SpecialHoliday;
import io.moer.booking.domain.business.Business;
import io.moer.booking.domain.business.BusinessSettings;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.business.repository.BusinessSettingsRepository;
import io.moer.booking.domain.customer.Customer;
import io.moer.booking.domain.customer.repository.CustomerRepository;
import io.moer.booking.domain.holiday.repository.SpecialHolidayRepository;
import io.moer.booking.domain.reservation.Reservation;
import io.moer.booking.domain.reservation.ReservationStatus;
import io.moer.booking.domain.reservation.dto.ReservationCreateRequest;
import io.moer.booking.domain.reservation.dto.ReservationResponse;
import io.moer.booking.domain.reservation.repository.ReservationRepository;
import io.moer.booking.domain.reservation.service.ReservationService;
import io.moer.booking.domain.service.repository.ServiceRepository;
import io.moer.booking.domain.staff.Staff;
import io.moer.booking.domain.staff.StaffSchedule;
import io.moer.booking.domain.staff.repository.StaffRepository;
import io.moer.booking.domain.staff.repository.StaffScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 고객용 예약 Public API 서비스
 * 기존 서비스를 조합하여 Public(비인증) 예약 로직을 구현합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicBookingService {

    private final BusinessRepository businessRepository;
    private final BusinessSettingsRepository businessSettingsRepository;
    private final ReservationService reservationService;
    private final ReservationRepository reservationRepository;
    private final StaffRepository staffRepository;
    private final StaffScheduleRepository staffScheduleRepository;
    private final ServiceRepository serviceRepository;
    private final CustomerRepository customerRepository;
    private final SpecialHolidayRepository specialHolidayRepository;

    // ========================================
    // 매장 휴무일 조회
    // ========================================

    /**
     * 매장 휴무일 목록 조회
     *
     * @param slug 매장 슬러그
     * @param year 조회 연도 (null이면 전체)
     */
    public List<PublicHolidayResponse> getHolidays(String slug, Integer year) {
        Business business = findBusinessBySlug(slug);
        Long businessId = business.getId();

        List<SpecialHoliday> holidays;
        if (year != null) {
            holidays = specialHolidayRepository.findByBusinessIdAndYear(businessId, year);
        } else {
            holidays = specialHolidayRepository.findByBusinessId(businessId);
        }

        return holidays.stream()
                .map(PublicHolidayResponse::from)
                .collect(Collectors.toList());
    }

    // ========================================
    // 예약 가능 날짜 조회
    // ========================================

    /**
     * 해당 월의 예약 가능 날짜 목록 조회
     *
     * @param slug      매장 슬러그
     * @param staffId   스태프 ID (선택)
     * @param serviceId 서비스 ID (선택, duration 계산용)
     * @param yearMonth 조회 년월 (예: "2026-02")
     */
    public AvailableDateResponse getAvailableDates(String slug, Long staffId, Long serviceId, YearMonth yearMonth) {
        Business business = findBusinessBySlug(slug);
        Long businessId = business.getId();

        // 서비스 duration 조회 (지정 시)
        int serviceDuration = 0;
        if (serviceId != null) {
            io.moer.booking.domain.service.Service service = serviceRepository.findById(serviceId)
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SERVICE_NOT_FOUND));
            serviceDuration = service.getDuration();
        }

        // 해당 월의 날짜 순회
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        LocalDate today = LocalDate.now();

        List<AvailableDateResponse.DateSlot> dateSlots = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            boolean hasSlots = checkDateHasSlots(businessId, staffId, date, today);
            dateSlots.add(AvailableDateResponse.DateSlot.builder()
                    .date(date)
                    .hasSlots(hasSlots)
                    .build());
        }

        return AvailableDateResponse.builder()
                .month(yearMonth.toString())
                .availableDates(dateSlots)
                .build();
    }

    /**
     * 특정 날짜에 예약 가능한 슬롯이 있는지 검사
     */
    private boolean checkDateHasSlots(Long businessId, Long staffId, LocalDate date, LocalDate today) {
        // 1. 과거 날짜 체크
        if (date.isBefore(today)) {
            return false;
        }

        // 2. 휴무일 체크
        if (specialHolidayRepository.existsByBusinessIdAndDate(businessId, date)) {
            return false;
        }

        // 3. 스태프 근무일 체크
        int dayOfWeek = date.getDayOfWeek().getValue();

        if (staffId != null) {
            // 특정 스태프 지정된 경우
            Optional<StaffSchedule> schedule = staffScheduleRepository.findByStaffIdAndDayOfWeek(staffId, dayOfWeek);
            if (schedule.isPresent() && !schedule.get().isWorkingDay()) {
                return false;
            }
            // 스케줄이 없으면 아직 미설정이므로 예약 가능으로 처리
        } else {
            // 스태프 미지정: 최소 1명의 활성 스태프가 해당 요일에 근무하는지 확인
            List<Staff> activeStaffs = staffRepository.findActiveByBusinessId(businessId);
            if (activeStaffs.isEmpty()) {
                return false;
            }

            boolean anyWorking = false;
            for (Staff staff : activeStaffs) {
                Optional<StaffSchedule> schedule = staffScheduleRepository.findByStaffIdAndDayOfWeek(staff.getId(), dayOfWeek);
                if (schedule.isEmpty() || schedule.get().isWorkingDay()) {
                    // 스케줄 미설정이면 근무로 간주, 설정되어 있으면 isWorkingDay 확인
                    anyWorking = true;
                    break;
                }
            }
            if (!anyWorking) {
                return false;
            }
        }

        return true;
    }

    // ========================================
    // 예약 가능 시간 조회
    // ========================================

    /**
     * 특정 날짜의 예약 가능 시간 슬롯 조회
     *
     * @param slug      매장 슬러그
     * @param date      날짜
     * @param serviceId 서비스 ID (duration 계산 필수)
     * @param staffId   스태프 ID (선택)
     */
    public AvailableTimeSlotResponse getAvailableTimes(String slug, LocalDate date, Long serviceId, Long staffId) {
        Business business = findBusinessBySlug(slug);
        Long businessId = business.getId();

        // 서비스 duration 조회
        io.moer.booking.domain.service.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.SERVICE_NOT_FOUND));

        if (!service.getBusinessId().equals(businessId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "해당 매장의 서비스가 아닙니다");
        }

        int duration = service.getDuration();

        // BusinessSettings에서 예약 간격 조회
        BusinessSettings settings = businessSettingsRepository.findByBusinessId(businessId).orElse(null);
        int bookingInterval = (settings != null && settings.getBookingInterval() != null)
                ? settings.getBookingInterval() : 30;

        // 대상 스태프 목록 결정
        List<Staff> targetStaffs;
        if (staffId != null) {
            Staff staff = staffRepository.findById(staffId)
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND));
            if (!staff.getBusinessId().equals(businessId)) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "해당 매장의 스태프가 아닙니다");
            }
            targetStaffs = List.of(staff);
        } else {
            // 서비스 담당 가능한 스태프 필터링
            List<Long> serviceStaffIds = service.getStaffIdList();
            List<Staff> activeStaffs = staffRepository.findActiveByBusinessId(businessId);

            if (serviceStaffIds.isEmpty()) {
                // staffIds가 비어있으면 모든 활성 스태프
                targetStaffs = activeStaffs;
            } else {
                targetStaffs = activeStaffs.stream()
                        .filter(s -> serviceStaffIds.contains(s.getId()))
                        .collect(Collectors.toList());
            }
        }

        // 해당 날짜의 예약 목록 조회 (PENDING, CONFIRMED만)
        List<Reservation> dayReservations = reservationRepository.findByBusinessIdAndDate(businessId, date);
        List<Reservation> activeReservations = dayReservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.PENDING
                        || r.getStatus() == ReservationStatus.CONFIRMED)
                .toList();

        // 각 스태프별 가용 슬롯 계산 후 통합
        int dayOfWeek = date.getDayOfWeek().getValue();
        Map<String, AvailableTimeSlotResponse.TimeSlot> slotMap = new LinkedHashMap<>();

        for (Staff staff : targetStaffs) {
            Optional<StaffSchedule> scheduleOpt = staffScheduleRepository.findByStaffIdAndDayOfWeek(staff.getId(), dayOfWeek);

            // 스케줄이 없거나 비근무일이면 건너뜀
            if (scheduleOpt.isPresent() && !scheduleOpt.get().isWorkingDay()) {
                continue;
            }

            // 스케줄이 없으면 건너뜀 (근무 시간을 알 수 없으므로)
            if (scheduleOpt.isEmpty()) {
                continue;
            }

            StaffSchedule schedule = scheduleOpt.get();

            // 스태프별 예약 필터링
            List<Reservation> staffReservations = activeReservations.stream()
                    .filter(r -> staff.getId().equals(r.getStaffId()))
                    .sorted(Comparator.comparing(Reservation::getStartTime))
                    .toList();

            // 가용 시간 블록 계산
            List<TimeBlock> availableBlocks = calculateAvailableBlocks(
                    schedule.getStartTime(),
                    schedule.getEndTime(),
                    schedule.getBreakStartTime(),
                    schedule.getBreakEndTime(),
                    staffReservations
            );

            // 블록을 bookingInterval 단위 슬롯으로 분할
            for (TimeBlock block : availableBlocks) {
                LocalTime slotStart = block.start;
                while (true) {
                    LocalTime slotEnd = slotStart.plusMinutes(duration);
                    // 블록 끝 시간을 넘으면 종료
                    if (slotEnd.isAfter(block.end)) {
                        break;
                    }

                    String slotKey = slotStart.toString() + "-" + slotEnd.toString();
                    AvailableTimeSlotResponse.StaffInfo staffInfo = AvailableTimeSlotResponse.StaffInfo.builder()
                            .id(staff.getId())
                            .name(staff.getName())
                            .build();

                    if (slotMap.containsKey(slotKey)) {
                        // 이미 존재하는 슬롯에 스태프 추가
                        AvailableTimeSlotResponse.TimeSlot existingSlot = slotMap.get(slotKey);
                        List<AvailableTimeSlotResponse.StaffInfo> staffList = new ArrayList<>(existingSlot.getAvailableStaffs());
                        staffList.add(staffInfo);

                        slotMap.put(slotKey, AvailableTimeSlotResponse.TimeSlot.builder()
                                .startTime(existingSlot.getStartTime())
                                .endTime(existingSlot.getEndTime())
                                .availableStaffs(staffList)
                                .build());
                    } else {
                        slotMap.put(slotKey, AvailableTimeSlotResponse.TimeSlot.builder()
                                .startTime(slotStart)
                                .endTime(slotEnd)
                                .availableStaffs(new ArrayList<>(List.of(staffInfo)))
                                .build());
                    }

                    slotStart = slotStart.plusMinutes(bookingInterval);
                }
            }
        }

        // 시간순 정렬
        List<AvailableTimeSlotResponse.TimeSlot> sortedSlots = slotMap.values().stream()
                .sorted(Comparator.comparing(AvailableTimeSlotResponse.TimeSlot::getStartTime))
                .toList();

        return AvailableTimeSlotResponse.builder()
                .date(date)
                .serviceDuration(duration)
                .availableSlots(sortedSlots)
                .build();
    }

    // ========================================
    // 예약 생성
    // ========================================

    /**
     * 고객용 예약 생성
     * 기존 ReservationService.createReservation()을 재활용합니다.
     */
    @Transactional
    public PublicReservationResponse createReservation(String slug, PublicReservationCreateRequest request) {
        Business business = findBusinessBySlug(slug);
        Long businessId = business.getId();

        // 1. BusinessSettings 조회
        BusinessSettings settings = businessSettingsRepository.findByBusinessId(businessId).orElse(null);

        // 2. 온라인 예약 허용 여부 검증
        if (settings != null && !settings.allowsOnlineBooking()) {
            throw new BusinessException(ErrorCode.ONLINE_BOOKING_DISABLED,
                    "해당 매장은 온라인 예약이 비활성화되어 있습니다");
        }

        // 3. 최대 사전 예약 일수 검증
        if (settings != null && settings.getMaxAdvanceBookingDays() != null) {
            LocalDate maxDate = LocalDate.now().plusDays(settings.getMaxAdvanceBookingDays());
            if (request.getReservationDate().isAfter(maxDate)) {
                throw new BusinessException(ErrorCode.ADVANCE_BOOKING_EXCEEDED,
                        String.format("예약 가능 기간을 초과했습니다 (최대 %d일 후까지)", settings.getMaxAdvanceBookingDays()));
            }
        }

        // 4. 최소 사전 예약 시간 검증
        if (settings != null && settings.getMinAdvanceBookingHours() != null) {
            LocalDateTime reservationDateTime = LocalDateTime.of(request.getReservationDate(), request.getStartTime());
            LocalDateTime minDateTime = LocalDateTime.now().plusHours(settings.getMinAdvanceBookingHours());
            if (reservationDateTime.isBefore(minDateTime)) {
                throw new BusinessException(ErrorCode.MIN_ADVANCE_TIME_NOT_MET,
                        String.format("최소 %d시간 전에 예약해야 합니다", settings.getMinAdvanceBookingHours()));
            }
        }

        // 5. 기존 ReservationService.createReservation() 재활용
        ReservationCreateRequest createRequest = new ReservationCreateRequest(
                null,                          // customerId (자동 생성)
                request.getCustomerName(),     // customerName
                request.getCustomerPhone(),    // customerPhone
                request.getStaffId(),          // staffId
                request.getServiceIds(),       // serviceIds
                request.getReservationDate(),  // reservationDate
                request.getStartTime(),        // startTime
                request.getCustomerRequest()   // customerMemo
        );

        ReservationResponse reservationResponse = reservationService.createReservation(businessId, createRequest);

        // 6. source를 ONLINE으로 업데이트
        reservationRepository.updateSource(reservationResponse.getId(), "ONLINE");

        log.info("Public reservation created: reservationNumber={}, businessId={}, slug={}",
                reservationResponse.getReservationNumber(), businessId, slug);

        // 7. 응답 생성
        String message;
        if (reservationResponse.getStatus() == ReservationStatus.CONFIRMED) {
            message = "예약이 자동 확정되었습니다. 예약번호: " + reservationResponse.getReservationNumber();
        } else {
            message = "예약이 접수되었습니다. 매장 확인 후 확정됩니다. 예약번호: " + reservationResponse.getReservationNumber();
        }

        return PublicReservationResponse.builder()
                .reservationNumber(reservationResponse.getReservationNumber())
                .status(reservationResponse.getStatus())
                .reservationDate(reservationResponse.getReservationDate())
                .startTime(reservationResponse.getStartTime())
                .endTime(reservationResponse.getEndTime())
                .message(message)
                .build();
    }

    // ========================================
    // 이름+전화번호 기반 예약 조회
    // ========================================

    /**
     * 이름+전화번호로 해당 고객의 모든 예약 목록 조회
     * 모든 매장의 예약을 반환하며, 완료/취소된 예약도 포함합니다.
     */
    public List<PublicReservationLookupResponse> lookupReservations(String name, String phone) {
        log.info("Looking up reservations: name={}, phone={}", name, phone);

        // 1. 이름+전화번호가 일치하는 모든 고객 조회
        List<Customer> customers = customerRepository.findAllByNameAndPhone(name, phone);
        if (customers.isEmpty()) {
            return List.of();
        }

        // 2. 고객 ID 목록 추출
        List<Long> customerIds = customers.stream()
                .map(Customer::getId)
                .toList();

        // 3. 해당 고객들의 모든 예약 조회 (최근순)
        List<Reservation> reservations = reservationRepository.findByCustomerIds(customerIds);
        if (reservations.isEmpty()) {
            return List.of();
        }

        // 4. 필요한 부가 정보 일괄 조회 (매장명, 스태프명)
        Map<Long, String> businessNameCache = new HashMap<>();
        Map<Long, String> staffNameCache = new HashMap<>();

        // 5. 응답 DTO 변환
        return reservations.stream()
                .map(reservation -> {
                    // 매장명 조회 (캐싱)
                    String businessName = businessNameCache.computeIfAbsent(
                            reservation.getBusinessId(),
                            bizId -> businessRepository.findById(bizId)
                                    .map(Business::getName)
                                    .orElse(null)
                    );

                    // 스태프명 조회 (캐싱)
                    String staffName = null;
                    if (reservation.getStaffId() != null) {
                        staffName = staffNameCache.computeIfAbsent(
                                reservation.getStaffId(),
                                sId -> staffRepository.findById(sId)
                                        .map(Staff::getName)
                                        .orElse(null)
                        );
                    }

                    return PublicReservationLookupResponse.builder()
                            .reservationNumber(reservation.getReservationNumber())
                            .status(reservation.getStatus())
                            .businessName(businessName)
                            .reservationDate(reservation.getReservationDate())
                            .startTime(reservation.getStartTime())
                            .endTime(reservation.getEndTime())
                            .staffName(staffName)
                            .services(reservation.getServiceNames())
                            .totalPrice(reservation.getTotalPrice())
                            .createdAt(reservation.getCreatedAt())
                            .build();
                })
                .toList();
    }

    // ========================================
    // 예약 조회
    // ========================================

    /**
     * 예약번호 + 전화번호로 예약 상세 조회
     */
    public PublicReservationDetailResponse getReservation(String reservationNumber, String phone) {
        // 1. 예약 조회
        Reservation reservation = reservationRepository.findByReservationNumber(reservationNumber)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.RESERVATION_NOT_FOUND,
                        "예약을 찾을 수 없습니다: " + reservationNumber));

        // 2. 전화번호로 본인 확인
        verifyPhoneMatch(reservation, phone);

        // 3. 매장 정보 조회
        Business business = businessRepository.findById(reservation.getBusinessId()).orElse(null);

        // 4. 스태프 이름 조회
        String staffName = null;
        if (reservation.getStaffId() != null) {
            staffName = staffRepository.findById(reservation.getStaffId())
                    .map(Staff::getName)
                    .orElse(null);
        }

        // 5. 취소 가능 여부 + 취소 기한 계산
        BusinessSettings settings = businessSettingsRepository.findByBusinessId(reservation.getBusinessId()).orElse(null);
        boolean canCancel = calculateCanCancel(reservation, settings);
        LocalDateTime cancelDeadline = calculateCancelDeadline(reservation, settings);

        return PublicReservationDetailResponse.builder()
                .reservationNumber(reservation.getReservationNumber())
                .status(reservation.getStatus())
                .businessName(business != null ? business.getName() : null)
                .businessAddress(business != null ? business.getAddress() : null)
                .businessPhone(business != null ? business.getPhone() : null)
                .reservationDate(reservation.getReservationDate())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .staffName(staffName)
                .services(reservation.getServiceNames())
                .totalPrice(reservation.getTotalPrice())
                .totalDuration(reservation.getTotalDuration())
                .canCancel(canCancel)
                .cancelDeadline(cancelDeadline)
                .customerMemo(reservation.getCustomerMemo())
                .createdAt(reservation.getCreatedAt())
                .build();
    }

    // ========================================
    // 예약 취소
    // ========================================

    /**
     * 예약번호 + 전화번호로 예약 취소
     */
    @Transactional
    public void cancelReservation(String reservationNumber, PublicReservationCancelRequest request) {
        // 1. 예약 조회
        Reservation reservation = reservationRepository.findByReservationNumber(reservationNumber)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.RESERVATION_NOT_FOUND,
                        "예약을 찾을 수 없습니다: " + reservationNumber));

        // 2. 전화번호로 본인 확인
        verifyPhoneMatch(reservation, request.getPhone());

        // 3. 취소 가능 상태 확인 (PENDING, CONFIRMED만 가능)
        if (!reservation.isPending() && !reservation.isConfirmed()) {
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CANCELLED,
                    "이미 취소되었거나 완료된 예약은 취소할 수 없습니다");
        }

        // 4. 취소 기한 검증
        BusinessSettings settings = businessSettingsRepository.findByBusinessId(reservation.getBusinessId()).orElse(null);
        if (!calculateCanCancel(reservation, settings)) {
            int deadlineHours = (settings != null && settings.getCancelDeadlineHours() != null)
                    ? settings.getCancelDeadlineHours() : 24;
            throw new BusinessException(ErrorCode.CANCEL_DEADLINE_EXCEEDED,
                    String.format("예약 시작 %d시간 전까지만 취소할 수 있습니다", deadlineHours));
        }

        // 5. 기존 ReservationService.cancelReservation() 호출
        reservationService.cancelReservation(
                reservation.getBusinessId(),
                reservation.getId(),
                request.getReason() != null ? request.getReason() : "고객 요청으로 취소"
        );

        log.info("Public reservation cancelled: reservationNumber={}, phone={}",
                reservationNumber, request.getPhone());
    }

    // ========================================
    // Private Helpers
    // ========================================

    /**
     * slug로 매장 조회
     */
    private Business findBusinessBySlug(String slug) {
        return businessRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND,
                        "매장을 찾을 수 없습니다: " + slug));
    }

    /**
     * 전화번호 본인 확인
     * 예약의 고객 전화번호와 요청 전화번호를 비교합니다.
     */
    private void verifyPhoneMatch(Reservation reservation, String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BusinessException(ErrorCode.RESERVATION_PHONE_MISMATCH, "전화번호는 필수입니다");
        }

        // 예약의 고객 전화번호 조회
        Customer customer = customerRepository.findById(reservation.getCustomerId()).orElse(null);
        if (customer == null || !phone.equals(customer.getPhone())) {
            throw new BusinessException(ErrorCode.RESERVATION_PHONE_MISMATCH,
                    "전화번호가 일치하지 않습니다");
        }
    }

    /**
     * 취소 가능 여부 계산
     */
    private boolean calculateCanCancel(Reservation reservation, BusinessSettings settings) {
        // 상태 체크 (PENDING, CONFIRMED만 취소 가능)
        if (!reservation.isPending() && !reservation.isConfirmed()) {
            return false;
        }

        // 취소 기한 체크
        int deadlineHours = (settings != null && settings.getCancelDeadlineHours() != null)
                ? settings.getCancelDeadlineHours() : 24;

        LocalDateTime reservationDateTime = LocalDateTime.of(
                reservation.getReservationDate(), reservation.getStartTime());
        LocalDateTime deadline = reservationDateTime.minusHours(deadlineHours);

        return LocalDateTime.now().isBefore(deadline);
    }

    /**
     * 취소 기한 계산
     */
    private LocalDateTime calculateCancelDeadline(Reservation reservation, BusinessSettings settings) {
        int deadlineHours = (settings != null && settings.getCancelDeadlineHours() != null)
                ? settings.getCancelDeadlineHours() : 24;

        LocalDateTime reservationDateTime = LocalDateTime.of(
                reservation.getReservationDate(), reservation.getStartTime());
        return reservationDateTime.minusHours(deadlineHours);
    }

    /**
     * 가용 시간 블록 계산 (근무 시간 - 휴식 시간 - 기존 예약)
     */
    private List<TimeBlock> calculateAvailableBlocks(
            LocalTime workStart, LocalTime workEnd,
            LocalTime breakStart, LocalTime breakEnd,
            List<Reservation> reservations) {

        // 1단계: 초기 블록 = 근무 시간 전체
        List<TimeBlock> blocks = new ArrayList<>();
        blocks.add(new TimeBlock(workStart, workEnd));

        // 2단계: 휴식 시간 제거
        if (breakStart != null && breakEnd != null) {
            blocks = subtractTimeFromBlocks(blocks, breakStart, breakEnd);
        }

        // 3단계: 기존 예약 시간 제거
        for (Reservation reservation : reservations) {
            blocks = subtractTimeFromBlocks(blocks, reservation.getStartTime(), reservation.getEndTime());
        }

        return blocks;
    }

    /**
     * 블록 목록에서 특정 시간 구간을 제거
     */
    private List<TimeBlock> subtractTimeFromBlocks(List<TimeBlock> blocks,
                                                    LocalTime removeStart, LocalTime removeEnd) {
        List<TimeBlock> result = new ArrayList<>();

        for (TimeBlock block : blocks) {
            if (!removeStart.isBefore(block.end) || !removeEnd.isAfter(block.start)) {
                result.add(block);
                continue;
            }

            if (removeStart.isAfter(block.start)) {
                result.add(new TimeBlock(block.start, removeStart));
            }

            if (removeEnd.isBefore(block.end)) {
                result.add(new TimeBlock(removeEnd, block.end));
            }
        }

        return result;
    }

    /**
     * 내부 시간 블록 클래스
     */
    private record TimeBlock(LocalTime start, LocalTime end) {
    }
}
