# moer 예약 시스템 백엔드 — Claude Code 가이드

> 📌 이 파일은 Claude Code 세션 시작 시 자동으로 로드됩니다.
> 신규 진입자/운영자용 일반 정보는 [`README.md`](./README.md), 환경변수는 [`docs/security/ENV-SETUP.md`](./docs/security/ENV-SETUP.md).

---

## 프로젝트 개요

- **목적**: 다업종(미용실, 필라테스, 카페 등) 예약 관리 SaaS 백엔드 API
- **기술**: Spring Boot 4.0.1 · Java 17 · PostgreSQL 16 · MyBatis 4.0.0
- **아키텍처**: Layered + DDD (Controller → Service → Repository)
- **프로필**: `local` (기본) / `prod` — [`docs/security/ENV-SETUP.md`](./docs/security/ENV-SETUP.md)

### 핵심 특징
- JWT 인증 (Access 1h + Refresh 7d, **해시 저장 + Rotation + Replay Detection**)
- PostgreSQL JSONB (영업시간, 서비스 목록 등 유연한 구조)
- MyBatis 동적 쿼리 (`<if>`, `<where>`, `<choose>`, `<bind>`)
- 통합 응답 (`ApiResponse<T>`) + 통합 예외 (`ErrorCode` enum)
- 다층 보안 방어 (Rate Limiting, PII 마스킹, XSS Sanitizer, HTTP 헤더, CHECK 제약)

---

## 디렉터리 구조

```
moer-booking/
├── src/main/
│   ├── java/io/moer/booking/
│   │   ├── common/             # 공통 컴포넌트
│   │   │   ├── config/         # SecurityConfig, SecretsValidator, WebConfig, SwaggerConfig, MyBatisConfig 등
│   │   │   ├── security/       # JwtTokenProvider, CustomUserDetails, OAuth2 핸들러
│   │   │   ├── exception/      # ErrorCode, BusinessException, GlobalExceptionHandler
│   │   │   ├── util/           # MaskingUtils, SqlEscapeUtils, FilenameUtils, HtmlSanitizer, PasswordPolicy
│   │   │   ├── ratelimit/      # RateLimiterService, RateLimitFilter (Bucket4j)
│   │   │   ├── pagination/     # PageSizeCapFilter
│   │   │   ├── storage/        # LocalFileStorageService
│   │   │   ├── mybatis/        # JsonTypeHandler
│   │   │   ├── service/        # EmailService
│   │   │   ├── dto/            # ApiResponse, PageResponse, ErrorInfo
│   │   │   └── controller/     # HealthController 등
│   │   └── domain/             # 도메인별 패키지
│   └── resources/
│       ├── application.yml          # 공통 (git 추적)
│       ├── application-local.yml    # 로컬 (gitignore)
│       ├── application-prod.yml     # 운영 (gitignore)
│       ├── mapper/             # MyBatis XML
│       └── db/
│           ├── schema.sql      # DDL (PG enum 미사용, VARCHAR + CHECK 제약)
│           └── migration/      # 마이그레이션 스크립트 (Vyyyymmdd__*.sql)
├── docker-compose.yml          # PostgreSQL
├── .env                        # docker-compose 환경변수 (gitignore)
└── docs/
    ├── security/               # 보안 감사/계획/체크리스트/인프라/환경변수 가이드
    ├── history/                # 일자별 작업 이력
    ├── agents/                 # Backend agent 정의
    ├── skills/                 # SKILL 문서
    ├── plans/                  # (작업 계획 임시 폴더, 현재 비어있음)
    └── reports/                # (개별 리포트 임시 폴더, 현재 비어있음)
```

> 메모: 이전 버전의 `docs/01_architecture/`, `02_domain/`, `03_database/`, `04_api/`, `05_development/`, `06_deployment/` 폴더는 **삭제되었습니다**. 본 CLAUDE.md 와 `docs/security/`, `docs/history/` 가 현재 유효한 문서입니다.

---

## 도메인 (23개)

