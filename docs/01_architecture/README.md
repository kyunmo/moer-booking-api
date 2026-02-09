# 01. 아키텍처 개요

moer 예약 시스템 백엔드의 전체 아키텍처를 설명합니다.

## 목차

1. [패키지 구조](./package-structure.md)
2. [레이어 아키텍처](./layered-architecture.md)
3. [보안 구조](./security.md)
4. [예외 처리](./exception-handling.md)

## 시스템 개요

### 프로젝트 정보
- **프로젝트명**: moer-booking
- **목적**: 다업종 예약 관리 시스템 백엔드 API (미용실, 필라테스, 카페 등)
- **아키텍처 스타일**: Layered Architecture + Domain-Driven Design (DDD)

### 기술 스택

| 분류 | 기술 |
|------|------|
| **Language** | Java 17 |
| **Framework** | Spring Boot 4.0.1 |
| **Database** | PostgreSQL 16 (JSONB 활용) |
| **ORM** | MyBatis 4.0.0 |
| **Security** | Spring Security 7 + JWT |
| **Build** | Gradle (Kotlin DSL) |
| **Documentation** | Swagger/OpenAPI 3.0 |
| **Container** | Docker Compose |

## 핵심 아키텍처 원칙

### 1. 도메인 중심 설계 (DDD)
```
io.moer.booking/
├── common/          # 공통 기능 (설정, 보안, 예외처리)
└── domain/          # 비즈니스 도메인
    ├── auth/        # 인증
    ├── user/        # 사용자
    ├── business/    # 매장
    ├── staff/       # 직원
    ├── service/     # 서비스 메뉴
    ├── customer/    # 고객
    ├── reservation/ # 예약
    ├── holiday/     # 특별 휴무일
    └── dashboard/   # 대시보드
```

각 도메인은 독립적인 패키지로 구성되며, 다음 구조를 따릅니다:
- `entity/` - 엔티티 (도메인 모델)
- `dto/` - 데이터 전송 객체
- `controller/` - REST API 컨트롤러
- `service/` - 비즈니스 로직
- `repository/` - 데이터 접근 계층

### 2. 레이어 아키텍처
```
┌─────────────────────────────────────┐
│   Presentation Layer (Controller)   │  ← REST API
├─────────────────────────────────────┤
│   Business Layer (Service)          │  ← 비즈니스 로직
├─────────────────────────────────────┤
│   Persistence Layer (Repository)    │  ← 데이터 접근
├─────────────────────────────────────┤
│   Database (PostgreSQL)              │  ← 영속성 저장소
└─────────────────────────────────────┘
```

### 3. 횡단 관심사 (Cross-Cutting Concerns)
- **보안**: JWT 기반 인증/인가 (SecurityConfig, JwtAuthenticationFilter)
- **예외 처리**: 전역 예외 핸들러 (GlobalExceptionHandler)
- **API 응답**: 통합 응답 포맷 (ApiResponse)
- **로깅**: SLF4J + Logback
- **CORS**: 프론트엔드 연동 설정

## 주요 설계 패턴

### 1. Repository Pattern
MyBatis를 사용한 데이터 접근 추상화
```java
public interface UserRepository {
    void save(User user);
    Optional<User> findById(Long id);
    List<User> findAll();
}
```

### 2. DTO Pattern
계층 간 데이터 전송을 위한 전용 객체
```java
public class UserCreateRequest { ... }  // 요청 DTO
public class UserResponse { ... }       // 응답 DTO
```

### 3. Service Layer Pattern
비즈니스 로직을 Service에 집중
```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    // 비즈니스 로직
}
```

### 4. Exception Handling Pattern
계층화된 예외 처리
```
BaseException (추상 클래스)
├── BusinessException (비즈니스 규칙 위반)
├── EntityNotFoundException (엔티티 미존재)
└── ... (도메인별 예외)
```

## 데이터베이스 설계 특징

### 1. JSONB 타입 활용
PostgreSQL의 JSONB를 사용하여 유연한 데이터 구조 지원:
- `businesses.business_hours` - 요일별 영업시간
- `reservations.services` - 예약한 서비스 목록
- `customer_histories.details` - 시술 상세 정보

### 2. 인덱스 전략
- Primary Key: BIGSERIAL (자동 증가)
- Foreign Key: 관계 무결성 보장
- 복합 인덱스: 검색 최적화 (business_id + date 등)

### 3. Soft Delete
`deleted_at` 컬럼을 사용한 논리적 삭제 (일부 엔티티)

## API 설계 원칙

### 1. RESTful API
```
GET    /api/users          # 목록 조회
POST   /api/users          # 생성
GET    /api/users/{id}     # 단건 조회
PUT    /api/users/{id}     # 전체 수정
PATCH  /api/users/{id}     # 부분 수정
DELETE /api/users/{id}     # 삭제
```

### 2. 통합 응답 포맷
모든 API는 `ApiResponse<T>` 포맷으로 응답:
```json
// 성공
{
  "success": true,
  "data": { ... },
  "timestamp": "2026-02-08T12:34:56"
}

// 실패
{
  "success": false,
  "error": {
    "code": "U001",
    "message": "사용자를 찾을 수 없습니다",
    "details": { ... }
  },
  "timestamp": "2026-02-08T12:34:56"
}
```

### 3. 페이징 응답
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "pageInfo": {
      "page": 1,
      "size": 20,
      "totalElements": 100,
      "totalPages": 5
    }
  }
}
```

## 보안 구조

### 인증 흐름
```
1. 사용자 로그인 → JWT Access Token + Refresh Token 발급
2. API 요청 시 Authorization 헤더에 Bearer 토큰 포함
3. JwtAuthenticationFilter에서 토큰 검증
4. SecurityContext에 인증 정보 저장
5. Controller에서 @AuthenticationPrincipal로 사용자 정보 접근
```

### 권한 제어
- **Public**: `/api/auth/**`, `/api/health`, Swagger UI
- **Authenticated**: 나머지 모든 API
- **Business-level**: 서비스 로직에서 `user.canAccessBusiness()` 검증

## 개발 환경

### 로컬 실행
```bash
# PostgreSQL 시작
docker-compose up -d

# 애플리케이션 실행
./gradlew bootRun
```

### 접속 정보
- API 서버: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- PostgreSQL: localhost:5432 (DB: moer_dev)

## 다음 문서

- [패키지 구조 상세](./package-structure.md)
- [레이어 아키텍처](./layered-architecture.md)
- [보안 구조](./security.md)
- [예외 처리](./exception-handling.md)
