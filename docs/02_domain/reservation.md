# Reservation 도메인

예약 관리 도메인의 상세 가이드입니다. 이 도메인은 시스템에서 가장 복잡한 비즈니스 로직을 포함합니다.

## 개요

### 주요 기능
- 예약 생성 (고객 자동 생성 지원)
- 예약 조회 (다양한 검색 조건)
- 예약 상태 관리 (PENDING → CONFIRMED → COMPLETED)
- 시간 충돌 검증
- 휴무일 체크
- 예약 완료 시 자동으로 고객 이력 생성

### 도메인 구조
```
domain/reservation/
├── Reservation.java                    # 예약 엔티티
├── ReservationStatus.java              # 상태 열거형
├── controller/
│   ├── ReservationController.java      # 예약 생성/수정/상태변경
│   └── ReservationQueryController.java # 예약 조회 (분리)
├── dto/
│   ├── ReservationResponse.java
│   ├── ReservationCreateRequest.java
│   ├── ReservationUpdateRequest.java
│   └── ReservationSearchCondition.java
├── repository/
│   └── ReservationRepository.java
└── service/
    └── ReservationService.java         # 복잡한 비즈니스 로직
```

## Entity

### Reservation.java

**위치**: `src/main/java/io/moer/booking/domain/reservation/Reservation.java`

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {
    private Long id;
    private Long businessId;
    private Long customerId;
    private Long staffId;  // nullable

    /**
     * 예약 번호 (고유)
     * 형식: YYMMDD-XXXX (예: 260210-A3B9)
     */
    private String reservationNumber;

    /**
     * 예약 날짜
     */
    private LocalDate reservationDate;

    /**
     * 시작 시간
     */
    private LocalTime startTime;

    /**
     * 종료 시간 (계산됨)
     */
    private LocalTime endTime;

    /**
     * 서비스 목록 (JSONB)
     * [{id, name, price, duration}, ...]
     */
    private List<Map<String, Object>> services;

    /**
     * 총 소요 시간 (분)
     */
    private Integer totalDuration;

    /**
     * 총 금액
     */
    private Integer totalPrice;

    /**
     * 예약 상태
     */
    private ReservationStatus status;

    /**
     * 고객 메모 (요청사항)
     */
    private String customerMemo;

    /**
     * 직원 메모 (내부 메모)
     */
    private String staffMemo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ========================================
    // 상태 전이 메서드
    // ========================================

    /**
     * 예약 확정
     */
    public void confirm() {
        if (this.status != ReservationStatus.PENDING) {
            throw new BusinessException(
                ErrorCode.RESERVATION_INVALID_STATUS,
                "대기 중인 예약만 확정할 수 있습니다"
            );
        }
        this.status = ReservationStatus.CONFIRMED;
    }

    /**
     * 예약 취소
     */
    public void cancel() {
        if (this.status == ReservationStatus.COMPLETED) {
            throw new BusinessException(
                ErrorCode.RESERVATION_INVALID_STATUS,
                "완료된 예약은 취소할 수 없습니다"
            );
        }
        if (this.status == ReservationStatus.CANCELLED) {
            throw new BusinessException(
                ErrorCode.RESERVATION_INVALID_STATUS,
                "이미 취소된 예약입니다"
            );
        }
        this.status = ReservationStatus.CANCELLED;
    }

    /**
     * 예약 완료
     */
    public void complete() {
        if (this.status != ReservationStatus.CONFIRMED) {
            throw new BusinessException(
                ErrorCode.RESERVATION_INVALID_STATUS,
                "확정된 예약만 완료할 수 있습니다"
            );
        }
        this.status = ReservationStatus.COMPLETED;
    }

    /**
     * 노쇼 처리
     */
    public void markAsNoShow() {
        if (this.status != ReservationStatus.CONFIRMED) {
            throw new BusinessException(
                ErrorCode.RESERVATION_INVALID_STATUS,
                "확정된 예약만 노쇼 처리할 수 있습니다"
            );
        }
        this.status = ReservationStatus.NO_SHOW;
    }
}
```

### ReservationStatus.java

```java
public enum ReservationStatus {
    PENDING,    // 대기 (예약 요청)
    CONFIRMED,  // 확정
    COMPLETED,  // 완료
    CANCELLED,  // 취소
    NO_SHOW     // 노쇼
}
```

**상태 전이 다이어그램**:
```
PENDING ─────→ CONFIRMED ─────→ COMPLETED
   │               │
   │               ↓
   └────────→ CANCELLED
   │
   └────────→ NO_SHOW (확정 상태에서만)