```
auditlog · auth · booking · bookmark · broadcast · business · coupon
customer · dashboard · help · holiday · inquiry · notification
notificationlog · payment · reservation · review · service · staff
statistics · subscription · superadmin · user
```

| 카테고리 | 도메인 |
|---------|--------|
| **인증/사용자** | auth, user, superadmin |
| **매장/운영** | business, staff, service, holiday |
| **예약 핵심** | reservation, customer, booking (공개 예약) |
| **CRM** | bookmark, notification, notificationlog, broadcast, review |
| **결제/구독** | payment, subscription, coupon |
| **운영/감사** | dashboard, statistics, auditlog, inquiry, help |

가장 복잡한 도메인: **reservation** (시간 충돌 검증, 자동 고객 생성, 서비스 목록 기반 시간/가격 계산, 상태 전이 제어, 완료 시 CustomerHistory 자동 생성).

---

## 표준 도메인 구조

```
domain/{domain}/
├── {Entity}.java               # 엔티티
├── {Enum}.java                 # 상태/타입 enum
├── controller/
│   └── {Domain}Controller.java # @RestController, @PreAuthorize
├── dto/
│   ├── {Domain}Response.java
│   ├── {Domain}CreateRequest.java
│   ├── {Domain}UpdateRequest.java
│   └── {Domain}SearchCondition.java
├── repository/
│   └── {Domain}Repository.java # @Mapper
└── service/
    └── {Domain}Service.java    # @Transactional
```

대응 위치:
- `src/main/resources/mapper/{domain}/{Domain}Mapper.xml`
- `src/main/java/io/moer/booking/common/exception/ErrorCode.java` (도메인 prefix 추가)

---

## 개발 순서 (새 도메인 추가)

1. **DB 테이블 DDL 작성** → `src/main/resources/db/schema.sql` (운영 DB 마이그레이션은 `db/migration/Vyyyymmdd__*.sql`)
   - VARCHAR enum 컬럼은 **반드시 CHECK 제약** 추가 (P3-1 룰)
2. **Entity 작성** — Lombok `@Getter @Builder @NoArgsConstructor @AllArgsConstructor`, 헬퍼 메서드
3. **DTO 작성**
   - Request: Jakarta Validation (`@NotNull/@Size/@Email/@Pattern`), `@Min(1) @Max(100)` 페이지 사이즈
   - Response: `static from(Entity)` 메서드, 민감 필드 제외
4. **Repository 작성** — `@Mapper` 인터페이스, `Optional<T>` 반환
5. **MyBatis XML 작성** — ResultMap, CRUD, 동적 검색
6. **Service 작성** — `@Transactional(readOnly=true)` 클래스 + 쓰기 메서드 `@Transactional`
7. **Controller 작성** — `@RestController`, `@PreAuthorize`, `ApiResponse<T>` 반환
8. **ErrorCode 추가** — `common/exception/ErrorCode.java`

---

## 핵심 코딩 규칙

### Entity → DTO 변환은 Service에서

```java
@Service
@Transactional(readOnly = true)
public class UserService {
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        User user = User.builder()...build();
        userRepository.save(user);
        return UserResponse.from(user);
    }
}
```

### 예외 처리

```java
// 엔티티 미존재
User user = userRepository.findById(id)
    .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));

// 비즈니스 규칙 위반
if (hasConflict) {
    throw new BusinessException(ErrorCode.RESERVATION_TIME_CONFLICT, "이미 예약된 시간입니다");
}
```

### 권한 검증 (P1-4 룰)

```java
// ❌ 금지: 반환값 무시 위험
userDetails.getUser().canAccessBusiness(businessId);

// ✅ 권장: 위반 시 AccessDeniedException 던짐
userDetails.getUser().requireAccessBusiness(businessId);

// 컨트롤러 메서드 단: 어노테이션 기반 (가능한 경우)
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@PreAuthorize("#userId == principal.userId or hasAnyRole('ADMIN','SUPER_ADMIN')")
```

### MyBatis Enum 매핑 ⚠️ 중요

