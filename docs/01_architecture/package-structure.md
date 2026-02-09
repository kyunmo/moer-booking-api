# 패키지 구조

moer 예약 시스템의 패키지 구조를 상세히 설명합니다.

## 전체 구조

```
io.moer.booking/
├── MoerBookingApplication.java     # Spring Boot 메인 클래스
│
├── common/                          # 공통 모듈
│   ├── config/                      # 설정 클래스
│   ├── controller/                  # 공통 컨트롤러
│   ├── dto/                         # 공통 DTO
│   ├── exception/                   # 예외 처리
│   ├── mybatis/                     # MyBatis 커스텀 핸들러
│   ├── security/                    # 보안/인증
│   └── util/                        # 유틸리티
│
└── domain/                          # 도메인 모듈
    ├── auth/                        # 인증 도메인
    ├── user/                        # 사용자 도메인
    ├── business/                    # 매장 도메인
    ├── staff/                       # 직원 도메인
    ├── service/                     # 서비스 메뉴 도메인
    ├── customer/                    # 고객 도메인
    ├── reservation/                 # 예약 도메인
    ├── holiday/                     # 특별 휴무일 도메인
    └── dashboard/                   # 대시보드 도메인
```

## 1. Common 패키지

공통 기능과 횡단 관심사를 담당하는 패키지입니다.

### 1.1 config 패키지

시스템 설정 클래스들을 포함합니다.

```
common/config/
├── MyBatisConfig.java          # MyBatis 설정
├── SecurityConfig.java         # Spring Security 설정
├── SwaggerConfig.java          # Swagger/OpenAPI 설정
└── WebConfig.java              # CORS 설정
```

**예시: SecurityConfig.java**
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
```

### 1.2 dto 패키지

공통 응답 DTO를 포함합니다.

```
common/dto/
├── ApiResponse.java            # 통합 API 응답
├── PageResponse.java           # 페이징 응답
├── PageInfo.java               # 페이지 정보
└── HealthResponse.java         # 헬스 체크 응답
```

**예시: ApiResponse.java**
```java
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final ErrorInfo error;
    private final LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorInfo(code, message));
    }
}
```

### 1.3 exception 패키지

전역 예외 처리를 담당합니다.

```
common/exception/
├── ErrorCode.java                  # 에러 코드 열거형
├── BaseException.java              # 기본 예외 클래스
├── BusinessException.java          # 비즈니스 예외
├── EntityNotFoundException.java    # 엔티티 미존재 예외
└── GlobalExceptionHandler.java     # 전역 예외 처리기
```

**ErrorCode 구조**:
- `C001~` - 공통 에러
- `U001~` - User 도메인 에러
- `B001~` - Business 도메인 에러
- `S001~` - Staff 도메인 에러
- `R001~` - Reservation 도메인 에러

### 1.4 mybatis 패키지

MyBatis 커스텀 TypeHandler를 포함합니다.

```
common/mybatis/
└── JsonTypeHandler.java        # JSONB ↔ Java Map/List 변환
```

**예시**:
```java
@MappedTypes({Map.class, List.class})
@MappedJdbcTypes(JdbcType.OTHER)
public class JsonTypeHandler extends BaseTypeHandler<Object> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                     Object parameter, JdbcType jdbcType) {
        // Java → JSONB 변환
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName) {
        // JSONB → Java 변환
    }
}
```

### 1.5 security 패키지

JWT 인증 관련 클래스들을 포함합니다.

```
common/security/
├── JwtTokenProvider.java           # JWT 생성/파싱
├── JwtAuthenticationFilter.java    # JWT 필터
├── JwtProperties.java              # JWT 설정
├── CustomUserDetails.java          # UserDetails 구현체
└── CustomUserDetailsService.java   # UserDetailsService 구현체
```

**인증 흐름**:
1. `JwtAuthenticationFilter` - 요청에서 JWT 추출
2. `JwtTokenProvider` - JWT 검증 및 파싱
3. `CustomUserDetailsService` - 사용자 정보 로딩
4. `SecurityContext` - 인증 정보 저장

### 1.6 util 패키지

유틸리티 클래스를 포함합니다.

```
common/util/
└── DateTimeUtils.java          # 날짜/시간 유틸
```

## 2. Domain 패키지

각 도메인은 독립적인 구조를 가지며, 다음 패턴을 따릅니다.

### 도메인 표준 구조

```
domain/{domain-name}/
├── {Entity}.java               # 엔티티 (도메인 모델)
├── {Enum}.java                 # 도메인 열거형
├── controller/
│   └── {Domain}Controller.java
├── dto/
│   ├── {Domain}Response.java
│   ├── {Domain}CreateRequest.java
│   ├── {Domain}UpdateRequest.java
│   └── {Domain}SearchCondition.java
├── repository/
│   └── {Domain}Repository.java
└── service/
    └── {Domain}Service.java
