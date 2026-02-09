# 레이어 아키텍처

moer 예약 시스템의 레이어 아키텍처 패턴을 설명합니다.

## 개요

시스템은 전형적인 3-Tier Layered Architecture를 따릅니다:

```
┌─────────────────────────────────────┐
│   Presentation Layer (Controller)   │  ← REST API 엔드포인트
├─────────────────────────────────────┤
│   Business Layer (Service)          │  ← 비즈니스 로직
├─────────────────────────────────────┤
│   Persistence Layer (Repository)    │  ← 데이터 접근
├─────────────────────────────────────┤
│   Database (PostgreSQL)              │  ← 영속성 저장소
└─────────────────────────────────────┘
```

## 1. Presentation Layer (Controller)

REST API 엔드포인트를 제공하며, HTTP 요청/응답을 처리합니다.

### 역할
- HTTP 요청 파싱 및 검증 (`@Valid`, `@RequestBody`)
- 비즈니스 로직 호출 (Service 계층)
- 응답 DTO 변환 및 반환 (`ApiResponse<T>`)
- 인증 정보 접근 (`@AuthenticationPrincipal`)

### 실제 코드 예시

```java
package io.moer.booking.domain.user.controller;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User", description = "사용자 관리 API")
public class UserController {

    private final UserService userService;

    /**
     * 사용자 생성
     */
    @PostMapping
    @Operation(summary = "사용자 생성")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserCreateRequest request) {

        // Service 호출
        UserResponse response = userService.createUser(request);

        // 통합 응답 포맷으로 반환
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 사용자 조회 (인증된 사용자 정보 사용)
     */
    @GetMapping("/{id}")
    @Operation(summary = "사용자 상세 조회")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        // Service 호출
        UserResponse response = userService.getUser(id, currentUser.getUser());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 목록 조회 (페이징)
     */
    @GetMapping
    @Operation(summary = "사용자 목록 조회")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @ModelAttribute UserSearchCondition condition,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Service 호출
        PageResponse<UserResponse> response = userService.getUsers(condition, page, size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

### Controller 규칙

1. **@RestController 사용**: `@Controller` + `@ResponseBody`
2. **ApiResponse<T> 반환**: 통합 응답 포맷
3. **Service만 의존**: Repository 직접 호출 금지
4. **DTO 사용**: Entity를 직접 노출하지 않음
5. **@Valid 검증**: 요청 DTO 자동 검증

## 2. Business Layer (Service)

비즈니스 로직을 처리하며, 트랜잭션을 관리합니다.

### 역할
- 비즈니스 규칙 검증
- 트랜잭션 관리 (`@Transactional`)
- Repository 조합 및 호출
- Entity ↔ DTO 변환
- 예외 발생 (`BusinessException` 등)

### 실제 코드 예시

```java
package io.moer.booking.domain.user.service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 생성
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        log.info("사용자 생성 시작: email={}", request.getEmail());

        // 1. 비즈니스 규칙 검증 (이메일 중복 체크)
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new BusinessException(
                ErrorCode.USER_EMAIL_DUPLICATED,
                "이미 사용 중인 이메일입니다: " + request.getEmail()
            );
        });

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. Entity 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .phone(request.getPhone())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .businessId(request.getBusinessId())
                .staffId(request.getStaffId())
                .build();

        // 4. Repository 호출 (저장)
        userRepository.save(user);

        log.info("사용자 생성 완료: id={}", user.getId());

        // 5. Entity → DTO 변환
        return UserResponse.from(user);
    }

    /**
     * 사용자 조회 (권한 체크 포함)
     */
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id, User currentUser) {
        log.info("사용자 조회: id={}, currentUserId={}", id, currentUser.getId());

        // 1. Repository 호출 (조회)
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                    ErrorCode.USER_NOT_FOUND,
                    "사용자를 찾을 수 없습니다: " + id
                ));

        // 2. 비즈니스 규칙 검증 (접근 권한 체크)
        if (!currentUser.canAccessUser(user)) {
            throw new BusinessException(
                ErrorCode.USER_ACCESS_DENIED,
                "사용자 정보에 접근할 권한이 없습니다"
            );
        }

        // 3. Entity → DTO 변환
        return UserResponse.from(user);
    }

    /**
     * 사용자 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(
            UserSearchCondition condition, int page, int size) {

        log.info("사용자 목록 조회: condition={}, page={}, size={}",
                 condition, page, size);

        // 1. 페이징 설정
        int offset = (page - 1) * size;

        // 2. Repository 호출 (목록 조회)
        List<User> users = userRepository.findByCondition(condition, offset, size);

        // 3. Repository 호출 (전체 개수)
        int totalElements = userRepository.countByCondition(condition);

        // 4. Entity → DTO 변환
        List<UserResponse> content = users.stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());

        // 5. 페이징 정보 생성
        PageInfo pageInfo = PageInfo.of(page, size, totalElements);

        return new PageResponse<>(content, pageInfo);
    }
}
```

### Service 규칙

1. **@Service 사용**: Spring 빈 등록
2. **@Transactional 관리**:
   - 쓰기 작업: `@Transactional`
   - 읽기 작업: `@Transactional(readOnly = true)`
3. **비즈니스 규칙 검증**: 예외 발생으로 처리
4. **Entity ↔ DTO 변환**: Service에서 수행
5. **로깅**: 주요 동작 로깅

### 복잡한 비즈니스 로직 예시 (ReservationService)

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final ServiceRepository serviceRepository;
    private final SpecialHolidayRepository specialHolidayRepository;
    private final CustomerHistoryRepository customerHistoryRepository;

    /**
     * 예약 생성 (복잡한 비즈니스 로직)
     */
    @Transactional
    public ReservationResponse createReservation(
            Long businessId, ReservationCreateRequest request, User currentUser) {

        log.info("예약 생성 시작: businessId={}, request={}", businessId, request);

        // 1. 권한 체크
        if (!currentUser.canAccessBusiness(businessId)) {
            throw new BusinessException(
                ErrorCode.BUSINESS_ACCESS_DENIED,
                "매장에 접근할 권한이 없습니다"
            );
        }

        // 2. 고객 처리 (없으면 자동 생성)
        Customer customer = getOrCreateCustomer(businessId, request);

        // 3. 직원 검증
        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new EntityNotFoundException(
                    ErrorCode.STAFF_NOT_FOUND,
                    "직원을 찾을 수 없습니다: " + request.getStaffId()
                ));

        // 4. 서비스 검증 및 시간/가격 계산
        List<Service> services = validateAndGetServices(request.getServiceIds());
        int totalDuration = services.stream()
                .mapToInt(Service::getDuration)
                .sum();
        int totalPrice = services.stream()
                .mapToInt(Service::getPrice)
                .sum();

        LocalTime endTime = request.getStartTime().plusMinutes(totalDuration);

        // 5. 휴무일 체크
        if (isHoliday(businessId, request.getReservationDate())) {
            throw new BusinessException(
                ErrorCode.RESERVATION_HOLIDAY,
                "해당 날짜는 휴무일입니다"
            );
        }

        // 6. 시간 충돌 체크
        if (hasTimeConflict(staff.getId(), request.getReservationDate(),
                           request.getStartTime(), endTime)) {
            throw new BusinessException(
                ErrorCode.RESERVATION_TIME_CONFLICT,
                "해당 시간에 이미 예약이 있습니다"
            );
        }

        // 7. 예약 생성
        Reservation reservation = Reservation.builder()
                .businessId(businessId)
                .customerId(customer.getId())
                .staffId(staff.getId())
                .reservationDate(request.getReservationDate())
                .startTime(request.getStartTime())
                .endTime(endTime)
                .services(convertServicesToJson(services))
                .totalDuration(totalDuration)
                .totalPrice(totalPrice)
                .status(ReservationStatus.PENDING)
                .notes(request.getNotes())
                .build();

        reservationRepository.save(reservation);

        log.info("예약 생성 완료: id={}", reservation.getId());

        return ReservationResponse.from(reservation, customer, staff);
    }

    /**
     * 고객 조회 또는 자동 생성
     */
    private Customer getOrCreateCustomer(Long businessId, ReservationCreateRequest request) {
        if (request.getCustomerId() != null) {
            // 기존 고객
            return customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.CUSTOMER_NOT_FOUND,
                        "고객을 찾을 수 없습니다"
                    ));
        } else {
            // 신규 고객 자동 생성
            return customerRepository.findByBusinessIdAndPhone(
                    businessId, request.getCustomerPhone())
                .orElseGet(() -> {
                    Customer newCustomer = Customer.builder()
                            .businessId(businessId)
                            .name(request.getCustomerName())
                            .phone(request.getCustomerPhone())
                            .visitCount(0)
                            .totalSpent(0)
                            .build();
                    customerRepository.save(newCustomer);
                    return newCustomer;
                });
        }
    }
}
```

## 3. Persistence Layer (Repository)

데이터베이스 접근을 추상화하며, MyBatis를 통해 SQL을 실행합니다.

### 역할
- CRUD 메서드 정의
- 복잡한 쿼리 메서드 정의
- MyBatis XML과 매핑

### 실제 코드 예시

```java
package io.moer.booking.domain.user.repository;