스키마는 **PG 커스텀 enum 미사용 / VARCHAR + CHECK 제약** 사용 (`schema.sql:5`).
`application.yml` 의 `default-enum-type-handler: org.apache.ibatis.type.EnumTypeHandler` 가 변환을 자동 처리.

```xml
<!-- ✅ 올바른 매핑: 단순 result, 타입핸들러 명시 X -->
<resultMap id="userResultMap" type="User">
    <result property="role" column="role"/>
    <result property="status" column="status"/>
</resultMap>

<!-- ❌ 금지: <result> 에 typeHandler 명시 시 mybatis-spring 4.0.0 + mybatis 3.5.19 버그 발생
     (다른 enum 클래스에 핸들러가 잘못 캐시됨, UserMapper/ReservationMapper 가 이미 검증된 패턴) -->
<result property="role" column="role" typeHandler="org.apache.ibatis.type.EnumTypeHandler"/>
```

### MyBatis 동적 SQL — SQL Injection 방지 (P0-2 룰)

```xml
<!-- ❌ 금지: ${} 문자열 치환 -->
SET ${column} = #{value}
INTERVAL '${monthsAgo} months'

<!-- ✅ 권장: 화이트리스트 분기 -->
SET
<choose>
    <when test='column == "onboarding_step_service"'>onboarding_step_service</when>
    <when test='column == "onboarding_step_staff"'>onboarding_step_staff</when>
    <otherwise>invalid_column_blocked_by_whitelist</otherwise>
</choose>
= #{value}

<!-- ✅ INTERVAL: PG 안전 함수 + 파라미터 바인딩 -->
CURRENT_DATE - MAKE_INTERVAL(months => #{monthsAgo})
```

### LIKE 검색 — 와일드카드 이스케이프 (P1-6 룰)

```xml
<if test="keyword != null and keyword != ''">
    <bind name="kwEscaped" value="@io.moer.booking.common.util.SqlEscapeUtils@escapeLike(keyword)"/>
    AND b.name LIKE '%' || #{kwEscaped} || '%' ESCAPE '\'
</if>
```

### JSONB 타입

```xml
<insert id="save">
    INSERT INTO reservations (services)
    VALUES (#{services, typeHandler=io.moer.booking.common.mybatis.JsonTypeHandler}::jsonb)
</insert>

<resultMap id="reservationResultMap" type="Reservation">
    <result property="services" column="services"
            typeHandler="io.moer.booking.common.mybatis.JsonTypeHandler"/>
</resultMap>
```

### 사용자 입력 텍스트 저장 (P1-5 룰)

리뷰 content, 문의 content, 고객 노트 등 사용자 입력 텍스트는 저장 직전 sanitize:

```java
.content(HtmlSanitizer.plainText(request.getContent()))  // 태그 전제거
// 또는 제한된 서식 허용 (관리자 작성용)
.content(HtmlSanitizer.safeRichText(request.getContent()))
```

### PII 로깅 (P1-7 룰)

```java
log.info("Customer created: name={}, phone={}, email={}",
    MaskingUtils.maskName(customer.getName()),
    MaskingUtils.maskPhone(customer.getPhone()),
    MaskingUtils.maskEmail(customer.getEmail()));
```

### 비밀번호 정책 (P3-7 룰)

```java
// register / changePassword / resetPassword 시 호출
PasswordPolicy.validate(rawPassword, email);  // 10자+, 블랙리스트, 이메일 포함 차단
```

### 페이징

```java
int offset = (page - 1) * size;
List<User> users = userRepository.findByCondition(condition, offset, size);
int totalElements = userRepository.countByCondition(condition);
PageInfo pageInfo = PageInfo.of(page, size, totalElements);
return new PageResponse<>(content, pageInfo);
// 글로벌 size 캡(≤100)은 PageSizeCapFilter 가 처리 — 컨트롤러 개별 @Max 불필요
```

### API 응답

```java
// 성공
return ResponseEntity.ok(ApiResponse.success(response));

// 생성
return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));

// 데이터 없음
return ResponseEntity.ok(ApiResponse.success());
```

### 트랜잭션

