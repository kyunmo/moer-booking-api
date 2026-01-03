package io.moer.booking.domain.customer.service;

import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.customer.Customer;
import io.moer.booking.domain.customer.CustomerHistory;
import io.moer.booking.domain.customer.dto.CustomerHistoryCreateRequest;
import io.moer.booking.domain.customer.dto.CustomerHistoryResponse;
import io.moer.booking.domain.customer.repository.CustomerHistoryRepository;
import io.moer.booking.domain.customer.repository.CustomerRepository;
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
public class CustomerHistoryService {

    private final CustomerHistoryRepository historyRepository;
    private final CustomerRepository customerRepository;
    private final BusinessRepository businessRepository;
    private final CustomerService customerService;

    /**
     * 이력 생성 (수동)
     */
    @Transactional
    public CustomerHistoryResponse createHistory(Long businessId, CustomerHistoryCreateRequest request) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // Customer 존재 확인
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND, "고객을 찾을 수 없습니다"));

        // Business가 일치하는지 확인
        if (!customer.getBusinessId().equals(businessId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND, "다른 매장의 고객입니다");
        }

        // CustomerHistory 엔티티 생성
        CustomerHistory history = CustomerHistory.builder()
                .customerId(request.getCustomerId())
                .businessId(businessId)
                .reservationId(request.getReservationId())
                .staffId(request.getStaffId())
                .visitDate(request.getVisitDate())
                .services(request.getServices())
                .totalPrice(request.getTotalPrice())
                .details(request.getDetails())
                .beforeImageUrl(request.getBeforeImageUrl())
                .afterImageUrl(request.getAfterImageUrl())
                //.adminMemo(request.getAdminMemo())
                .build();

        // 저장
        historyRepository.save(history);

        // Customer 방문 통계 업데이트
        /*customerService.updateVisitStats(
                request.getCustomerId(),
                request.getTotalPrice(),
                request.getVisitDate()
        );*/

        log.info("CustomerHistory created: id={}, customerId={}, businessId={}, visitDate={}",
                history.getId(), request.getCustomerId(), businessId, request.getVisitDate());

        return CustomerHistoryResponse.from(history);
    }

    /**
     * 이력 생성 (예약 완료 시 자동 호출)
     */
    @Transactional
    public CustomerHistoryResponse createHistoryFromReservation(
            Long businessId,
            Long customerId,
            Long reservationId,
            Long staffId,
            LocalDate visitDate,
            List<Long> serviceIds,
            List<String> serviceNames,
            Integer totalPrice) {

        // services JSON 생성
        List<java.util.Map<String, Object>> services = new java.util.ArrayList<>();
        for (int i = 0; i < serviceIds.size(); i++) {
            java.util.Map<String, Object> service = new java.util.HashMap<>();
            service.put("id", serviceIds.get(i));
            service.put("name", serviceNames.get(i));
            // 개별 가격은 나중에 추가 가능 (현재는 총액만)
            services.add(service);
        }

        CustomerHistoryCreateRequest request = CustomerHistoryCreateRequest.builder()
                .customerId(customerId)
                .reservationId(reservationId)
                .staffId(staffId)
                .visitDate(visitDate)
                .services(services)
                .totalPrice(totalPrice)
                .build();

        return createHistory(businessId, request);
    }

    /**
     * 이력 단건 조회
     */
    public CustomerHistoryResponse getHistory(Long businessId, Long historyId) {
        if (!historyRepository.existsByBusinessIdAndId(businessId, historyId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        CustomerHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        return CustomerHistoryResponse.from(history);
    }

    /**
     * Customer의 전체 이력 조회
     */
    public List<CustomerHistoryResponse> getHistoriesByCustomer(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND, "고객을 찾을 수 없습니다");
        }

        return historyRepository.findByCustomerId(customerId).stream()
                .map(CustomerHistoryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Business의 전체 이력 조회
     */
    public List<CustomerHistoryResponse> getHistoriesByBusiness(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return historyRepository.findByBusinessId(businessId).stream()
                .map(CustomerHistoryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 기간별 이력 조회
     */
    public List<CustomerHistoryResponse> getHistoriesByDateRange(
            Long customerId, LocalDate startDate, LocalDate endDate) {
        if (!customerRepository.existsById(customerId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND, "고객을 찾을 수 없습니다");
        }

        return historyRepository.findByCustomerIdAndDateRange(customerId, startDate, endDate).stream()
                .map(CustomerHistoryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 최근 방문 이력 조회
     */
    public CustomerHistoryResponse getLatestHistory(Long customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND, "고객을 찾을 수 없습니다");
        }

        CustomerHistory history = historyRepository.findLatestByCustomerId(customerId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND,
                        "방문 이력이 없습니다"));

        return CustomerHistoryResponse.from(history);
    }

    /**
     * 이력 수정
     */
    @Transactional
    public CustomerHistoryResponse updateHistory(Long businessId, Long historyId,
                                                 CustomerHistoryCreateRequest request) {
        if (!historyRepository.existsByBusinessIdAndId(businessId, historyId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        CustomerHistory history = historyRepository.findById(historyId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        // 수정
        CustomerHistory updatedHistory = CustomerHistory.builder()
                .id(history.getId())
                .staffId(request.getStaffId() != null ? request.getStaffId() : history.getStaffId())
                .visitDate(request.getVisitDate() != null ? request.getVisitDate() : history.getVisitDate())
                .services(request.getServices() != null ? request.getServices() : history.getServices())
                .totalPrice(request.getTotalPrice() != null ? request.getTotalPrice() : history.getTotalPrice())
                .details(request.getDetails() != null ? request.getDetails() : history.getDetails())
                .beforeImageUrl(request.getBeforeImageUrl() != null ?
                        request.getBeforeImageUrl() : history.getBeforeImageUrl())
                .afterImageUrl(request.getAfterImageUrl() != null ?
                        request.getAfterImageUrl() : history.getAfterImageUrl())
                //.adminMemo(request.getAdminMemo() != null ? request.getAdminMemo() : history.getAdminMemo())
                .build();

        historyRepository.update(updatedHistory);

        log.info("CustomerHistory updated: id={}, businessId={}", historyId, businessId);

        return getHistory(businessId, historyId);
    }

    /**
     * 이력 삭제
     */
    @Transactional
    public void deleteHistory(Long businessId, Long historyId) {
        if (!historyRepository.existsByBusinessIdAndId(businessId, historyId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        historyRepository.delete(historyId);

        log.info("CustomerHistory deleted: id={}, businessId={}", historyId, businessId);
    }
}