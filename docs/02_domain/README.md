# 02. 도메인 가이드

moer 예약 시스템의 도메인별 상세 가이드입니다.

## 목차

1. [도메인 개발 패턴](#도메인-개발-패턴)
2. [도메인 목록](#도메인-목록)
3. [상세 문서](#상세-문서)

## 도메인 개발 패턴

모든 도메인은 동일한 구조와 패턴을 따릅니다:

```
domain/{domain-name}/
├── {Entity}.java               # 도메인 엔티티
├── {Enum}.java                 # 도메인 열거형
├── controller/
│   └── {Domain}Controller.java # REST API 컨트롤러
├── dto/
│   ├── {Domain}Response.java   # 응답 DTO
│   ├── {Domain}CreateRequest.java  # 생성 요청 DTO
│   ├── {Domain}UpdateRequest.java  # 수정 요청 DTO
│   └── {Domain}SearchCondition.java # 검색 조건 DTO
├── repository/
│   └── {Domain}Repository.java # MyBatis 인터페이스
└── service/
    └── {Domain}Service.java    # 비즈니스 로직
```

## 표준 개발 흐름

### 1. Entity 정의

```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private Long id;
    private String email;
    private String password;
    private String name;
    private UserRole role;      // Enum
    private UserStatus status;  // Enum
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 헬퍼 메서드
    public boolean isAdmin() {
        return this.role == UserRole.ADMIN;
    }
}
```

**포인트**:
- Lombok `@Getter`, `@Builder` 사용
- Enum 타입 활용
- 헬퍼 메서드로 비즈니스 로직 캡슐화

### 2. DTO 정의

#### 요청 DTO
```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserCreateRequest {
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다")
    private String password;

    @NotBlank(message = "이름은 필수입니다")
    private String name;

    @NotNull(message = "역할은 필수입니다")
    private UserRole role;
}
```

#### 응답 DTO
```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private UserRole role;
    private UserStatus status;
    private LocalDateTime createdAt;

    // Entity → DTO 변환
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
```

**포인트**:
- Request DTO에 Validation 어노테이션 사용
- Response DTO는 비밀번호 등 민감 정보 제외
- `from()` 정적 메서드로 Entity → DTO 변환

### 3. Repository 정의

```java
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

    // 동적 쿼리 (검색)
    List<User> findByCondition(
            @Param("condition") UserSearchCondition condition,
            @Param("offset") int offset,
            @Param("limit") int limit
    );
    int countByCondition(@Param("condition") UserSearchCondition condition);
}
```

**포인트**:
- `@Mapper` 어노테이션 필수
- `Optional<T>` 반환으로 null-safety
- 동적 쿼리는 `@Param` 사용

### 4. MyBatis XML 매핑

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.moer.booking.domain.user.repository.UserRepository">

    <!-- ResultMap -->
    <resultMap id="userResultMap" type="io.moer.booking.domain.user.User">
        <id property="id" column="id"/>
        <result property="email" column="email"/>
        <result property="name" column="name"/>
        <result property="role" column="role"
                typeHandler="org.apache.ibatis.type.EnumTypeHandler"/>
        <result property="status" column="status"
                typeHandler="org.apache.ibatis.type.EnumTypeHandler"/>
        <result property="createdAt" column="created_at"/>
        <result property="updatedAt" column="updated_at"/>
    </resultMap>

    <!-- 저장 -->
    <insert id="save" parameterType="io.moer.booking.domain.user.User"
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO users (
            email, password, name, role, status
        ) VALUES (
            #{email}, #{password}, #{name},
            #{role}::user_role, #{status}::user_status
        )
    </insert>

    <!-- 조회 -->
    <select id="findById" resultMap="userResultMap">
        SELECT * FROM users WHERE id = #{id}
    </select>

    <!-- 동적 쿼리 -->
    <select id="findByCondition" resultMap="userResultMap">
        SELECT * FROM users
        <where>
            <if test="condition.businessId != null">
                AND business_id = #{condition.businessId}
            </if>
            <if test="condition.role != null">
                AND role = #{condition.role}::user_role
            </if>
            <if test="condition.keyword != null and condition.keyword != ''">
                AND (
                    name ILIKE '%' || #{condition.keyword} || '%'
                    OR email ILIKE '%' || #{condition.keyword} || '%'
                )
            </if>
        </where>
        ORDER BY created_at DESC
        LIMIT #{limit} OFFSET #{offset}
    </select>
</mapper>
```

**포인트**:
- `resultMap`으로 컬럼 매핑
- Enum은 `EnumTypeHandler` 사용
- PostgreSQL Enum은 `::enum_type` 캐스팅
- 동적 쿼리는 `<where>`, `<if>` 사용

### 5. Service 구현

```java
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 생성
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user: email={}", request.getEmail());

        // 1. 비즈니스 규칙 검증 (이메일 중복 체크)
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            throw new BusinessException(
                ErrorCode.DUPLICATE_EMAIL,
                "이미 사용 중인 이메일입니다"
            );
        });

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. Entity 생성
        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .build();

        // 4. 저장
        userRepository.save(user);

        log.info("User created: id={}", user.getId());

        // 5. Entity → DTO 변환
        return UserResponse.from(user);
    }

    /**
     * 사용자 조회
     */
    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                    ErrorCode.USER_NOT_FOUND,
                    "사용자를 찾을 수 없습니다"
                ));

        return UserResponse.from(user);
    }

    /**
     * 사용자 목록 조회 (페이징)
     */
    public PageResponse<UserResponse> getUsers(
            UserSearchCondition condition, int page, int size) {

        int offset = (page - 1) * size;

        List<User> users = userRepository.findByCondition(condition, offset, size);
        int totalElements = userRepository.countByCondition(condition);

        List<UserResponse> content = users.stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());

        PageInfo pageInfo = PageInfo.of(page, size, totalElements);

        return new PageResponse<>(content, pageInfo);
    }
}
```

**포인트**:
- `@Transactional(readOnly = true)` 클래스 레벨
- 쓰기 작업은 `@Transactional` 메서드 레벨
- 비즈니스 규칙 검증 → 예외 발생
- Entity ↔ DTO 변환은 Service에서

### 6. Controller 구현

```java
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

        UserResponse response = userService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * 사용자 조회
     */
    @GetMapping("/{id}")
    @Operation(summary = "사용자 상세 조회")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(
            @PathVariable Long id) {

        UserResponse response = userService.getUser(id);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 목록 조회
     */
    @GetMapping
    @Operation(summary = "사용자 목록 조회")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getUsers(
            @ModelAttribute UserSearchCondition condition,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<UserResponse> response = userService.getUsers(condition, page, size);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

**포인트**:
- `@RestController` + `@RequestMapping`
- `@Valid`로 DTO 검증
- `ApiResponse<T>` 통합 응답 포맷
- Swagger 어노테이션 (`@Tag`, `@Operation`)

## 도메인 목록

| 도메인 | 설명 | 주요 기능 |
|--------|------|----------|
| **auth** | 인증/인가 | 로그인, 회원가입, 토큰 갱신 |
| **user** | 사용자 관리 | CRUD, 역할 관리 |
| **business** | 매장 관리 | 매장 정보, 설정 관리 |
| **staff** | 직원 관리 | 직원 CRUD, 포트폴리오 |
| **service** | 서비스 메뉴 | 시술/수업 메뉴 관리 |
| **customer** | 고객 관리 | 고객 CRUD, 이력 관리 |
| **reservation** | 예약 관리 | 예약 생성/조회/상태 관리 |
| **holiday** | 휴무일 관리 | 특별 휴무일 관리 |
| **dashboard** | 대시보드 | 통계 및 요약 정보 |

## 상세 문서

각 도메인의 상세 구현 가이드:

- [User 도메인](./user.md) - 사용자 관리
- [Business 도메인](./business.md) - 매장 관리
- [Reservation 도메인](./reservation.md) - 예약 관리 (가장 복잡)
- [도메인 개발 패턴 상세](./development-pattern.md) - 새 도메인 추가 가이드

## 공통 규칙

### 1. 명명 규칙
- **Entity**: `{Domain}.java` (예: `User.java`)
- **Request DTO**: `{Domain}{Action}Request.java` (예: `UserCreateRequest.java`)
- **Response DTO**: `{Domain}Response.java` (예: `UserResponse.java`)
- **Repository**: `{Domain}Repository.java`
- **Service**: `{Domain}Service.java`
- **Controller**: `{Domain}Controller.java`

### 2. 패키지 구조
```
domain/{domain}/
├── {Entity}.java           # 루트에 엔티티
├── {Enum}.java             # 루트에 열거형
├── controller/             # 컨트롤러 패키지
├── dto/                    # DTO 패키지
├── repository/             # 레포지토리 패키지
└── service/                # 서비스 패키지
```

### 3. 데이터베이스 규칙
- 테이블명: 복수형 소문자 (예: `users`, `businesses`, `reservations`)
- 컬럼명: snake_case (예: `created_at`, `business_id`)
- Primary Key: `id BIGSERIAL PRIMARY KEY`
- Timestamp: `created_at`, `updated_at`

### 4. API 엔드포인트 규칙
```
GET    /api/{resources}          # 목록 조회
POST   /api/{resources}          # 생성
GET    /api/{resources}/{id}     # 단건 조회
PUT    /api/{resources}/{id}     # 전체 수정
PATCH  /api/{resources}/{id}     # 부분 수정
DELETE /api/{resources}/{id}     # 삭제
```

## 다음 단계

새로운 도메인을 추가하려면 [도메인 개발 패턴 상세](./development-pattern.md) 문서를 참고하세요.