```java
@Service
@Transactional(readOnly = true)
public class UserService {
    @Transactional
    public UserResponse createUser(...) { ... }
}
```

---

## 코드 생성 체크리스트

- [ ] **Entity**: Lombok `@Getter @Builder`, 헬퍼 메서드 (canX, isX)
- [ ] **DTO Request**: Jakarta Validation (`@Valid`, `@NotNull`, `@Size`, `@Email`, `@Pattern`)
- [ ] **DTO Response**: `static from(Entity)`, 민감 필드(password 등) 제외
- [ ] **Repository**: `@Mapper`, `Optional<T>` 반환
- [ ] **MyBatis XML**:
  - `<result>` 에 typeHandler 미명시 (JsonTypeHandler 제외)
  - `${}` 문자열 치환 미사용
  - LIKE 검색: `<bind>` + `SqlEscapeUtils` + `ESCAPE '\'`
  - VARCHAR enum 컬럼은 `application.yml` 의 default-enum-type-handler 가 처리
- [ ] **Service**: `@Transactional` 명시, 비즈니스 규칙 검증, `BusinessException` 던짐
- [ ] **Controller**: `@RestController`, `@PreAuthorize` (또는 `requireAccessBusiness()`), `@Valid`, `ApiResponse<T>`
- [ ] **ErrorCode**: 새 도메인 prefix 추가
- [ ] **사용자 텍스트**: `HtmlSanitizer.plainText/safeRichText` 적용
- [ ] **PII 로그**: `MaskingUtils.maskPhone/Email/Name` 적용
- [ ] **감사 로그**: 권한/삭제/상태 변경 등 중요 액션은 `auditLogService.log(...)` 호출
- [ ] **DB DDL**: VARCHAR enum 컬럼에 CHECK 제약 추가

---

## 자주 쓰는 명령

```bash
# 빠른 시작
docker-compose up -d           # PostgreSQL (.env 자동 로드)
./gradlew bootRun              # SPRING_PROFILES_ACTIVE 기본값 = local

# 컴파일
./gradlew compileJava compileTestJava

# 운영 프로필 부팅 (환경변수 필요)
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun

# 의존성 취약점 스캔 (OWASP)
./gradlew dependencyCheckAnalyze
```

**API 문서**: `http://localhost:8080/swagger-ui.html` (**local 프로필 한정** — prod에서는 denyAll + springdoc.enabled=false)

---

## 문제 발생 시

| 증상 | 해결 |
|------|------|
| `IllegalStateException: 필수 시크릿/설정 누락` | `application-local.yml` 의 `[LEAKED-SECRET]` 항목 또는 환경변수 확인 |
| PostgreSQL 연결 실패 | `docker-compose restart chatbot-db` |
| Lombok 미작동 | IntelliJ Annotation Processing 활성화 |
| 포트 충돌 | `application.yml` 포트 변경 또는 점유 프로세스 종료 |
| `429 TOO_MANY_REQUESTS` | Rate Limit 정상 동작 (로그인 5회/15분 등) |
| SQL 디버깅 | `application-local.yml` 에 `io.moer.booking.domain: TRACE` 추가 |

---

## 🛡️ 보안 작업 이력 (2026-05-12)

### 진행 결과 요약

- **출발점**: 5개 영역 병렬 정적 분석 → 30건 발견 (Critical 7 / High 11 / Medium 8 / Low 4)
- **완료**: P0~P3 30건 + 추가 비기능 강화 모두 코드 반영
- **빌드**: `./gradlew compileJava compileTestJava` 통과
- **실행**: `./gradlew bootRun` 정상 부팅 확인 (local 프로필)

상세: [`docs/history/2026-05-12.md`](./docs/history/2026-05-12.md), [`docs/security/audit-2026-05-12.md`](./docs/security/audit-2026-05-12.md)

### 신규 코드 컴포넌트