@Mapper
public interface UserRepository {

    // 기본 CRUD
    void save(User user);
    Optional<User> findById(Long id);
    void update(User user);
    void deleteById(Long id);

    // 커스텀 조회
    Optional<User> findByEmail(String email);
    List<User> findByBusinessId(Long businessId);

    // 동적 쿼리
    List<User> findByCondition(
            @Param("condition") UserSearchCondition condition,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    int countByCondition(@Param("condition") UserSearchCondition condition);
}
```

### MyBatis XML 매핑

`src/main/resources/mapper/user/UserMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.moer.booking.domain.user.repository.UserRepository">

    <!-- ResultMap -->
    <resultMap id="userResultMap" type="io.moer.booking.domain.user.User">
        <id property="id" column="id"/>
        <result property="email" column="email"/>
        <result property="password" column="password"/>
        <result property="name" column="name"/>
        <result property="phone" column="phone"/>
        <result property="role" column="role" typeHandler="org.apache.ibatis.type.EnumTypeHandler"/>
        <result property="status" column="status" typeHandler="org.apache.ibatis.type.EnumTypeHandler"/>
        <result property="businessId" column="business_id"/>
        <result property="staffId" column="staff_id"/>
        <result property="emailVerified" column="email_verified"/>
        <result property="emailVerifiedAt" column="email_verified_at"/>
        <result property="lastLoginAt" column="last_login_at"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <!-- 저장 -->
    <insert id="save" parameterType="io.moer.booking.domain.user.User"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO users (
            email, password, name, phone, role, status,
            business_id, staff_id, email_verified
        ) VALUES (
            #{email}, #{password}, #{name}, #{phone},
            #{role}::user_role, #{status}::user_status,
            #{businessId}, #{staffId}, #{emailVerified}
        )
    </insert>