```

## DTO

### ReservationCreateRequest.java

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationCreateRequest {

    /**
     * 고객 ID (선택)
     * - 있으면: 기존 고객 사용
     * - 없으면: customerName + customerPhone으로 조회/생성
     */
    private Long customerId;

    /**
     * 고객 이름 (customerId 없을 때 필수)
     */
    private String customerName;

    /**
     * 고객 전화번호 (customerId 없을 때 필수)
     */
    @Pattern(regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$",
             message = "전화번호 형식이 올바르지 않습니다")
    private String customerPhone;

    /**
     * 직원 ID (선택)
     */
    private Long staffId;

    /**
     * 예약 날짜
     */
    @NotNull(message = "예약 날짜는 필수입니다")
    @Future(message = "과거 날짜는 예약할 수 없습니다")
    private LocalDate reservationDate;

    /**
     * 시작 시간
     */
    @NotNull(message = "시작 시간은 필수입니다")
    private LocalTime startTime;

    /**
     * 서비스 ID 목록
     */
    @NotEmpty(message = "서비스는 최소 1개 이상 선택해야 합니다")
    private List<Long> serviceIds;

    /**
     * 고객 메모 (요청사항)
     */
    @Size(max = 500, message = "메모는 500자 이하여야 합니다")
    private String customerMemo;
}
```