`src/main/java/io/moer/booking/common/`:
- `config/SecretsValidator.java` — 부팅 시 필수 시크릿 검증 (Fail Fast)
- `util/MaskingUtils.java` — PII 마스킹 (phone/email/name/token)
- `util/SqlEscapeUtils.java` — LIKE 와일드카드 이스케이프
- `util/FilenameUtils.java` — 파일명 sanitize (Path Traversal 방어)
- `util/HtmlSanitizer.java` — OWASP Java HTML Sanitizer wrapper (XSS)
- `util/PasswordPolicy.java` — NIST 800-63B 비밀번호 정책
- `ratelimit/RateLimiterService.java` — Bucket4j 인메모리 토큰 버킷
- `ratelimit/RateLimitFilter.java` — IP 기반 Rate Limiting Filter (ObjectMapper 자체 인스턴스)
- `pagination/PageSizeCapFilter.java` — 글로벌 `size` ≤ 100 캡

DB 마이그레이션:
- `src/main/resources/db/migration/V20260512__security_enum_check_constraints.sql`

보안 문서:
- `docs/security/audit-2026-05-12.md` · `improvement-plan.md` · `launch-checklist.md` · `infrastructure-security.md` · `ENV-SETUP.md` · `README.md`

### 핵심 변경 사항 (P0~P3 통합)

| 영역 | 변경 |
|------|------|
| **시크릿** | application.yml 평문 제거. `application-local.yml`(gitignore) 분리. `SecretsValidator` 부팅 검증 |
| **인증** | UserController 전체 `@PreAuthorize`. `User.requireAccessBusiness()` enforce 메서드, 25개 broken caller 일괄 교체 |
| **Refresh Token** | `refresh_tokens.token` → `token_hash` (BCrypt). userId 기반 조회 + matches() 검증. Rotation + Replay Detection |
| **SQL Injection** | `${column}` → 화이트리스트 `<choose>`. `INTERVAL '${monthsAgo}'` → `MAKE_INTERVAL` |
| **Path Traversal** | `LocalFileStorageService` `normalize() + startsWith(uploadRoot)` + subDir 화이트리스트 |
| **HTTP 헤더** | HSTS, X-Frame-Options DENY, nosniff, Referrer-Policy, **CSP (prod strict / dev unsafe-inline)**, Permissions-Policy |
| **Swagger** | prod 프로필 `denyAll()` + `springdoc.enabled=false`. 테스트 계정 문구 제거 |
| **Rate Limit** | Bucket4j. 로그인 5회/15분 + 계정 잠금, 비밀번호 재설정 3회/시간, Public 60회/분 |
| **CORS** | `allowedHeaders` 화이트리스트, WebConfig CORS 제거 → SecurityConfig 단일 소스 |
| **XSS** | 리뷰/문의/고객노트 content 저장 시 `HtmlSanitizer.plainText()` |
| **LIKE** | 6개 mapper에 `<bind>` + `ESCAPE '\'` |
| **PII 로그** | Inquiry/Customer/PublicBooking/Email/Auth Service 마스킹 |
| **파일명** | ReviewService `originalFilename` 저장 전 `FilenameUtils.sanitize()` |
| **결제** | `PaymentResponse.pgTransactionIdMasked` 필드 추가 |
| **BCrypt** | strength 10 → **12** |
| **Pagination** | `PageSizeCapFilter` 글로벌 캡 |
| **로깅** | application.yml 기본 INFO. local DEBUG, prod WARN |
| **Actuator** | health/info만, 나머지 ADMIN+ |
| **의존성** | OWASP `dependencycheck` 11.1.1, CVSS 7.0+ 빌드 실패 |
| **CHECK 제약** | `users.role/status/Y-N` 등, 마이그레이션 스크립트로 나머지 enum 컬럼 |
| **Validation 응답** | prod에서 필드명 미노출 |
| **비밀번호 강제 변경** | `users.password_change_required` + `LoginResponse.passwordChangeRequired` |
| **감사 로그** | StaffService.deleteStaff 추가 + STAFF_* AuditAction |
| **비밀번호 정책** | `PasswordPolicy` 10자+, 블랙리스트, 이메일 포함 차단 |

### ⚠️ 후속 작업 (코드 외 / 운영자 액션)

