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

    @Transactional
    public SpecialHolidayResponse createHoliday(Long businessId, SpecialHolidayCreateRequest request) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        if (holidayRepository.existsByBusinessIdAndDate(businessId, request.getDate())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "이미 등록된 휴무일입니다: " + request.getDate());
        }

        SpecialHoliday holiday = SpecialHoliday.builder()
                .businessId(businessId)
                .name(request.getName())
                .date(request.getDate())
                .type(request.getType())
                .reason(request.getReason())
                .build();

        holidayRepository.save(holiday);

        log.info("SpecialHoliday created: id={}, businessId={}, name={}, date={}",
                holiday.getId(), businessId, holiday.getName(), holiday.getDate());

        return SpecialHolidayResponse.from(holiday);
    }

    public List<SpecialHolidayResponse> getHolidaysByBusiness(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return holidayRepository.findByBusinessId(businessId).stream()
                .map(SpecialHolidayResponse::from)
                .collect(Collectors.toList());
    }

    public List<SpecialHolidayResponse> getHolidaysByDateRange(
            Long businessId, LocalDate startDate, LocalDate endDate) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return holidayRepository.findByBusinessIdAndDateRange(businessId, startDate, endDate).stream()
                .map(SpecialHolidayResponse::from)
                .collect(Collectors.toList());
    }

    public boolean isHoliday(Long businessId, LocalDate date) {
        return holidayRepository.findByBusinessIdAndDate(businessId, date).isPresent();
    }

    @Transactional
    public void deleteHoliday(Long businessId, Long holidayId) {
        if (!holidayRepository.existsByBusinessIdAndId(businessId, holidayId)) {
            throw new EntityNotFoundException(ErrorCode.HOLIDAY_NOT_FOUND);
        }

        holidayRepository.delete(holidayId);

        log.info("SpecialHoliday deleted: id={}, businessId={}", holidayId, businessId);
    }

    @Transactional
    public void deleteHolidayByDate(Long businessId, LocalDate holidayDate) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        if (!holidayRepository.existsByBusinessIdAndDate(businessId, holidayDate)) {
            throw new EntityNotFoundException(ErrorCode.HOLIDAY_NOT_FOUND,
                    "해당 날짜에 등록된 휴무일이 없습니다: " + holidayDate);
        }

        holidayRepository.deleteByBusinessIdAndDate(businessId, holidayDate);

        log.info("SpecialHoliday deleted by date: businessId={}, date={}", businessId, holidayDate);
    }
}