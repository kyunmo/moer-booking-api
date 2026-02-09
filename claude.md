# moer 예약 시스템 백엔드 - Claude Code 가이드

## 프로젝트 개요

**목적**: 다업종(미용실, 필라테스, 카페 등) 예약 관리 시스템 백엔드 API
**기술**: Spring Boot 4.0.1, Java 17, PostgreSQL 16, MyBatis 4.0.0
**아키텍처**: Layered Architecture + DDD (Domain-Driven Design)

### 핵심 특징
- JWT 기반 인증 (Access Token 1시간 + Refresh Token 7일)
- PostgreSQL JSONB 활용 (유연한 데이터 구조)
- MyBatis 동적 쿼리 (복잡한 검색 조건)
- 통합 API 응답 (`ApiResponse<T>`)
- 계층화된 예외 처리 (`ErrorCode` Enum)

## 📚 문서 구조

### ⭐ 필수 참조 문서

1. **[도메인 개발 패턴](./docs/02_domain/development-pattern.md)** - 새 도메인 추가 시 필수
2. **[Reservation 도메인](./docs/02_domain/reservation.md)** - 복잡한 비즈니스 로직 예시
3. **[예외 처리](./docs/01_architecture/exception-handling.md)** - ErrorCode 사용법

### 전체 문서

#### [01. 아키텍처](./docs/01_architecture/README.md)
- [패키지 구조](./docs/01_architecture/package-structure.md) - 도메인별 패키지 구조
- [레이어 아키텍처](./docs/01_architecture/layered-architecture.md) - Controller→Service→Repository 흐름
- ⭐ [보안 구조](./docs/01_architecture/security.md) - JWT 인증 흐름
- ⭐ [예외 처리](./docs/01_architecture/exception-handling.md) - ErrorCode 체계

#### [02. 도메인](./docs/02_domain/README.md)
- ⭐ [도메인 개발 패턴](./docs/02_domain/development-pattern.md) - 새 도메인 추가 가이드
- ⭐ [Reservation 도메인](./docs/02_domain/reservation.md) - 가장 복잡한 도메인 예시

#### [03. 데이터베이스](./docs/03_database/README.md)
- 테이블 스키마 (11개 테이블)
- MyBatis XML 매핑 규칙
- JSONB 타입 핸들러 (`JsonTypeHandler`)
- 쿼리 패턴 (시간 충돌, 집계, 조인)

#### [04. API](./docs/04_api/README.md)
- 모든 REST API 엔드포인트
- 요청/응답 예시
- 인증 방식 (JWT Bearer Token)
- 에러 코드 목록

#### [05. 개발](./docs/05_development/README.md)
- 개발 환경 설정
- 코딩 컨벤션
- Git 워크플로우
- 디버깅 및 문제 해결

#### [06. 배포](./docs/06_deployment/README.md)
- Docker 배포
- 프로덕션 설정
- 환경 변수 관리

## 🚀 표준 개발 패턴

### 도메인 구조 (9개 도메인)

```
domain/{domain}/
├── {Entity}.java           # 엔티티
├── {Enum}.java             # 열거형 (Status, Type 등)
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

### 개발 순서

1. **DB 테이블 생성** (`src/main/resources/db/schema.sql`)
2. **Entity 작성** - Lombok `@Getter`, `@Builder`, 헬퍼 메서드
3. **DTO 작성** - Request(Validation), Response(from 메서드), SearchCondition
4. **Repository 작성** - MyBatis `@Mapper` 인터페이스
5. **MyBatis XML 작성** (`src/main/resources/mapper/`)
   - ResultMap 정의
   - CRUD 쿼리
   - 동적 검색 쿼리 (`<if>`, `<where>`)
6. **Service 작성** - `@Transactional`, 비즈니스 로직, 예외 처리
7. **Controller 작성** - `@RestController`, `ApiResponse<T>` 반환

### 핵심 규칙

#### Entity → DTO 변환 (Service에서)
```java
// Service
public UserResponse createUser(UserCreateRequest request) {
    User user = User.builder()...build();
    userRepository.save(user);
    return UserResponse.from(user);  // Entity → DTO
}
```

#### 예외 처리
```java
// 엔티티 미존재
User user = userRepository.findById(id)
    .orElseThrow(() -> new EntityNotFoundException(
        ErrorCode.USER_NOT_FOUND,
        "사용자를 찾을 수 없습니다: " + id
    ));