### ReservationResponse.java

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {
    private Long id;
    private String reservationNumber;
    private LocalDate reservationDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer totalDuration;
    private Integer totalPrice;
    private ReservationStatus status;
    private String customerMemo;
    private String staffMemo;

    // 조인된 정보
    private CustomerInfo customer;
    private StaffInfo staff;  // nullable
    private List<ServiceInfo> services;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 중첩 클래스
    @Getter
    @Builder
    public static class CustomerInfo {
        private Long id;
        private String name;
        private String phone;
    }

    @Getter
    @Builder
    public static class StaffInfo {
        private Long id;
        private String name;
        private String specialty;
    }

    @Getter
    @Builder
    public static class ServiceInfo {
        private Long id;
        private String name;
        private Integer price;
        private Integer duration;
    }

    /**
     * Entity + 관련 엔티티 → DTO 변환
     */
    public static ReservationResponse from(
            Reservation reservation,
            Customer customer,
            Staff staff) {

        // services JSONB → ServiceInfo 변환
        List<ServiceInfo> serviceInfos = reservation.getServices().stream()
                .map(service -> ServiceInfo.builder()
                        .id(((Number) service.get("id")).longValue())
                        .name((String) service.get("name"))
                        .price((Integer) service.get("price"))
                        .duration((Integer) service.get("duration"))
                        .build())
                .collect(Collectors.toList());

        return ReservationResponse.builder()
                .id(reservation.getId())
                .reservationNumber(reservation.getReservationNumber())
                .reservationDate(reservation.getReservationDate())
                .startTime(reservation.getStartTime())
                .endTime(reservation.getEndTime())
                .totalDuration(reservation.getTotalDuration())
                .totalPrice(reservation.getTotalPrice())
                .status(reservation.getStatus())
                .customerMemo(reservation.getCustomerMemo())
                .staffMemo(reservation.getStaffMemo())
                .customer(CustomerInfo.builder()
                        .id(customer.getId())
                        .name(customer.getName())
                        .phone(customer.getPhone())
                        .build())
                .staff(staff != null ? StaffInfo.builder()
                        .id(staff.getId())
                        .name(staff.getName())
                        .specialty(staff.getSpecialty())
                        .build() : null)
                .services(serviceInfos)
                .createdAt(reservation.getCreatedAt())
                .updatedAt(reservation.getUpdatedAt())
                .build();
    }
}
```

## Service

### ReservationService.java

**위치**: `src/main/java/io/moer/booking/domain/reservation/service/ReservationService.java`

#### 예약 생성 (핵심 로직)

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BusinessRepository businessRepository;
    private final CustomerRepository customerRepository;
    private final CustomerService customerService;
    private final StaffRepository staffRepository;
    private final ServiceRepository serviceRepository;
    private final SpecialHolidayRepository specialHolidayRepository;
    private final CustomerHistoryService customerHistoryService;

    /**
     * 예약 생성 (복잡한 비즈니스 로직)
     */
    @Transactional
    public ReservationResponse createReservation(
            Long businessId, ReservationCreateRequest request) {

        log.info("Creating reservation: businessId={}, date={}, time={}",
                 businessId, request.getReservationDate(), request.getStartTime());

        // 1. Business 존재 확인
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new EntityNotFoundException(
                    ErrorCode.BUSINESS_NOT_FOUND,
                    "매장을 찾을 수 없습니다"
                ));

        // 2. Customer 조회 또는 자동 생성
        Customer customer = resolveCustomer(businessId, request);

        // 3. Staff 존재 확인 (선택 시)
        Staff staff = null;
        if (request.getStaffId() != null) {
            staff = staffRepository.findById(request.getStaffId())
                    .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.STAFF_NOT_FOUND,
                        "직원을 찾을 수 없습니다"
                    ));

            // Business 일치 확인
            if (!staff.getBusinessId().equals(businessId)) {
                throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "다른 매장의 직원입니다"
                );
            }
        }

        // 4. Service 조회 및 검증
        List<Service> services = request.getServiceIds().stream()
                .map(serviceId -> serviceRepository.findById(serviceId)
                        .orElseThrow(() -> new EntityNotFoundException(
                            ErrorCode.SERVICE_NOT_FOUND,
                            "서비스를 찾을 수 없습니다: " + serviceId
                        )))
                .collect(Collectors.toList());

        // Business 일치 확인
        services.forEach(service -> {
            if (!service.getBusinessId().equals(businessId)) {
                throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "다른 매장의 서비스입니다: " + service.getName()
                );
            }
        });

        // 5. 총 시간 및 가격 계산
        int totalDuration = services.stream()
                .mapToInt(Service::getDuration)
                .sum();

        int totalPrice = services.stream()
                .mapToInt(Service::getPrice)
                .sum();

        // 6. 종료 시간 계산
        LocalTime endTime = request.getStartTime().plusMinutes(totalDuration);

        // 7. 예약 가능 여부 검증
        validateReservation(businessId, request.getStaffId(),
                           request.getReservationDate(),
                           request.getStartTime(), endTime, null);

        // 8. 예약 번호 생성
        String reservationNumber = generateReservationNumber(
                request.getReservationDate());

        // 9. services JSONB 데이터 생성
        List<Map<String, Object>> servicesJsonb = services.stream()
                .map(service -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", service.getId());
                    map.put("name", service.getName());
                    map.put("price", service.getPrice());
                    map.put("duration", service.getDuration());
                    return map;
                })
                .collect(Collectors.toList());

        // 10. Reservation 엔티티 생성
        Reservation reservation = Reservation.builder()
                .businessId(businessId)
                .customerId(customer.getId())
                .staffId(request.getStaffId())
                .reservationNumber(reservationNumber)
                .reservationDate(request.getReservationDate())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .services(servicesJsonb)
                .totalDuration(totalDuration)
                .totalPrice(totalPrice)
                .status(ReservationStatus.PENDING)
                .customerMemo(request.getCustomerMemo())
                .staffMemo(null)
                .build();

        // 11. 저장
        reservationRepository.save(reservation);

        log.info("Reservation created: id={}, number={}, customer={}",
                 reservation.getId(), reservationNumber, customer.getName());

        return getReservation(businessId, reservation.getId());
    }

    /**
     * Customer 확인 로직
     *
     * Case 1: customerId가 있으면 → 기존 고객 사용 (관리자가 직접 선택)
     * Case 2: customerId가 없으면 → 이름/전화번호로 조회 또는 자동 생성
     */
    private Customer resolveCustomer(
            Long businessId, ReservationCreateRequest request) {

        // Case 1: customerId가 있으면 기존 고객 사용
        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.CUSTOMER_NOT_FOUND,
                        "고객을 찾을 수 없습니다"
                    ));

            // Business가 일치하는지 확인
            if (!customer.getBusinessId().equals(businessId)) {
                throw new BusinessException(
                    ErrorCode.INVALID_INPUT_VALUE,
                    "다른 매장의 고객입니다"
                );
            }

            log.info("Using existing customer: id={}, name={}",
                     customer.getId(), customer.getName());
            return customer;
        }

        // Case 2: 이름/전화번호로 조회 또는 자동 생성
        // 검증: customerId가 없으면 이름과 전화번호 필수
        if (request.getCustomerName() == null || request.getCustomerName().isBlank()) {
            throw new BusinessException(
                ErrorCode.INVALID_INPUT_VALUE,
                "고객 ID 또는 고객 이름은 필수입니다"
            );
        }
        if (request.getCustomerPhone() == null || request.getCustomerPhone().isBlank()) {
            throw new BusinessException(
                ErrorCode.INVALID_INPUT_VALUE,
                "고객 ID 또는 고객 전화번호는 필수입니다"
            );
        }

        return customerService.findOrCreateCustomer(
                businessId,
                request.getCustomerName(),
                request.getCustomerPhone()
        );
    }

    /**
     * 예약 가능 여부 검증
     *
     * 1. 과거 날짜 체크
     * 2. 휴무일 체크
     * 3. 시간 충돌 체크
     */
    private void validateReservation(
            Long businessId, Long staffId,
            LocalDate reservationDate, LocalTime startTime, LocalTime endTime,
            Long excludeReservationId) {

        // 1. 과거 날짜 체크
        if (reservationDate.isBefore(LocalDate.now())) {
            throw new BusinessException(
                ErrorCode.RESERVATION_PAST_DATE,
                "과거 날짜는 예약할 수 없습니다"
            );
        }

        // 2. 휴무일 체크
        boolean isHoliday = specialHolidayRepository
                .existsByBusinessIdAndDate(businessId, reservationDate);

        if (isHoliday) {
            throw new BusinessException(
                ErrorCode.RESERVATION_HOLIDAY,
                "해당 날짜는 휴무일입니다"
            );
        }

        // 3. 시간 충돌 체크 (staffId가 있을 때만)
        if (staffId != null) {
            boolean hasConflict = reservationRepository
                    .existsConflictingReservation(
                            staffId, reservationDate, startTime, endTime,
                            excludeReservationId
                    );

            if (hasConflict) {
                throw new BusinessException(
                    ErrorCode.RESERVATION_TIME_CONFLICT,
                    Map.of(
                        "staffId", staffId,
                        "date", reservationDate,
                        "startTime", startTime,
                        "endTime", endTime
                    )
                );
            }
        }
    }

    /**
     * 예약 번호 생성
     * 형식: YYMMDD-RANDOM4 (예: 260210-A3B9)
     */
    private String generateReservationNumber(LocalDate date) {
        String datePrefix = date.format(DateTimeFormatter.ofPattern("yyMMdd"));
        String randomSuffix = generateRandomString(4);
        return datePrefix + "-" + randomSuffix;
    }

    private String generateRandomString(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 혼동되는 문자 제외
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
```