    <!-- 조회 -->
    <select id="findById" resultMap="userResultMap">
        SELECT * FROM users WHERE id = #{id}
    </select>

    <!-- 동적 쿼리 (검색 조건) -->
    <select id="findByCondition" resultMap="userResultMap">
        SELECT * FROM users
        <where>
            <if test="condition.businessId != null">
                AND business_id = #{condition.businessId}
            </if>
            <if test="condition.role != null">
                AND role = #{condition.role}::user_role
            </if>
            <if test="condition.status != null">
                AND status = #{condition.status}::user_status
            </if>
            <if test="condition.keyword != null and condition.keyword != ''">
                AND (
                    name ILIKE '%' || #{condition.keyword} || '%'
                    OR email ILIKE '%' || #{condition.keyword} || '%'
                    OR phone ILIKE '%' || #{condition.keyword} || '%'
                )
            </if>
        </where>
        ORDER BY created_at DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>
</mapper>
```

### Repository 규칙

1. **@Mapper 사용**: MyBatis 인터페이스 표시
2. **Optional 반환**: 단건 조회 시 null-safety
3. **@Param 사용**: 파라미터 2개 이상일 때
4. **메서드명 규칙**:
   - `save()` - 저장
   - `findById()` - ID 조회
   - `findByXxx()` - 특정 조건 조회
   - `update()` - 수정
   - `deleteById()` - 삭제
   - `countByXxx()` - 개수 조회

## 레이어 간 데이터 흐름

### 요청 처리 흐름

```
1. Client Request
   ↓
2. Controller (Presentation Layer)
   - HTTP 요청 파싱
   - DTO 검증 (@Valid)
   ↓
3. Service (Business Layer)
   - 비즈니스 규칙 검증
   - Repository 호출
   - Entity ↔ DTO 변환
   ↓
4. Repository (Persistence Layer)
   - MyBatis를 통해 SQL 실행
   ↓
5. Database (PostgreSQL)
   - 데이터 저장/조회
   ↓
6. Entity 반환
   ↓
7. Service에서 DTO 변환
   ↓
8. Controller에서 ApiResponse 래핑
   ↓
9. Client Response
```

### 실제 흐름 예시 (사용자 생성)

```java
// 1. Client Request
POST /api/users
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동"
}