1. **노출 시크릿 회수** — `application-local.yml` 의 `[LEAKED-SECRET / TODO-ROTATE]` 항목 6개:
   - DB 비밀번호, SMTP 비밀번호, Google/Naver/Kakao OAuth client_secret, JWT 시크릿
   - 각 콘솔에서 회전 후 `application-local.yml` / 운영 환경변수에 새 값 반영
2. **DB 마이그레이션** — 기존 DB 환경:
   - `users.password_change_required` 컬럼 추가
   - `refresh_tokens.token` → `token_hash` 변경 (기존 토큰 무효화 → 전체 재로그인 필요)
   - `db/migration/V20260512__security_enum_check_constraints.sql` 실행 전 enum 정합성 검증
3. **git 이력 정리** (옵션) — BFG / `git filter-repo`. 1번 회전 완료 시 생략 가능
4. **인프라 보안** — [`docs/security/infrastructure-security.md`](./docs/security/infrastructure-security.md): CloudFlare/AWS WAF, TLS 강제, 시크릿 매니저, 멀티 인스턴스 시 `RateLimiterService` Redis 교체
5. **HIBP 연동** — `PasswordPolicy` 블랙리스트를 Have I Been Pwned API 연동으로 강화
6. **회귀 테스트 작성** — 권한, Rate Limit, Refresh Token Rotation 통합 테스트
7. **SwaggerConfig 조건부 Bean** — prod 에서 `springdoc.enabled=false` 이지만 `OpenAPI` Bean 자체는 생성됨. `@ConditionalOnProperty(name="springdoc.api-docs.enabled", havingValue="true", matchIfMissing=true)` 추가 검토

### 후속 세션 시 시작점

1. **현재 상태 확인**: [`docs/history/2026-05-12.md`](./docs/history/2026-05-12.md) 전문 읽기
2. **부팅 검증**: `docker-compose up -d && ./gradlew bootRun` → `Started MoerBookingApplication` 확인
3. **남은 작업**: 위 후속 작업 1~7번 중 선택
4. **변경 의도 파악**: 코드의 `// SECURITY (P*-*)` 코멘트가 어떤 항목인지 표시

### 세션 간 컨텍스트 유지 주의사항

- `application-local.yml`, `application-prod.yml`, `.env` 는 **gitignore**. 신규 클론 시 [`docs/security/ENV-SETUP.md`](./docs/security/ENV-SETUP.md) 보고 재생성
- `RateLimitFilter` 는 ObjectMapper 자체 인스턴스 사용 (Spring Boot 4 starter 구조 호환). 변경 시 주의
- **신규 코드 작성 시**:
  - 권한: `requireAccessBusiness()` 사용 (boolean 무시 위험 회피)
  - MyBatis: `${}` 문자열 치환 절대 금지
  - LIKE: `<bind>` + `SqlEscapeUtils.escapeLike()` + `ESCAPE '\'`
  - 사용자 텍스트: `HtmlSanitizer.plainText()` 저장
  - PII 로그: `MaskingUtils.*` 사용
  - 비밀번호: `PasswordPolicy.validate()` 호출
  - DDL: VARCHAR enum 컬럼에 CHECK 제약
  - DTO `<result>` 에 EnumTypeHandler 명시 금지 (default-enum-type-handler가 처리)

---

## 🤖 에이전트 활용

복잡한 작업은 에이전트를 적극 활용:
- 코드 탐색 → **Explore**
- 구현 계획 → **Plan**
- 코드 생성 → **Backend Code Generator**
- 품질 검증 → **Backend QA Engineer**
- 프로젝트 분석 → **Backend Project Analyzer**
- 아키텍처 검토 → **Backend Senior Planner**
- 독립 작업은 병렬로 여러 에이전트 동시 실행

에이전트 정의: [`docs/agents/`](./docs/agents/) — backend-code-generator, backend-project-analyzer, backend-qa-engineer, backend-senior-planner

---

## 🤝 협업 모드

**FE-BE 협업 모드** (메모리 등록): 백엔드 디렉터리에서는 디자인 작업 시작 안 함. FE에서 요청이 넘어오면 처리.
