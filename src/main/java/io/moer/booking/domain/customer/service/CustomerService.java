package io.moer.booking.domain.customer.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.customer.Customer;
import io.moer.booking.domain.customer.dto.*;
import io.moer.booking.domain.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
}