#### 예약 상태 변경

```java
/**
 * 예약 확정
 */
@Transactional
public void confirmReservation(Long businessId, Long id) {
    Reservation reservation = getReservationEntity(businessId, id);

    // 상태 전이 (Entity 메서드 사용)
    reservation.confirm();

    reservationRepository.update(reservation);

    log.info("Reservation confirmed: id={}", id);

    // TODO: 카카오톡 알림 발송
}

/**
 * 예약 완료 (자동으로 CustomerHistory 생성)
 */
@Transactional
public void completeReservation(Long businessId, Long id) {
    Reservation reservation = getReservationEntity(businessId, id);

    // 상태 전이
    reservation.complete();
    reservationRepository.update(reservation);

    // 고객 이력 자동 생성
    customerHistoryService.createHistoryFromReservation(reservation);

    log.info("Reservation completed: id={}, customer history created", id);
}
```

## Repository

### ReservationRepository.java

```java
@Mapper
public interface ReservationRepository {

    // 기본 CRUD
    void save(Reservation reservation);
    Optional<Reservation> findById(Long id);
    void update(Reservation reservation);
    void deleteById(Long id);

    // 예약 조회 (조인 포함)
    Optional<ReservationResponse> findDetailById(
            @Param("id") Long id,
            @Param("businessId") Long businessId
    );

    // 시간 충돌 체크
    boolean existsConflictingReservation(
            @Param("staffId") Long staffId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") Long excludeId
    );

    // 동적 검색
    List<ReservationResponse> findByCondition(
            @Param("condition") ReservationSearchCondition condition,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countByCondition(@Param("condition") ReservationSearchCondition condition);
}
```