```

### 2.1 auth 도메인

인증/인가를 담당합니다.

```
domain/auth/
├── RefreshToken.java           # 엔티티
├── controller/
│   └── AuthController.java     # 로그인/회원가입/토큰갱신
├── dto/
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── RegisterRequest.java
│   ├── RegisterResponse.java
│   └── RefreshTokenRequest.java
├── repository/
│   └── RefreshTokenRepository.java
└── service/
    └── AuthService.java
```

**주요 기능**:
- POST `/api/auth/login` - 로그인
- POST `/api/auth/register` - 회원가입 (User + Business 동시 생성)
- POST `/api/auth/refresh` - Access Token 갱신
- POST `/api/auth/logout` - 로그아웃 (Refresh Token 삭제)

### 2.2 user 도메인

사용자 관리를 담당합니다.

```
domain/user/
├── User.java                   # 엔티티
├── UserRole.java               # Enum: ADMIN, OWNER, STAFF
├── UserStatus.java             # Enum: ACTIVE, INACTIVE, SUSPENDED
├── controller/
│   └── UserController.java
├── dto/
│   ├── UserResponse.java
│   ├── UserCreateRequest.java
│   ├── UserUpdateRequest.java
│   └── UserSearchCondition.java
├── repository/
│   └── UserRepository.java
└── service/
    └── UserService.java
```

**역할**:
- `ADMIN` - 시스템 관리자
- `OWNER` - 매장 소유주
- `STAFF` - 직원 (예약 관리 권한)

### 2.3 business 도메인

매장 관리를 담당합니다.

```
domain/business/
├── Business.java               # 매장 엔티티
├── BusinessSettings.java       # 매장 설정 엔티티
├── BusinessType.java           # Enum: BEAUTY_SHOP, PILATES, CAFE
├── BusinessStatus.java         # Enum: ACTIVE, INACTIVE, SUSPENDED
├── controller/
│   └── BusinessController.java
├── dto/
│   ├── BusinessResponse.java
│   ├── BusinessCreateRequest.java
│   ├── BusinessUpdateRequest.java
│   └── BusinessSearchCondition.java
├── repository/
│   ├── BusinessRepository.java
│   └── BusinessSettingsRepository.java
└── service/
    └── BusinessService.java
```

**Business vs BusinessSettings**:
- `Business` - 매장 기본 정보 (이름, 주소, 영업시간 등)
- `BusinessSettings` - 예약 설정 (예약 간격, 자동 확정, 알림 설정 등)

### 2.4 staff 도메인

직원 및 포트폴리오 관리를 담당합니다.

```
domain/staff/
├── Staff.java                  # 직원 엔티티
├── Portfolio.java              # 포트폴리오 엔티티
├── controller/
│   ├── StaffController.java
│   └── PortfolioController.java
├── dto/
│   ├── StaffResponse.java
│   ├── StaffCreateRequest.java
│   ├── StaffUpdateRequest.java
│   ├── PortfolioResponse.java
│   └── PortfolioCreateRequest.java
├── repository/
│   ├── StaffRepository.java
│   └── PortfolioRepository.java
└── service/
    ├── StaffService.java
    └── PortfolioService.java
```

**Staff 특징**:
- `specialty` - 전문 분야 (컷 전문, 필라테스 강사 등)
- `career_years` - 경력 연수
- `is_active` - 활성 상태

### 2.5 service 도메인

서비스 메뉴 관리를 담당합니다.

```
domain/service/
├── Service.java                # 서비스 메뉴 엔티티
├── controller/
│   └── ServiceController.java
├── dto/
│   ├── ServiceResponse.java
│   ├── ServiceCreateRequest.java
│   ├── ServiceUpdateRequest.java
│   └── ServiceSearchCondition.java
├── repository/
│   └── ServiceRepository.java
├── service/
│   └── ServiceService.java
└── util/
    └── ServiceCombinationCalculator.java   # 서비스 조합 계산
