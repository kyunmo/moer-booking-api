package io.moer.booking.domain.customer.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.customer.Customer;
import io.moer.booking.domain.customer.dto.CustomerCreateRequest;
import io.moer.booking.domain.customer.dto.CustomerResponse;
import io.moer.booking.domain.customer.dto.CustomerSearchCondition;
import io.moer.booking.domain.customer.dto.CustomerUpdateRequest;
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
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final BusinessRepository businessRepository;

    /**
     * Customer 생성
     */
    @Transactional
    public CustomerResponse createCustomer(Long businessId, CustomerCreateRequest request) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        // 전화번호 중복 확인
        if (customerRepository.existsByBusinessIdAndPhone(businessId, request.getPhone())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    "이미 등록된 전화번호입니다: " + request.getPhone());
        }

        // Customer 엔티티 생성
        Customer customer = Customer.builder()
                .businessId(businessId)
                .name(request.getName())
                .phone(request.getPhone())
                .email(request.getEmail())
                .visitCount(0)
                .totalSpent(0)
                .tags(request.getTags())
                .adminMemo(request.getAdminMemo())
                .kakaoUserKey(request.getKakaoUserKey())
                .build();

        // 저장
        customerRepository.save(customer);

        log.info("Customer created: id={}, businessId={}, name={}, phone={}",
                customer.getId(), businessId, customer.getName(), customer.getPhone());

        return CustomerResponse.from(customer);
    }

    /**
     * Customer 단건 조회
     */
    public CustomerResponse getCustomer(Long businessId, Long customerId) {
        // Business의 Customer인지 확인
        if (!customerRepository.existsByBusinessIdAndId(businessId, customerId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        return CustomerResponse.from(customer);
    }

    /**
     * 전화번호로 Customer 조회
     */
    public CustomerResponse getCustomerByPhone(Long businessId, String phone) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        Customer customer = customerRepository.findByBusinessIdAndPhone(businessId, phone)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND,
                        "해당 전화번호의 고객을 찾을 수 없습니다: " + phone));

        return CustomerResponse.from(customer);
    }

    /**
     * Business의 전체 Customer 목록 조회
     */
    public List<CustomerResponse> getCustomersByBusiness(Long businessId) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return customerRepository.findByBusinessId(businessId).stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 조건별 Customer 검색
     */
    public List<CustomerResponse> searchCustomers(CustomerSearchCondition condition) {
        // Business 존재 확인
        if (!businessRepository.existsById(condition.getBusinessId())) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return customerRepository.findByCondition(condition).stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * VIP 고객 목록 조회 (10회 이상)
     */
    public List<CustomerResponse> getVipCustomers(Long businessId) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return customerRepository.findVipCustomers(businessId).stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 신규 고객 목록 조회 (1회)
     */
    public List<CustomerResponse> getNewCustomers(Long businessId) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return customerRepository.findNewCustomers(businessId).stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 단골 고객 목록 조회 (3회 이상)
     */
    public List<CustomerResponse> getRegularCustomers(Long businessId) {
        // Business 존재 확인
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return customerRepository.findRegularCustomers(businessId).stream()
                .map(CustomerResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Customer 수정
     */
    @Transactional
    public CustomerResponse updateCustomer(Long businessId, Long customerId, CustomerUpdateRequest request) {
        // Business의 Customer인지 확인
        if (!customerRepository.existsByBusinessIdAndId(businessId, customerId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        // 전화번호 변경 시 중복 확인
        if (request.getPhone() != null && !request.getPhone().equals(customer.getPhone())) {
            if (customerRepository.existsByBusinessIdAndPhone(businessId, request.getPhone())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                        "이미 등록된 전화번호입니다: " + request.getPhone());
            }
        }

        // 수정할 필드만 업데이트
        Customer updatedCustomer = Customer.builder()
                .id(customer.getId())
                .name(request.getName() != null ? request.getName() : customer.getName())
                .phone(request.getPhone() != null ? request.getPhone() : customer.getPhone())
                .email(request.getEmail() != null ? request.getEmail() : customer.getEmail())
                .tags(request.getTags() != null ? request.getTags() : customer.getTags())
                .adminMemo(request.getAdminMemo() != null ? request.getAdminMemo() : customer.getAdminMemo())
                .kakaoUserKey(request.getKakaoUserKey() != null ? request.getKakaoUserKey() : customer.getKakaoUserKey())
                .build();

        customerRepository.update(updatedCustomer);

        log.info("Customer updated: id={}, businessId={}", customerId, businessId);

        return getCustomer(businessId, customerId);
    }

    /**
     * Customer 방문 통계 업데이트 (예약 완료 시 호출)
     */
    @Transactional
    public void updateVisitStats(Long customerId, int additionalSpent, LocalDate visitDate) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND));

        // 방문 횟수 증가
        customer.incrementVisitCount();

        // 총 결제 금액 증가
        customer.addSpent(additionalSpent);

        // 최근 방문일 업데이트
        customer.updateLastVisitDate(visitDate);

        // 통계 업데이트
        customerRepository.updateVisitStats(
                customerId,
                customer.getVisitCount(),
                customer.getTotalSpent(),
                customer.getLastVisitDate()
        );

        // 태그 자동 업데이트 (VIP, 단골)
        autoUpdateTags(customer);

        log.info("Customer visit stats updated: id={}, visitCount={}, totalSpent={}",
                customerId, customer.getVisitCount(), customer.getTotalSpent());
    }

    /**
     * 태그 자동 업데이트 (VIP, 단골, 신규)
     */
    private void autoUpdateTags(Customer customer) {
        List<String> tags = customer.getTags();
        if (tags == null) {
            tags = new java.util.ArrayList<>();
        }

        // VIP 태그 (10회 이상)
        if (customer.isVip() && !tags.contains("VIP")) {
            tags.add("VIP");
        }

        // 단골 태그 (3회 이상)
        if (customer.isRegular() && !tags.contains("단골")) {
            tags.add("단골");
        }

        // 신규 태그 제거 (2회 이상 방문 시)
        if (customer.getVisitCount() > 1 && tags.contains("신규")) {
            tags.remove("신규");
        }

        // 태그 업데이트
        Customer updatedCustomer = Customer.builder()
                .id(customer.getId())
                .tags(tags)
                .build();

        customerRepository.update(updatedCustomer);
    }

    /**
     * Customer 삭제
     */
    @Transactional
    public void deleteCustomer(Long businessId, Long customerId) {
        // Business의 Customer인지 확인
        if (!customerRepository.existsByBusinessIdAndId(businessId, customerId)) {
            throw new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
        }

        customerRepository.delete(customerId);

        log.info("Customer deleted: id={}, businessId={}", customerId, businessId);
    }
}