// 비즈니스 규칙 위반
if (hasConflict) {
    throw new BusinessException(
        ErrorCode.RESERVATION_TIME_CONFLICT,
        "이미 예약된 시간입니다"
    );
}
```

#### MyBatis XML (PostgreSQL Enum)
```xml
<!-- Enum 저장 -->
<insert id="save">
    INSERT INTO users (role, status)
    VALUES (#{role}::user_role, #{status}::user_status)
</insert>

<!-- Enum 조회 -->
<resultMap id="userResultMap" type="User">
    <result property="role" column="role"
            typeHandler="org.apache.ibatis.type.EnumTypeHandler"/>
</resultMap>
```

#### JSONB 타입
```xml
<!-- JSONB 저장 -->
<insert id="save">
    INSERT INTO reservations (services)
    VALUES (#{services, typeHandler=io.moer.booking.common.mybatis.JsonTypeHandler}::jsonb)
</insert>

<!-- JSONB 조회 -->
<resultMap id="reservationResultMap" type="Reservation">
    <result property="services" column="services"
            typeHandler="io.moer.booking.common.mybatis.JsonTypeHandler"/>
</resultMap>
```

## 📌 현재 작업 상태

### 완료된 도메인

| 도메인 | 상태 | 주요 기능 |
|--------|------|----------|
| **auth** | ✅ 완료 | 로그인, 회원가입, 토큰 갱신 |
| **user** | ✅ 완료 | 사용자 CRUD, 역할 관리 (SUPER_ADMIN/ADMIN/OWNER/STAFF) |
| **business** | ✅ 완료 | 매장 정보, 설정 관리, 영업시간(JSONB), 권한 보안 강화 |
| **staff** | ✅ 완료 | 직원 CRUD, 포트폴리오 관리 |
| **service** | ✅ 완료 | 서비스 메뉴 CRUD |
| **customer** | ✅ 완료 | 고객 CRUD, 이력 관리 |
| **reservation** | ✅ 완료 | 예약 생성/조회/상태 관리, 시간 충돌 검증 |
| **holiday** | ✅ 완료 | 특별 휴무일 관리 |
| **dashboard** | ✅ 완료 | 오늘/주간/월간 통계 |
| **auditlog** | ✅ 완료 | 감사 로그, 중요 액션 자동 기록 |
| **superadmin** | ✅ 완료 | 슈퍼 관리자 전용 기능 (시스템 관리) |

### Reservation 도메인 (가장 복잡)

**복잡한 비즈니스 로직**:
- 고객 자동 생성 (customerId 없을 시)
- 서비스 목록 기반 총 시간/가격 자동 계산
- 과거 날짜, 휴무일, 시간 충돌 검증
- 상태 전이 제어 (PENDING → CONFIRMED → COMPLETED)
- 예약 완료 시 자동으로 CustomerHistory 생성

**참고**: [Reservation 도메인 상세 문서](./docs/02_domain/reservation.md)

### 슈퍼 관리자 (SUPER_ADMIN) ⭐ 신규 추가

**전체 시스템 관리 기능**:
- **시스템 대시보드**: 전체 매장/사용자 통계, 매출 랭킹, 업종별 분석
- **매장 관리**: 전체 매장 조회/검색/삭제/상태 일괄 변경
- **사용자 관리**: 전체 사용자 조회, 역할 변경, 정지, 삭제
- **감사 로그**: 중요 액션 이력 조회 (삭제, 역할 변경 등)

**보안 강화**:
- Business 도메인 전체 메서드에 권한 체크 추가
- SUPER_ADMIN 회원가입 차단
- SUPER_ADMIN 계정 삭제/정지 차단

**초기 계정**:
```
Email: superadmin@moer.io
Password: Admin123!
```

**📖 상세 문서**:
- **[슈퍼 관리자 요약](./docs/superadmin_summary.md)** - 빠른 개요 및 API 명세
- **[화면 구현 가이드](./docs/superadmin_frontend_guide.md)** - 프론트엔드 개발자용 완전 가이드 (130+ 페이지)

## 💡 코드 생성 시 체크리스트

- [ ] **Entity**: Lombok `@Getter`, `@Builder`, 헬퍼 메서드
- [ ] **DTO**: Request(Validation), Response(`from()` 메서드)
- [ ] **Repository**: `@Mapper`, `Optional<T>` 반환
- [ ] **MyBatis XML**: PostgreSQL Enum 캐스팅 (`::enum_type`)
- [ ] **Service**: `@Transactional`, 비즈니스 규칙 검증, 예외 처리
- [ ] **Controller**: `@RestController`, `ApiResponse<T>` 반환, `@Valid`
- [ ] **ErrorCode**: 새 도메인 에러 코드 추가 (예: `PR001~PR099`)
- [ ] **권한 체크**: `user.canAccessBusiness()` 등

## 🔧 자주 사용하는 코드 패턴

### API 응답
```java
// 성공 (데이터 있음)
return ResponseEntity.ok(ApiResponse.success(response));

// 성공 (데이터 없음)
return ResponseEntity.ok(ApiResponse.success());

// 생성
return ResponseEntity.status(HttpStatus.CREATED)
    .body(ApiResponse.success(response));
```

### 페이징
```java
int offset = (page - 1) * size;
List<User> users = userRepository.findByCondition(condition, offset, size);
int totalElements = userRepository.countByCondition(condition);
PageInfo pageInfo = PageInfo.of(page, size, totalElements);
return new PageResponse<>(content, pageInfo);
```

### 트랜잭션
```java
@Service
@Transactional(readOnly = true)  // 클래스 레벨: 읽기 전용
public class UserService {

    @Transactional  // 메서드 레벨: 쓰기
    public UserResponse createUser(...) {
    }
}
```

## 📞 문제 발생 시

1. **PostgreSQL 연결 실패**: `docker-compose restart postgres`
2. **Lombok 미작동**: Annotation Processing 활성화 확인
3. **포트 충돌**: `application.yml`에서 포트 변경 또는 프로세스 종료
4. **SQL 디버깅**: `mybatis.configuration.log-impl=org.apache.ibatis.logging.stdout.StdOutImpl`

## 🎯 다음 작업 제안

새 기능 추가 시:
1. [도메인 개발 패턴](./docs/02_domain/development-pattern.md) 참고
2. 복잡한 비즈니스 로직은 [Reservation 도메인](./docs/02_domain/reservation.md) 참고
3. ErrorCode 추가 (`common/exception/ErrorCode.java`)
4. Swagger UI로 테스트 (http://localhost:8080/swagger-ui.html)

---

**빠른 시작**: `docker-compose up -d && ./gradlew bootRun`
**API 문서**: http://localhost:8080/swagger-ui.html
**상세 문서**: [docs/README.md](./docs/README.md)