```

**Service 특징**:
- `duration` - 소요 시간 (분)
- `price` - 가격
- `staff_ids` - 담당 가능한 직원 목록 (TEXT, 콤마 구분)

### 2.6 customer 도메인

고객 및 이력 관리를 담당합니다.

```
domain/customer/
├── Customer.java               # 고객 엔티티
├── CustomerHistory.java        # 고객 이력 엔티티
├── controller/
│   ├── CustomerController.java
│   └── CustomerHistoryController.java
├── dto/
│   ├── CustomerResponse.java
│   ├── CustomerCreateRequest.java
│   ├── CustomerUpdateRequest.java
│   ├── CustomerSearchCondition.java
│   ├── CustomerHistoryResponse.java
│   └── CustomerHistoryCreateRequest.java
├── repository/
│   ├── CustomerRepository.java
│   └── CustomerHistoryRepository.java
└── service/
    ├── CustomerService.java
    └── CustomerHistoryService.java
```

**Customer 특징**:
- `visit_count` - 방문 횟수 (자동 집계)
- `total_spent` - 총 결제 금액 (자동 집계)
- `tags` - 태그 (VIP, 단골, 신규 등, TEXT 콤마 구분)

### 2.7 reservation 도메인

예약 관리를 담당합니다.

```
domain/reservation/
├── Reservation.java            # 예약 엔티티
├── ReservationStatus.java      # Enum: PENDING, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW
├── controller/
│   ├── ReservationController.java        # 예약 생성/수정/상태변경
│   └── ReservationQueryController.java   # 예약 조회
├── dto/
│   ├── ReservationResponse.java
│   ├── ReservationCreateRequest.java
│   ├── ReservationUpdateRequest.java
│   └── ReservationSearchCondition.java
├── repository/
│   └── ReservationRepository.java
└── service/
    └── ReservationService.java           # 복잡한 비즈니스 로직
```

**ReservationService 주요 기능**:
- 고객 자동 생성 (전화번호 기반)
- 시간 충돌 검증
- 휴무일 체크
- 상태 전이 제어
- 예약 완료 시 자동으로 CustomerHistory 생성

### 2.8 holiday 도메인

특별 휴무일 관리를 담당합니다.

```
domain/holiday/
├── SpecialHoliday.java         # 특별 휴무일 엔티티
├── controller/
│   └── SpecialHolidayController.java
├── dto/
│   ├── SpecialHolidayResponse.java
│   └── SpecialHolidayCreateRequest.java
├── repository/
│   └── SpecialHolidayRepository.java
└── service/
    └── SpecialHolidayService.java
```

### 2.9 dashboard 도메인

대시보드 통계를 담당합니다.

```
domain/dashboard/
├── controller/
│   └── DashboardController.java
├── dto/
│   ├── DashboardResponse.java      # 통합 대시보드 응답
│   ├── TodayStats.java             # 오늘 통계
│   ├── WeekStats.java              # 주간 통계
│   ├── MonthStats.java             # 월간 통계
│   ├── DailyCount.java             # 일별 카운트
│   ├── RecentReservation.java      # 최근 예약
│   └── RecentCustomer.java         # 최근 고객
└── service/
    └── DashboardService.java       # 대시보드 통계 집계
```

**DashboardService 기능**:
- 오늘/주간/월간 예약 통계
- 일별 예약 건수 그래프 데이터
- 최근 예약/고객 목록

## 패키지 명명 규칙

### 1. 클래스 명명
- **Entity**: `{Domain}.java` (예: `User.java`, `Business.java`)
- **DTO (Request)**: `{Domain}{Action}Request.java` (예: `UserCreateRequest.java`)
- **DTO (Response)**: `{Domain}Response.java` (예: `UserResponse.java`)
- **Controller**: `{Domain}Controller.java` (예: `UserController.java`)
- **Service**: `{Domain}Service.java` (예: `UserService.java`)
- **Repository**: `{Domain}Repository.java` (예: `UserRepository.java`)
- **Enum**: `{Domain}{Meaning}.java` (예: `UserRole.java`, `ReservationStatus.java`)

### 2. 패키지 명명
- 소문자 사용
- 복합어는 붙여쓰기 (예: `customerhistory` 대신 간단히 `customer/`)
- 도메인명은 단수형 사용 (예: `user`, `business`, `reservation`)

## 의존성 방향

```
Controller → Service → Repository → Database
     ↓          ↓
    DTO      Entity
     ↑          ↑
    └──────────┘
    (변환)
```

**규칙**:
- Controller는 Service에만 의존
- Service는 Repository에만 의존
- Repository는 Entity를 반환
- Service에서 Entity ↔ DTO 변환 수행
- Entity는 다른 레이어에 의존하지 않음 (순수 도메인 모델)

## 다음 문서

- [레이어 아키텍처](./layered-architecture.md)
- [보안 구조](./security.md)
- [예외 처리](./exception-handling.md)