## MyBatis XML

### ReservationMapper.xml (중요 부분)

```xml
<!-- 시간 충돌 체크 -->
<select id="existsConflictingReservation" resultType="boolean">
    SELECT EXISTS (
        SELECT 1
        FROM reservations
        WHERE staff_id = #{staffId}
          AND reservation_date = #{date}
          AND status IN ('PENDING', 'CONFIRMED')
          AND (
              -- 새 예약의 시작 시간이 기존 예약 시간대에 포함
              (#{startTime} >= start_time AND #{startTime} < end_time)
              OR
              -- 새 예약의 종료 시간이 기존 예약 시간대에 포함
              (#{endTime} > start_time AND #{endTime} <= end_time)
              OR
              -- 새 예약이 기존 예약을 완전히 포함
              (#{startTime} <= start_time AND #{endTime} >= end_time)
          )
          <if test="excludeId != null">
              AND id != #{excludeId}
          </if>
    )
</select>

<!-- 조인 조회 -->
<select id="findDetailById" resultType="io.moer.booking.domain.reservation.dto.ReservationResponse">
    SELECT
        r.*,
        c.id as customer_id,
        c.name as customer_name,
        c.phone as customer_phone,
        s.id as staff_id,
        s.name as staff_name,
        s.specialty as staff_specialty
    FROM reservations r
    INNER JOIN customers c ON r.customer_id = c.id
    LEFT JOIN staffs s ON r.staff_id = s.id
    WHERE r.id = #{id}
      AND r.business_id = #{businessId}
</select>
```

## API 엔드포인트

### ReservationController.java

```
POST   /api/businesses/{businessId}/reservations
       - 예약 생성

GET    /api/businesses/{businessId}/reservations
       - 예약 목록 조회 (검색/페이징)

GET    /api/businesses/{businessId}/reservations/{id}
       - 예약 상세 조회

PUT    /api/businesses/{businessId}/reservations/{id}
       - 예약 수정

PATCH  /api/businesses/{businessId}/reservations/{id}/confirm
       - 예약 확정

PATCH  /api/businesses/{businessId}/reservations/{id}/complete
       - 예약 완료

PATCH  /api/businesses/{businessId}/reservations/{id}/cancel
       - 예약 취소

DELETE /api/businesses/{businessId}/reservations/{id}
       - 예약 삭제
```

## 비즈니스 규칙 요약

### 예약 생성 시
1. ✅ 과거 날짜 예약 불가
2. ✅ 휴무일 예약 불가
3. ✅ 동일 직원의 시간 충돌 불가
4. ✅ customerId 없으면 자동으로 고객 생성
5. ✅ 서비스 목록 기반으로 총 시간/가격 자동 계산
6. ✅ 예약 번호 자동 생성 (YYMMDD-XXXX)

### 상태 전이
- `PENDING` → `CONFIRMED` (확정)
- `CONFIRMED` → `COMPLETED` (완료)
- `CONFIRMED` → `NO_SHOW` (노쇼)
- `PENDING/CONFIRMED` → `CANCELLED` (취소)

### 예약 완료 시
- ✅ 자동으로 `CustomerHistory` 생성
- ✅ 고객의 `visitCount`, `totalSpent` 업데이트

## 다음 문서

- [User 도메인](./user.md)
- [Business 도메인](./business.md)
- [도메인 개발 패턴](./development-pattern.md)
