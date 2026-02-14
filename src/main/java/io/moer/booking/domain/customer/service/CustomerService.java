package io.moer.booking.domain.customer.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.customer.Customer;
import io.moer.booking.domain.customer.dto.*;
import io.moer.booking.domain.customer.repository.CustomerRepository;
import io.moer.booking.domain.reservation.Reservation;
import io.moer.booking.domain.reservation.repository.ReservationRepository;
import io.moer.booking.domain.staff.Staff;
import io.moer.booking.domain.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 고객 관리 Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final BusinessRepository businessRepository;
    private final ReservationRepository reservationRepository;
    private final StaffRepository staffRepository;

    /**
     * 고객 생성
     */
    @Transactional
    public CustomerResponse createCustomer(Long businessId, CustomerCreateRequest request) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // 전화번호 중복 체크
        if (customerRepository.existsByBusinessIdAndPhone(businessId, request.getPhone())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE,
                    "이미 등록된 전화번호입니다: " + request.getPhone());
        }

        // List<String> → String 변환
        String tagsString = Customer.tagsToString(request.getTags());

        Customer customer = Customer.builder()
                .businessId(businessId)
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .birthDate(request.getBirthDate())
                .gender(request.getGender())
                .visitCount(0)
                .totalSpent(0)
                .tags(tagsString)
                .memo(request.getMemo())
                .build();

        customerRepository.save(customer);

        log.info("Customer created: id={}, businessId={}, name={}, phone={}",
                customer.getId(), businessId, customer.getName(), customer.getPhone());

        return CustomerResponse.from(customer);
    }

    /**
     * 고객 단건 조회
     */
    public CustomerResponse getCustomer(Long businessId, Long customerId) {
        if (!customerRepository.existsByBusinessIdAndId(businessId, customerId)) {
            throw new EntityNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND));

        return CustomerResponse.from(customer);
    }

    /**
     * 전화번호로 고객 조회
     */
    public CustomerResponse getCustomerByPhone(Long businessId, String phone) {
        Customer customer = customerRepository.findByBusinessIdAndPhone(businessId, phone)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND,
                        "해당 전화번호의 고객을 찾을 수 없습니다: " + phone));

        return CustomerResponse.from(customer);
    }

    /**
     * Business의 전체 고객 조회
     */
    public List<CustomerResponse> getCustomersByBusiness(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return customerRepository.findByBusinessId(businessId).stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 고객 검색 (조건별)
     */
    public List<CustomerResponse> searchCustomers(CustomerSearchCondition condition) {
        if (!businessRepository.existsById(condition.getBusinessId())) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return customerRepository.findByCondition(condition).stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * VIP 고객 목록 조회
     */
    public List<CustomerResponse> getVipCustomers(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return customerRepository.findVipCustomers(businessId).stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 신규 고객 목록 조회
     */
    public List<CustomerResponse> getNewCustomers(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return customerRepository.findNewCustomers(businessId).stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 단골 고객 목록 조회
     */
    public List<CustomerResponse> getRegularCustomers(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return customerRepository.findRegularCustomers(businessId).stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 고객 수정
     */
    @Transactional
    public CustomerResponse updateCustomer(Long businessId, Long customerId, CustomerUpdateRequest request) {
        if (!customerRepository.existsByBusinessIdAndId(businessId, customerId)) {
            throw new EntityNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND));

        // 전화번호 변경 시 중복 체크
        if (request.getPhone() != null && !request.getPhone().equals(customer.getPhone())) {
            if (customerRepository.existsByBusinessIdAndPhone(businessId, request.getPhone())) {
                throw new BusinessException(ErrorCode.DUPLICATE_PHONE,
                        "이미 등록된 전화번호입니다: " + request.getPhone());
            }
        }

        // List<String> → String 변환
        String tagsString = request.getTags() != null
                ? Customer.tagsToString(request.getTags())
                : customer.getTags();

        Customer updatedCustomer = Customer.builder()
                .id(customer.getId())
                .businessId(customer.getBusinessId())
                .name(request.getName() != null ? request.getName() : customer.getName())
                .phone(request.getPhone() != null ? request.getPhone() : customer.getPhone())
                .email(request.getEmail() != null ? request.getEmail() : customer.getEmail())
                .birthDate(request.getBirthDate() != null ? request.getBirthDate() : customer.getBirthDate())
                .gender(request.getGender() != null ? request.getGender() : customer.getGender())
                .visitCount(customer.getVisitCount())
                .totalSpent(customer.getTotalSpent())
                .lastVisitDate(customer.getLastVisitDate())
                .tags(tagsString)
                .memo(request.getMemo() != null ? request.getMemo() : customer.getMemo())
                .createdAt(customer.getCreatedAt())
                .build();

        customerRepository.update(updatedCustomer);

        log.info("Customer updated: id={}, businessId={}", customerId, businessId);

        return getCustomer(businessId, customerId);
    }

    /**
     * 고객 삭제
     */
    @Transactional
    public void deleteCustomer(Long businessId, Long customerId) {
        if (!customerRepository.existsByBusinessIdAndId(businessId, customerId)) {
            throw new EntityNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        customerRepository.delete(customerId);

        log.info("Customer deleted: id={}, businessId={}", customerId, businessId);
    }

    /**
     * 고객 조회 또는 자동 생성
     * 전화번호로 먼저 조회하고, 없으면 새로 생성
     */
    @Transactional
    public Customer findOrCreateCustomer(Long businessId, String name, String phone) {
        // 1. 전화번호로 기존 고객 조회
        Optional<Customer> existingCustomer = customerRepository.findByBusinessIdAndPhone(businessId, phone);

        if (existingCustomer.isPresent()) {
            log.info("Found existing customer: id={}, name={}, phone={}",
                    existingCustomer.get().getId(), existingCustomer.get().getName(), phone);
            return existingCustomer.get();
        }

        // 2. 없으면 새로 생성
        log.info("Creating new customer: name={}, phone={}", name, phone);

        Customer newCustomer = Customer.builder()
                .businessId(businessId)
                .name(name)
                .phone(phone)
                .visitCount(0)
                .totalSpent(0)
                .build();

        customerRepository.save(newCustomer);

        log.info("Customer created: id={}, businessId={}, name={}, phone={}",
                newCustomer.getId(), businessId, name, phone);

        return newCustomer;
    }

    /**
     * 예약 완료 시 고객 통계 업데이트
     * - visitCount +1
     * - totalSpent +금액
     * - lastVisitDate 업데이트
     * - visitCount 기반 tags 자동 업데이트 (VIP, 단골, 신규)
     */
    @Transactional
    public void updateVisitStats(Long customerId, int amount, java.time.LocalDate visitDate) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND));

        // 통계 업데이트
        int newVisitCount = (customer.getVisitCount() != null ? customer.getVisitCount() : 0) + 1;
        int newTotalSpent = (customer.getTotalSpent() != null ? customer.getTotalSpent() : 0) + amount;

        // tags 자동 업데이트
        String newTags = generateAutoTags(newVisitCount);

        // DB 업데이트
        customerRepository.updateVisitStats(customerId, newVisitCount, newTotalSpent, visitDate);

        // tags 업데이트 (별도 쿼리)
        Customer updatedCustomer = Customer.builder()
                .id(customer.getId())
                .businessId(customer.getBusinessId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .birthDate(customer.getBirthDate())
                .gender(customer.getGender())
                .visitCount(newVisitCount)
                .totalSpent(newTotalSpent)
                .lastVisitDate(visitDate)
                .tags(newTags)
                .memo(customer.getMemo())
                .createdAt(customer.getCreatedAt())
                .build();

        customerRepository.update(updatedCustomer);

        log.info("Customer visit stats updated: id={}, visitCount={}, totalSpent={}, lastVisitDate={}, tags={}",
                customerId, newVisitCount, newTotalSpent, visitDate, newTags);
    }

    /**
     * 예약 취소 시 고객 통계 롤백
     * - visitCount -1
     * - totalSpent -금액
     */
    @Transactional
    public void rollbackVisitStats(Long customerId, int amount) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND));

        // 통계 롤백
        int newVisitCount = Math.max(0, (customer.getVisitCount() != null ? customer.getVisitCount() : 0) - 1);
        int newTotalSpent = Math.max(0, (customer.getTotalSpent() != null ? customer.getTotalSpent() : 0) - amount);

        // tags 자동 업데이트
        String newTags = generateAutoTags(newVisitCount);

        // lastVisitDate는 유지 (다른 완료된 예약이 있을 수 있으므로)
        customerRepository.updateVisitStats(customerId, newVisitCount, newTotalSpent, customer.getLastVisitDate());

        // tags 업데이트
        Customer updatedCustomer = Customer.builder()
                .id(customer.getId())
                .businessId(customer.getBusinessId())
                .name(customer.getName())
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .birthDate(customer.getBirthDate())
                .gender(customer.getGender())
                .visitCount(newVisitCount)
                .totalSpent(newTotalSpent)
                .lastVisitDate(customer.getLastVisitDate())
                .tags(newTags)
                .memo(customer.getMemo())
                .createdAt(customer.getCreatedAt())
                .build();

        customerRepository.update(updatedCustomer);

        log.info("Customer visit stats rolled back: id={}, visitCount={}, totalSpent={}, tags={}",
                customerId, newVisitCount, newTotalSpent, newTags);
    }

    /**
     * 고객 예약 이력 조회
     * 특정 고객의 과거 예약 목록 + 요약 통계
     */
    public CustomerReservationHistoryResponse getCustomerReservationHistory(
            Long businessId, Long customerId, String status, int page, int size) {

        // 1. Customer 존재 확인
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND,
                        "고객을 찾을 수 없습니다: " + customerId));

        if (!customer.getBusinessId().equals(businessId)) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND,
                    "해당 매장의 고객이 아닙니다: " + customerId);
        }

        int offset = (page - 1) * size;

        // 2. 예약 목록 조회
        List<Reservation> reservations = reservationRepository.findByBusinessIdAndCustomerId(
                businessId, customerId, status, offset, size);
        int totalCount = reservationRepository.countByBusinessIdAndCustomerId(
                businessId, customerId, status);

        // 3. 예약 목록을 DTO 변환 (services JSONB에서 서비스명 추출)
        List<CustomerReservationItem> items = reservations.stream()
                .map(r -> CustomerReservationItem.builder()
                        .id(r.getId())
                        .reservationDate(r.getReservationDate())
                        .startTime(r.getStartTime())
                        .endTime(r.getEndTime())
                        .staffName(getStaffName(r.getStaffId()))
                        .services(extractServiceNames(r.getServices()))
                        .totalPrice(r.getTotalPrice())
                        .status(r.getStatus().name())
                        .build())
                .collect(Collectors.toList());

        // 4. 요약 통계
        String favoriteService = reservationRepository.findFavoriteServiceByCustomerId(businessId, customerId);
        Long favoriteStaffId = reservationRepository.findFavoriteStaffIdByCustomerId(businessId, customerId);
        String favoriteStaffName = null;
        if (favoriteStaffId != null) {
            favoriteStaffName = staffRepository.findById(favoriteStaffId)
                    .map(Staff::getName).orElse(null);
        }

        CustomerReservationSummary summary = CustomerReservationSummary.builder()
                .totalVisits(customer.getVisitCount())
                .totalSpent(customer.getTotalSpent())
                .lastVisitDate(customer.getLastVisitDate())
                .favoriteService(favoriteService)
                .favoriteStaff(favoriteStaffName)
                .build();

        return CustomerReservationHistoryResponse.builder()
                .items(items)
                .totalCount(totalCount)
                .summary(summary)
                .build();
    }

    /**
     * staffId로 직원 이름 조회
     */
    private String getStaffName(Long staffId) {
        if (staffId == null) {
            return null;
        }
        return staffRepository.findById(staffId)
                .map(Staff::getName)
                .orElse(null);
    }

    /**
     * JSONB services에서 서비스명 추출
     */
    private List<String> extractServiceNames(List<Map<String, Object>> services) {
        if (services == null) {
            return List.of();
        }
        return services.stream()
                .map(s -> (String) s.get("name"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * visitCount 기반 자동 태그 생성
     * - VIP: 10회 이상
     * - 단골: 3회 이상
     * - 신규: 1회
     */
    private String generateAutoTags(int visitCount) {
        List<String> tags = new java.util.ArrayList<>();

        if (visitCount >= 10) {
            tags.add("VIP");
        }
        if (visitCount >= 3) {
            tags.add("단골");
        }
        if (visitCount == 1) {
            tags.add("신규");
        }

        return tags.isEmpty() ? null : String.join(",", tags);
    }
}