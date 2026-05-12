# moer 예약 시스템 백엔드

다업종(미용실, 필라테스, 카페 등) 예약 관리 SaaS의 백엔드 API.

- **기술**: Spring Boot 4.0.1 · Java 17 · PostgreSQL 16 · MyBatis 4.0.0
- **아키텍처**: Layered + DDD (Controller → Service → Repository)
- **인증**: JWT (Access 1h + Refresh 7d, 해시 저장 + Rotation)
- **API 문서**: Swagger UI `http://localhost:8080/swagger-ui.html` (local 프로필 한정)

---

## 빠른 시작

```bash
# 1) PostgreSQL 컨테이너 실행 (.env 자동 로드)
docker-compose up -d

# 2) 애플리케이션 부팅 (SPRING_PROFILES_ACTIVE 기본값 = local)
./gradlew bootRun
```

부팅 성공 시 로그:
```
The following 1 profile is active: "local"
[SecretsValidator] 시크릿 검증 완료 (profile=local)
Tomcat started on port 8080
Started MoerBookingApplication in 25.xxx seconds
```

> ⚠️ 최초 클론 시 `application-local.yml`, `application-prod.yml`, `.env` 는 `.gitignore` 에 등록되어 있어 존재하지 않습니다.
> 운영자에게 받거나 [`docs/security/ENV-SETUP.md`](./docs/security/ENV-SETUP.md) 참고하여 생성하세요.

---

## 프로필 구조

| 파일 | 활성 조건 | 용도 | 추적 |
|------|----------|------|------|
| `application.yml` | 항상 | 공통 설정, 비밀 없음 | git ✓ |
| `application-local.yml` | `SPRING_PROFILES_ACTIVE=local` (기본) | 로컬 개발, DEBUG 로깅, Swagger 허용 | gitignore |
| `application-prod.yml` | `SPRING_PROFILES_ACTIVE=prod` | 운영, 시크릿 환경변수 강제, Swagger OFF | gitignore |
| `.env` | docker-compose 자동 로드 | PostgreSQL 컨테이너 설정 | gitignore |

부팅 시 `SecretsValidator` 가 `JWT_SECRET` 32바이트 이상 / `DB_PASSWORD` 등을 검증, 누락 시 `IllegalStateException` 으로 부팅 차단.

---

## 보안 아키텍처

- **인증/인가**: JWT + Spring Security `@PreAuthorize`
  - Refresh Token: BCrypt 해시 저장 + Rotation + Replay Detection
  - BCrypt strength **12**
- **레이트 제한** (Bucket4j 인메모리): 로그인 5회/15분, 비밀번호 재설정 3회/시간, Public API 60회/분
- **HTTP 보안 헤더**: HSTS, X-Frame-Options DENY, CSP (prod strict / dev unsafe-inline), Referrer-Policy, Permissions-Policy
- **입력 정화**: OWASP Java HTML Sanitizer (XSS), SQL LIKE 와일드카드 이스케이프, Path Traversal 차단
- **페이지 사이즈 글로벌 캡**: Servlet Filter 로 모든 `size` 파라미터 ≤ 100
- **PII 마스킹**: 로그 출력 시 전화번호/이메일/이름 자동 마스킹
- **감사 로그**: 권한 변경 / 삭제 / 결제 등 중요 액션 자동 기록
- **비밀번호 정책**: NIST 800-63B 기반 (10자 이상, 흔한 패턴 차단)
- **CORS**: 화이트리스트 헤더, 운영 도메인만 origin 허용

상세: [`docs/security/`](./docs/security/) — 감사 보고서, 개선 계획, 런칭 체크리스트, 인프라 가이드

---

## 도메인 (완료된 11개)

| 도메인 | 주요 기능 |
|--------|----------|
| **auth** | 로그인/회원가입/토큰 갱신, OAuth2 (Google/Naver/Kakao) |
| **user** | 사용자 CRUD, 역할(SUPER_ADMIN/ADMIN/OWNER/STAFF/CUSTOMER) |
| **business** | 매장 정보·설정·영업시간(JSONB), Public 매장 검색 |
| **staff** | 직원 CRUD, 포트폴리오, 스케줄 |
| **service** | 서비스 메뉴 CRUD |
| **customer** | 고객 CRUD, 이력, 노트, 북마크 |
| **reservation** | 예약 생성/조회/상태 관리, 시간 충돌 검증 |
| **holiday** | 특별 휴무일 |
| **dashboard** | 오늘/주간/월간 통계, 매출/예약 분석 |
| **auditlog** | 감사 로그 |
| **superadmin** | 전체 시스템 관리, 매장/사용자 통계, 매출 랭킹 |

