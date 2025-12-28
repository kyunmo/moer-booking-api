package io.moer.booking.domain.holiday.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.holiday.SpecialHoliday;
import io.moer.booking.domain.holiday.dto.SpecialHolidayCreateRequest;
import io.moer.booking.domain.holiday.dto.SpecialHolidayResponse;
import io.moer.booking.domain.holiday.repository.SpecialHolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpecialHolidayService {

    private final SpecialHolidayRepository holidayRepository;
    private final BusinessRepository businessRepository;

    /**
     * 특별 휴무일 생성
     */
    @Transactional
    public SpecialHolidayResponse createHoliday(Long businessId, SpecialHolidayCreateRequest request) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // 중복 확인 (같은 날짜에 이미 휴무일 등록되어 있는지)
        if (holidayRepository.existsByBusinessIdAndDate(businessId, request.getHolidayDate())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "이미 등록된 휴무일입니다: " + request.getHolidayDate());
        }

        // SpecialHoliday 엔티티 생성
        SpecialHoliday holiday = SpecialHoliday.builder()
                .businessId(businessId)
                .holidayDate(request.getHolidayDate())
                .title(request.getTitle())
                .isClosed(request.getIsClosed() != null ? request.getIsClosed() : true)
                .build();

        // 저장
        holidayRepository.save(holiday);

        log.info("SpecialHoliday created: id={}, businessId={}, date={}",
                holiday.getId(), businessId, holiday.getHolidayDate());

        return SpecialHolidayResponse.from(holiday);
    }

    /**
     * Business의 전체 휴무일 조회
     */
    public List<SpecialHolidayResponse> getHolidaysByBusiness(Long businessId) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return holidayRepository.findByBusinessId(businessId).stream()
                .map(SpecialHolidayResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Business의 특정 기간 휴무일 조회
     */
    public List<SpecialHolidayResponse> getHolidaysByDateRange(
            Long businessId, LocalDate startDate, LocalDate endDate) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return holidayRepository.findByBusinessIdAndDateRange(businessId, startDate, endDate).stream()
                .map(SpecialHolidayResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 날짜가 휴무일인지 확인
     */
    public boolean isHoliday(Long businessId, LocalDate date) {
        return holidayRepository.findByBusinessIdAndDate(businessId, date)
                .map(holiday -> Boolean.TRUE.equals(holiday.getIsClosed()))
                .orElse(false);
    }

    /**
     * 특별 휴무일 삭제 (ID)
     */
    @Transactional
    public void deleteHoliday(Long businessId, Long holidayId) {
        // Business의 Holiday인지 확인
        if (!holidayRepository.existsByBusinessIdAndId(businessId, holidayId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        holidayRepository.delete(holidayId);

        log.info("SpecialHoliday deleted: id={}, businessId={}", holidayId, businessId);
    }

    /**
     * 특별 휴무일 삭제 (날짜)
     */
    @Transactional
    public void deleteHolidayByDate(Long businessId, LocalDate holidayDate) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // 해당 날짜에 휴무일이 있는지 확인
        if (!holidayRepository.existsByBusinessIdAndDate(businessId, holidayDate)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND,
                    "해당 날짜에 등록된 휴무일이 없습니다: " + holidayDate);
        }

        holidayRepository.deleteByBusinessIdAndDate(businessId, holidayDate);

        log.info("SpecialHoliday deleted by date: businessId={}, date={}", businessId, holidayDate);
    }
}