package io.moer.booking.domain.customer.service;

import io.moer.booking.common.exception.BusinessException;
import io.moer.booking.common.exception.EntityNotFoundException;
import io.moer.booking.common.exception.ErrorCode;
import io.moer.booking.common.util.HtmlSanitizer;
import io.moer.booking.common.util.MaskingUtils;
import io.moer.booking.domain.business.BusinessSettings;
import io.moer.booking.domain.business.repository.BusinessRepository;
import io.moer.booking.domain.business.repository.BusinessSettingsRepository;
import io.moer.booking.domain.customer.Customer;
import io.moer.booking.domain.customer.CustomerNote;
import io.moer.booking.domain.customer.dto.*;
import io.moer.booking.domain.customer.repository.CustomerNoteRepository;
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
    private final CustomerNoteRepository customerNoteRepository;
    private final BusinessRepository businessRepository;
    private final BusinessSettingsRepository businessSettingsRepository;
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

        // SECURITY (P1-7): PII 로그 마스킹
        log.info("Customer created: id={}, businessId={}, name={}, phone={}",
                customer.getId(), businessId,
                MaskingUtils.maskName(customer.getName()),
                MaskingUtils.maskPhone(customer.getPhone()));

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
     * 고객 세그멘테이션 조회
     */
    public CustomerSegmentResponse getCustomerSegment(Long businessId, String segmentType) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        List<Customer> customers;
        switch (segmentType.toUpperCase()) {
            case "VIP":
                customers = customerRepository.findVipCustomers(businessId);
                break;
            case "INACTIVE":
                customers = customerRepository.findInactiveCustomers(businessId, 3);
                break;
            case "BIRTHDAY":
                customers = customerRepository.findBirthdayCustomers(businessId, 7);
                break;
            case "NEW":
                customers = customerRepository.findNewCustomers(businessId);
                break;
            case "FREQUENT":
                customers = customerRepository.findFrequentCustomers(businessId);
                break;
            default:
                throw new BusinessException(ErrorCode.INVALID_SEGMENT_TYPE);
        }

        List<CustomerSegmentItem> items = customers.stream()
                .map(CustomerSegmentItem::from)
                .collect(Collectors.toList());

        return CustomerSegmentResponse.builder()
                .segmentType(segmentType.toUpperCase())
                .count(items.size())
                .customers(items)
                .build();
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
            // SECURITY (P1-7): PII 로그 마스킹
            log.info("Found existing customer: id={}, name={}, phone={}",
                    existingCustomer.get().getId(),
                    MaskingUtils.maskName(existingCustomer.get().getName()),
                    MaskingUtils.maskPhone(phone));
            return existingCustomer.get();
        }

        // 2. 없으면 새로 생성
        log.info("Creating new customer: name={}, phone={}",
                MaskingUtils.maskName(name), MaskingUtils.maskPhone(phone));

        Customer newCustomer = Customer.builder()
                .businessId(businessId)
                .name(name)
                .phone(phone)
                .visitCount(0)
                .totalSpent(0)
                .build();

        customerRepository.save(newCustomer);

        // SECURITY (P1-7): PII 로그 마스킹
        log.info("Customer created: id={}, businessId={}, name={}, phone={}",
                newCustomer.getId(), businessId,
                MaskingUtils.maskName(name), MaskingUtils.maskPhone(phone));

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

        // tags 자동 업데이트 (매장별 설정 참조)
        String newTags = generateAutoTags(newVisitCount, customer.getBusinessId());

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

        // tags 자동 업데이트 (매장별 설정 참조)
        String newTags = generateAutoTags(newVisitCount, customer.getBusinessId());

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

    // ========================================
    // 고객 메모 CRUD
    // ========================================

    /**
     * 고객 메모 생성
     */
    @Transactional
    public CustomerNoteResponse createNote(Long businessId, Long customerId,
                                           CustomerNoteRequest request, Long userId, String userName) {
        validateCustomerBelongsToBusiness(businessId, customerId);

        CustomerNote note = CustomerNote.builder()
                .customerId(customerId)
                .businessId(businessId)
                // SECURITY (P1-5): 입력 텍스트 정화 (XSS 방어)
                .content(HtmlSanitizer.plainText(request.getContent()))
                .isPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : false)
                .authorId(userId)
                .authorName(userName)
                .build();

        customerNoteRepository.save(note);

        log.info("Customer note created: noteId={}, customerId={}, businessId={}", note.getId(), customerId, businessId);

        return CustomerNoteResponse.from(note);
    }

    /**
     * 고객 메모 목록 조회
     */
    public List<CustomerNoteResponse> getNotes(Long businessId, Long customerId) {
        validateCustomerBelongsToBusiness(businessId, customerId);

        return customerNoteRepository.findByCustomerIdAndBusinessId(customerId, businessId).stream()
                .map(CustomerNoteResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 고객 메모 수정
     */
    @Transactional
    public CustomerNoteResponse updateNote(Long businessId, Long customerId, Long noteId,
                                           CustomerNoteRequest request) {
        validateCustomerBelongsToBusiness(businessId, customerId);

        CustomerNote note = customerNoteRepository.findById(noteId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.CUSTOMER_NOTE_NOT_FOUND));

        CustomerNote updatedNote = CustomerNote.builder()
                .id(note.getId())
                .customerId(note.getCustomerId())
                .businessId(note.getBusinessId())
                // SECURITY (P1-5): 입력 텍스트 정화 (XSS 방어)
                .content(HtmlSanitizer.plainText(request.getContent()))
                .isPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : note.getIsPrivate())
                .authorId(note.getAuthorId())
                .authorName(note.getAuthorName())
                .createdAt(note.getCreatedAt())
                .build();

        customerNoteRepository.update(updatedNote);

        log.info("Customer note updated: noteId={}, customerId={}", noteId, customerId);

        return customerNoteRepository.findById(noteId)
                .map(CustomerNoteResponse::from)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.CUSTOMER_NOTE_NOT_FOUND));
    }

    /**
     * 고객 메모 삭제
     */
    @Transactional
    public void deleteNote(Long businessId, Long customerId, Long noteId) {
        validateCustomerBelongsToBusiness(businessId, customerId);

        customerNoteRepository.findById(noteId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.CUSTOMER_NOTE_NOT_FOUND));

        customerNoteRepository.deleteById(noteId);

        log.info("Customer note deleted: noteId={}, customerId={}", noteId, customerId);
    }

    // ========================================
    // 고객 태그 관리
    // ========================================

    /**
     * 고객 태그 수정
     */
    @Transactional
    public CustomerResponse updateTags(Long businessId, Long customerId, CustomerTagRequest request) {
        validateCustomerBelongsToBusiness(businessId, customerId);

        List<String> tags = request.getTags();

        // 태그 개수 검증 (최대 10개)
        if (tags.size() > 10) {
            throw new BusinessException(ErrorCode.CUSTOMER_TAG_LIMIT_EXCEEDED);
        }

        // 각 태그 길이 검증 (최대 20자)
        for (String tag : tags) {
            if (tag.length() > 20) {
                throw new BusinessException(ErrorCode.CUSTOMER_TAG_LENGTH_EXCEEDED,
                        "태그 '" + tag + "'이(가) 20자를 초과합니다");
            }
        }

        String tagsString = tags.isEmpty() ? null : String.join(",", tags);
        customerRepository.updateTags(customerId, businessId, tagsString);

        log.info("Customer tags updated: customerId={}, businessId={}, tags={}", customerId, businessId, tagsString);

        return getCustomer(businessId, customerId);
    }

    /**
     * 매장의 모든 고유 태그 조회
     */
    public CustomerTagResponse getAllTags(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        List<String> tags = customerRepository.findAllTagsByBusinessId(businessId);
        return CustomerTagResponse.of(tags);
    }

    // ========================================
    // 고객 CSV 내보내기
    // ========================================

    /**
     * 내보내기용 고객 목록 조회
     */
    public List<Customer> getCustomersForExport(Long businessId, String segment,
                                                 List<String> tags, String startDate, String endDate) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        return customerRepository.findForExport(businessId, segment, tags, startDate, endDate);
    }

    // ========================================
    // 고객 중복 감지 및 병합
    // ========================================

    /**
     * 전화번호 기반 중복 고객 감지
     */
    public List<DuplicateCustomerResponse> findDuplicates(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new EntityNotFoundException(ErrorCode.BUSINESS_NOT_FOUND);
        }

        List<Customer> duplicates = customerRepository.findDuplicatesByPhone(businessId);

        // 전화번호별로 그룹핑
        Map<String, List<Customer>> grouped = duplicates.stream()
                .collect(Collectors.groupingBy(Customer::getPhone));

        return grouped.entrySet().stream()
                .map(entry -> DuplicateCustomerResponse.builder()
                        .phone(entry.getKey())
                        .count(entry.getValue().size())
                        .customers(entry.getValue().stream()
                                .map(CustomerResponse::from)
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 고객 병합
     */
    @Transactional
    public CustomerMergeResponse mergeCustomers(Long businessId, CustomerMergeRequest request) {
        Long primaryId = request.getPrimaryCustomerId();
        List<Long> mergeIds = request.getMergeCustomerIds();

        // CRM004: primaryId가 mergeIds에 포함되면 안 됨
        if (mergeIds.contains(primaryId)) {
            throw new BusinessException(ErrorCode.CUSTOMER_MERGE_PRIMARY_CONFLICT);
        }

        // CRM005: 병합 목록이 비어있으면 안 됨
        if (mergeIds.isEmpty()) {
            throw new BusinessException(ErrorCode.CUSTOMER_MERGE_EMPTY);
        }

        // 주 고객 확인
        Customer primary = customerRepository.findById(primaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND,
                        "주 고객을 찾을 수 없습니다: " + primaryId));

        if (!primary.getBusinessId().equals(businessId)) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND,
                    "해당 매장의 고객이 아닙니다: " + primaryId);
        }

        int totalMergedReservations = 0;
        int totalMergedNotes = 0;
        int additionalVisitCount = 0;
        int additionalTotalSpent = 0;
        java.util.Set<String> mergedTags = new java.util.LinkedHashSet<>();

        // 주 고객의 기존 태그 추가
        if (primary.getTags() != null && !primary.getTags().isEmpty()) {
            mergedTags.addAll(primary.getTagList());
        }

        for (Long mergeId : mergeIds) {
            Customer mergeCustomer = customerRepository.findById(mergeId)
                    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND,
                            "병합 대상 고객을 찾을 수 없습니다: " + mergeId));

            if (!mergeCustomer.getBusinessId().equals(businessId)) {
                throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND,
                        "해당 매장의 고객이 아닙니다: " + mergeId);
            }

            // 1. 예약 이관
            int movedReservations = customerRepository.updateReservationCustomerId(mergeId, primaryId);
            totalMergedReservations += movedReservations;

            // 2. 메모 이관
            int movedNotes = customerNoteRepository.updateCustomerId(mergeId, primaryId);
            totalMergedNotes += movedNotes;

            // 3. 통계 합산
            additionalVisitCount += (mergeCustomer.getVisitCount() != null ? mergeCustomer.getVisitCount() : 0);
            additionalTotalSpent += (mergeCustomer.getTotalSpent() != null ? mergeCustomer.getTotalSpent() : 0);

            // 4. 태그 합치기
            if (mergeCustomer.getTags() != null && !mergeCustomer.getTags().isEmpty()) {
                mergedTags.addAll(mergeCustomer.getTagList());
            }

            // 5. 병합된 고객 삭제
            customerRepository.delete(mergeId);

            log.info("Customer merged: mergeId={} -> primaryId={}", mergeId, primaryId);
        }

        // 주 고객 통계 업데이트
        int newVisitCount = (primary.getVisitCount() != null ? primary.getVisitCount() : 0) + additionalVisitCount;
        int newTotalSpent = (primary.getTotalSpent() != null ? primary.getTotalSpent() : 0) + additionalTotalSpent;
        customerRepository.updateVisitStats(primaryId, newVisitCount, newTotalSpent, primary.getLastVisitDate());

        // 태그 업데이트 (최대 10개)
        List<String> finalTags = new java.util.ArrayList<>(mergedTags);
        if (finalTags.size() > 10) {
            finalTags = finalTags.subList(0, 10);
        }
        String tagsString = finalTags.isEmpty() ? null : String.join(",", finalTags);
        customerRepository.updateTags(primaryId, businessId, tagsString);

        // 최종 고객 정보 조회
        Customer mergedCustomer = customerRepository.findById(primaryId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND));

        log.info("Customer merge completed: primaryId={}, mergedIds={}, reservations={}, notes={}",
                primaryId, mergeIds, totalMergedReservations, totalMergedNotes);

        return CustomerMergeResponse.builder()
                .primaryCustomerId(primaryId)
                .mergedCustomerIds(mergeIds)
                .mergedReservationCount(totalMergedReservations)
                .mergedNoteCount(totalMergedNotes)
                .mergedCustomer(CustomerResponse.from(mergedCustomer))
                .build();
    }

    // ========================================
    // Private 헬퍼
    // ========================================

    /**
     * 고객이 해당 매장 소속인지 검증
     */
    private void validateCustomerBelongsToBusiness(Long businessId, Long customerId) {
        if (!customerRepository.existsByBusinessIdAndId(businessId, customerId)) {
            throw new EntityNotFoundException(ErrorCode.CUSTOMER_NOT_FOUND);
        }
    }

    /**
     * visitCount 기반 자동 태그 생성
     * 매장별 설정된 임계값을 참조 (미설정 시 기본값: VIP=10, 단골=3)
     */
    private String generateAutoTags(int visitCount, Long businessId) {
        int regularThreshold = 3;
        int vipThreshold = 10;

        if (businessId != null) {
            BusinessSettings settings = businessSettingsRepository.findByBusinessId(businessId).orElse(null);
            if (settings != null) {
                regularThreshold = settings.getRegularThresholdValue();
                vipThreshold = settings.getVipThresholdValue();
            }
        }

        List<String> tags = new java.util.ArrayList<>();

        if (visitCount >= vipThreshold) {
            tags.add("VIP");
        }
        if (visitCount >= regularThreshold) {
            tags.add("단골");
        }
        if (visitCount == 1) {
            tags.add("신규");
        }

        return tags.isEmpty() ? null : String.join(",", tags);
    }
}