기타: payment, coupon, broadcast, inquiry, help, review, notification, subscription, bookmark

---

## 문서 인덱스

### 운영/보안
- [`docs/security/README.md`](./docs/security/README.md) — 보안 문서 인덱스
- [`docs/security/audit-2026-05-12.md`](./docs/security/audit-2026-05-12.md) — 종합 보안 감사 (Critical 7 / High 11 / Medium 8 / Low 4)
- [`docs/security/improvement-plan.md`](./docs/security/improvement-plan.md) — P0~P3 개선 로드맵 (전 항목 완료)
- [`docs/security/launch-checklist.md`](./docs/security/launch-checklist.md) — 운영 배포 전 체크리스트
- [`docs/security/infrastructure-security.md`](./docs/security/infrastructure-security.md) — WAF/DDoS/Edge 보안
- [`docs/security/ENV-SETUP.md`](./docs/security/ENV-SETUP.md) — 환경변수/프로필 가이드

### 작업 이력
- [`docs/history/`](./docs/history/) — 일자별 작업 기록 (2026-05-12 보안 점검 + P0~P3 구현)

### 개발 가이드
- [`CLAUDE.md`](./CLAUDE.md) — 코드 패턴, 도메인 규칙, 현재 작업 상태
- [`docs/02_domain/development-pattern.md`](./docs/02_domain/development-pattern.md) — 새 도메인 추가
- [`docs/02_domain/reservation.md`](./docs/02_domain/reservation.md) — 복잡한 도메인 예시
- [`docs/04_api/`](./docs/04_api/) — REST API 명세
- [`docs/05_development/`](./docs/05_development/) — 개발 환경, 컨벤션

### 슈퍼 관리자
- [`docs/superadmin_summary.md`](./docs/superadmin_summary.md)
- [`docs/superadmin_frontend_guide.md`](./docs/superadmin_frontend_guide.md)

---

## 디렉터리 구조

```
moer-booking/
├── src/main/
│   ├── java/io/moer/booking/
│   │   ├── common/             # 공통 (security/config/util/exception/storage/pagination/ratelimit)
│   │   └── domain/             # 도메인별 패키지 (entity/dto/repository/service/controller)
│   └── resources/
│       ├── application.yml     # 공통
│       ├── application-local.yml   # 로컬 (gitignore)
│       ├── application-prod.yml    # 운영 (gitignore)
│       ├── mapper/             # MyBatis XML
│       └── db/
│           ├── schema.sql      # DDL
│           └── migration/      # 마이그레이션 스크립트
├── docker-compose.yml          # PostgreSQL 컨테이너
├── docs/
│   ├── security/               # 보안 문서
│   ├── history/                # 작업 이력
│   └── ...                     # 도메인/아키텍처/API 문서
└── .env                        # docker-compose 환경변수 (gitignore)
```

---

## 자주 쓰는 명령

```bash
# 컴파일
./gradlew compileJava

# 부팅 (local 프로필 기본)
./gradlew bootRun

# 운영 프로필로 부팅 (환경변수 필요)
SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun

# 의존성 취약점 스캔 (OWASP Dependency-Check)
./gradlew dependencyCheckAnalyze

# DB 컨테이너 재시작
docker-compose restart chatbot-db
```

---

## 트러블슈팅

| 증상 | 해결 |
|------|------|
| `IllegalStateException: 필수 시크릿/설정이 누락` | `application-local.yml` 의 `[LEAKED-SECRET]` 항목 확인 또는 환경변수 설정 |
| `BUILD FAILED: rateLimitFilter ... ObjectMapper` | (해결됨) `RateLimitFilter` 가 자체 ObjectMapper 사용. 최신 코드 pull |
| PostgreSQL healthcheck 실패 | `.env` 의 `POSTGRES_USER/PASSWORD` 확인 |
| `429 TOO_MANY_REQUESTS` | Rate Limit 정상 동작. 로그인 5회/15분, 비밀번호 재설정 3회/시간 |
| Lombok 미작동 | IntelliJ Settings → Annotation Processing 활성화 |

---

## 라이선스

내부 프로젝트 (비공개).