// 2. Controller
@PostMapping
public ResponseEntity<ApiResponse<UserResponse>> createUser(
        @Valid @RequestBody UserCreateRequest request) {
    UserResponse response = userService.createUser(request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response));
}

// 3. Service
@Transactional
public UserResponse createUser(UserCreateRequest request) {
    // 비즈니스 규칙 검증
    validateEmailDuplication(request.getEmail());

    // Entity 생성
    User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .build();

    // Repository 호출
    userRepository.save(user);

    // DTO 변환
    return UserResponse.from(user);
}

// 4. Repository
@Mapper
public interface UserRepository {
    void save(User user);
}

// 5. MyBatis XML
<insert id="save" parameterType="User" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO users (email, password, name) VALUES (#{email}, #{password}, #{name})
</insert>

// 6-9. 역순으로 반환
Entity → DTO → ApiResponse → HTTP Response
```

## 레이어 간 의존성 규칙

### 의존 방향
```
Controller → Service → Repository
     ↓          ↓          ↓
    DTO      Entity     Entity
```

### 금지 사항
- ❌ Controller에서 Repository 직접 호출
- ❌ Controller에서 Entity 직접 노출
- ❌ Repository에서 DTO 반환
- ❌ Entity에서 Service/Repository 의존

### 권장 사항
- ✅ Controller는 Service만 의존
- ✅ Service에서 Entity ↔ DTO 변환
- ✅ Repository는 Entity만 반환
- ✅ Entity는 순수 도메인 모델 유지

## 트랜잭션 관리

### @Transactional 사용 원칙

```java
@Service
public class UserService {

    // 쓰기 작업 - @Transactional
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // 여러 Repository 호출 시 하나의 트랜잭션으로 묶임
        userRepository.save(user);
        // 예외 발생 시 자동 롤백
    }

    // 읽기 작업 - @Transactional(readOnly = true)
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        // 읽기 전용 - 성능 최적화
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(...);
    }
}
```

### 트랜잭션 전파

```java
@Service
public class ReservationService {

    @Transactional  // 외부 트랜잭션
    public void createReservation(ReservationCreateRequest request) {
        // 1. 예약 생성
        reservationRepository.save(reservation);

        // 2. 고객 정보 업데이트 (같은 트랜잭션)
        customerService.updateVisitCount(customerId);  // ← 내부에서도 @Transactional

        // 3. 예약 이력 생성 (같은 트랜잭션)
        customerHistoryRepository.save(history);
    }
}
```

## 다음 문서

- [보안 구조](./security.md)
- [예외 처리](./exception-handling.md)
