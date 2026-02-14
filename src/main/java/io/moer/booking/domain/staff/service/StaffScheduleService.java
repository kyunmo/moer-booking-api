package io.moer.booking.domain.staff.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.reservation.Reservation;
import io.moer.booking.domain.reservation.ReservationStatus;
import io.moer.booking.domain.reservation.repository.ReservationRepository;
import io.moer.booking.domain.staff.Staff;
import io.moer.booking.domain.staff.StaffSchedule;
import io.moer.booking.domain.staff.dto.*;
import io.moer.booking.domain.staff.repository.StaffRepository;
import io.moer.booking.domain.staff.repository.StaffScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 직원 근무 스케줄 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StaffScheduleService {

    private final StaffScheduleRepository staffScheduleRepository;
    private final StaffRepository staffRepository;
    private final ReservationRepository reservationRepository;

    /**
     * 직원의 주간 근무 스케줄 조회 (7일분)
     */
    public List<StaffScheduleResponse> getSchedules(Long businessId, Long staffId) {
        // Staff 존재 & 소속 확인
        validateStaffBelongsToBusiness(businessId, staffId);

        List<StaffSchedule> schedules = staffScheduleRepository.findByStaffId(staffId);

        return schedules.stream()
                .map(StaffScheduleResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 직원의 주간 근무 스케줄 일괄 저장 (delete-insert 방식)
     */
    @Transactional
    public List<StaffScheduleResponse> saveSchedules(Long businessId, Long staffId,
                                                      StaffScheduleSaveRequest request) {
        // Staff 존재 & 소속 확인
        validateStaffBelongsToBusiness(businessId, staffId);

        // 각 요일별 스케줄 검증
        for (StaffScheduleSaveRequest.DaySchedule daySchedule : request.getSchedules()) {
            validateDaySchedule(daySchedule);
        }

        // 기존 스케줄 전체 삭제
        staffScheduleRepository.deleteByStaffId(staffId);

        // 새 스케줄 일괄 저장
        List<StaffSchedule> savedSchedules = new ArrayList<>();
        for (StaffScheduleSaveRequest.DaySchedule daySchedule : request.getSchedules()) {
            StaffSchedule schedule = StaffSchedule.builder()
                    .staffId(staffId)
                    .businessId(businessId)
                    .dayOfWeek(daySchedule.getDayOfWeek())
                    .startTime(daySchedule.getIsWorking() ? daySchedule.getStartTime() : null)
                    .endTime(daySchedule.getIsWorking() ? daySchedule.getEndTime() : null)
                    .breakStartTime(daySchedule.getIsWorking() ? daySchedule.getBreakStartTime() : null)
                    .breakEndTime(daySchedule.getIsWorking() ? daySchedule.getBreakEndTime() : null)
                    .isWorking(daySchedule.getIsWorking() ? "Y" : "N")
                    .build();

            staffScheduleRepository.save(schedule);
            savedSchedules.add(schedule);
        }

        log.info("Staff schedules saved: staffId={}, businessId={}, count={}",
                staffId, businessId, savedSchedules.size());

        return savedSchedules.stream()
                .map(StaffScheduleResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 날짜의 직원 가용 시간 조회
     */
    public StaffAvailableTimesResponse getAvailableTimes(Long businessId, Long staffId, LocalDate date) {
        // 1. Staff 존재 & 소속 확인
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND));

        if (!staff.getBusinessId().equals(businessId)) {
            throw new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND);
        }

        // 2. 해당 날짜의 요일 (ISO-8601: 1=월~7=일)
        int dayOfWeek = date.getDayOfWeek().getValue();
        String dayName = getDayNameFromNumber(dayOfWeek);

        // 3. 해당 요일 스케줄 조회
        StaffSchedule schedule = staffScheduleRepository
                .findByStaffIdAndDayOfWeek(staffId, dayOfWeek)
                .orElse(null);

        // 4. 스케줄 없거나 비근무일이면 빈 가용슬롯으로 응답 (에러 아님)
        if (schedule == null || !schedule.isWorkingDay()) {
            return StaffAvailableTimesResponse.builder()
                    .staffId(staffId)
                    .staffName(staff.getName())
                    .date(date)
                    .dayName(dayName)
                    .isWorkingDay(false)
                    .bookedSlots(List.of())
                    .availableSlots(List.of())
                    .build();
        }

        // 5. 해당 날짜의 기존 예약 조회 (PENDING, CONFIRMED 상태만)
        List<Reservation> dayReservations = reservationRepository
                .findByBusinessIdAndDate(businessId, date);

        List<Reservation> staffReservations = dayReservations.stream()
                .filter(r -> staffId.equals(r.getStaffId()))
                .filter(r -> r.getStatus() == ReservationStatus.PENDING
                        || r.getStatus() == ReservationStatus.CONFIRMED)
                .sorted(Comparator.comparing(Reservation::getStartTime))
                .toList();

        // 6. BookedSlot 목록 생성
        List<StaffAvailableTimesResponse.BookedSlot> bookedSlots = staffReservations.stream()
                .map(r -> StaffAvailableTimesResponse.BookedSlot.builder()
                        .start(r.getStartTime())
                        .end(r.getEndTime())
                        .reservationId(r.getId())
                        .build())
                .toList();

        // 7. 가용 슬롯 계산: 근무 시간 - 휴식 시간 - 예약 시간
        List<AvailableTimeSlot> availableSlots = calculateAvailableSlots(
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getBreakStartTime(),
                schedule.getBreakEndTime(),
                staffReservations
        );

        return StaffAvailableTimesResponse.builder()
                .staffId(staffId)
                .staffName(staff.getName())
                .date(date)
                .dayName(dayName)
                .isWorkingDay(true)
                .workStart(schedule.getStartTime())
                .workEnd(schedule.getEndTime())
                .breakStart(schedule.getBreakStartTime())
                .breakEnd(schedule.getBreakEndTime())
                .bookedSlots(bookedSlots)
                .availableSlots(availableSlots)
                .build();
    }

    // ========================================
    // Private Methods
    // ========================================

    /**
     * 직원이 해당 매장에 소속되어 있는지 검증
     */
    private void validateStaffBelongsToBusiness(Long businessId, Long staffId) {
        if (!staffRepository.existsByBusinessIdAndId(businessId, staffId)) {
            throw new EntityNotFoundException(ErrorCode.STAFF_NOT_FOUND);
        }
    }

    /**
     * 요일별 스케줄 유효성 검증
     */
    private void validateDaySchedule(StaffScheduleSaveRequest.DaySchedule daySchedule) {
        if (!daySchedule.getIsWorking()) {
            // 비근무일은 시간 검증 불필요
            return;
        }

        // 근무일인데 시작/종료 시간이 없으면 에러
        if (daySchedule.getStartTime() == null || daySchedule.getEndTime() == null) {
            throw new BusinessException(ErrorCode.INVALID_SCHEDULE_TIME,
                    "근무일에는 시작 시간과 종료 시간이 필수입니다 (요일: " + daySchedule.getDayOfWeek() + ")");
        }

        // 시작 시간이 종료 시간 이후이면 에러
        if (!daySchedule.getStartTime().isBefore(daySchedule.getEndTime())) {
            throw new BusinessException(ErrorCode.INVALID_SCHEDULE_TIME,
                    "시작 시간은 종료 시간보다 이전이어야 합니다 (요일: " + daySchedule.getDayOfWeek() + ")");
        }

        // 휴식 시간 검증 (둘 다 있거나 둘 다 없어야 함)
        boolean hasBreakStart = daySchedule.getBreakStartTime() != null;
        boolean hasBreakEnd = daySchedule.getBreakEndTime() != null;

        if (hasBreakStart != hasBreakEnd) {
            throw new BusinessException(ErrorCode.INVALID_SCHEDULE_TIME,
                    "휴식 시작 시간과 종료 시간을 모두 입력해야 합니다 (요일: " + daySchedule.getDayOfWeek() + ")");
        }

        if (hasBreakStart && hasBreakEnd) {
            // 휴식 시작이 휴식 종료 이후이면 에러
            if (!daySchedule.getBreakStartTime().isBefore(daySchedule.getBreakEndTime())) {
                throw new BusinessException(ErrorCode.INVALID_SCHEDULE_TIME,
                        "휴식 시작 시간은 종료 시간보다 이전이어야 합니다 (요일: " + daySchedule.getDayOfWeek() + ")");
            }

            // 휴식 시간이 근무 시간 범위 밖이면 에러
            if (daySchedule.getBreakStartTime().isBefore(daySchedule.getStartTime())
                    || daySchedule.getBreakEndTime().isAfter(daySchedule.getEndTime())) {
                throw new BusinessException(ErrorCode.INVALID_SCHEDULE_TIME,
                        "휴식 시간은 근무 시간 범위 내에 있어야 합니다 (요일: " + daySchedule.getDayOfWeek() + ")");
            }
        }
    }

    /**
     * 가용 시간 슬롯 계산
     * 근무 시간 블록 -> 휴식 시간 제거 -> 기존 예약 시간 제거
     */
    private List<AvailableTimeSlot> calculateAvailableSlots(
            LocalTime workStart, LocalTime workEnd,
            LocalTime breakStart, LocalTime breakEnd,
            List<Reservation> reservations) {

        // 1단계: 초기 가용 블록 = 근무 시간 전체
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

        // 4단계: TimeBlock -> AvailableTimeSlot 변환
        return blocks.stream()
                .map(block -> AvailableTimeSlot.builder()
                        .startTime(block.start)
                        .endTime(block.end)
                        .build())
                .toList();
    }

    /**
     * 블록 목록에서 특정 시간 구간을 제거
     */
    private List<TimeBlock> subtractTimeFromBlocks(List<TimeBlock> blocks,
                                                    LocalTime removeStart, LocalTime removeEnd) {
        List<TimeBlock> result = new ArrayList<>();

        for (TimeBlock block : blocks) {
            // 제거 구간과 겹치지 않는 경우 -> 블록 유지
            if (!removeStart.isBefore(block.end) || !removeEnd.isAfter(block.start)) {
                result.add(block);
                continue;
            }

            // 제거 구간 앞부분이 남는 경우
            if (removeStart.isAfter(block.start)) {
                result.add(new TimeBlock(block.start, removeStart));
            }

            // 제거 구간 뒷부분이 남는 경우
            if (removeEnd.isBefore(block.end)) {
                result.add(new TimeBlock(removeEnd, block.end));
            }
        }

        return result;
    }

    /**
     * ISO-8601 요일 번호 -> 한글 요일명
     */
    private String getDayNameFromNumber(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "월";
            case 2 -> "화";
            case 3 -> "수";
            case 4 -> "목";
            case 5 -> "금";
            case 6 -> "토";
            case 7 -> "일";
            default -> null;
        };
    }

    /**
     * 내부 시간 블록 클래스
     */
    private record TimeBlock(LocalTime start, LocalTime end) {
    }
}
