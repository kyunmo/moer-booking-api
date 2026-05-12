# moer-booking 보안 개선 계획

- **작성일**: 2026-05-12
- **기반**: [audit-2026-05-12.md](./audit-2026-05-12.md)
- **목표**: 서비스 런칭 전 Critical/High 0건, Medium 50% 이상 해소

## 우선순위 정의

| 우선순위 | 의미 | 처리 시점 |
|---------|------|----------|
| **P0** | 런칭 차단(Blocker) — 노출 시 즉시 사고 | 런칭 전 반드시 |
| **P1** | 런칭 차단 — 출시 후 즉시 표적 가능 | 런칭 전 반드시 |
| **P2** | 권고 — 출시 후 1개월 내 처리 | 런칭 직후 |
| **P3** | 백로그 — 운영 안정화 후 처리 | 분기 단위 |

---

## P0 — 런칭 절대 차단 (예상 5~7일)

### P0-1. 시크릿 전면 회수 및 환경변수화 (C-01)
- **작업 내용**:
  1. `.gitignore`에 `src/main/resources/application.yml` 추가
  2. `application.yml`을 `application-example.yml`로 복사하고 모든 시크릿을 빈 문자열로 교체 후 커밋
  3. 실제 `application.yml`은 `.gitignore` 적용 후 로컬에서만 유지
  4. `application-prod.yml`은 모든 값이 `${ENV_VAR}` 형태이며 기본값 미제공 (미설정 시 부팅 실패)
  5. **git 이력 정리**: BFG Repo-Cleaner 또는 `git filter-repo`로 노출 시크릿 제거
  6. **노출된 시크릿 전면 교체**:
     - DB 비밀번호 재발급
     - Google/Naver/Kakao OAuth Client Secret 콘솔에서 회전
     - SMTP 비밀번호 재발급
     - JWT 시크릿 32바이트 랜덤 재생성 (`openssl rand -base64 48`)
  7. CI/CD 또는 부팅 시 필수 환경변수 검증 (`@PostConstruct`에서 누락 시 예외)
- **예상**: 2일 (이력 정리 + 외부 콘솔 작업 포함)
- **검증 방법**: `git log -p -- application.yml | grep -i "secret\|password"` 결과 0건

### P0-2. SQL Injection 제거 (C-02, C-03)
- **작업 내용**:
  - `BusinessSettingsMapper.xml:164`: `${column}` 사용을 제거. Service 레이어에서 화이트리스트(`Set<String> ALLOWED_COLUMNS`) 검증 후 `<choose><when>` 분기로 컬럼별 쿼리 작성
  - `CustomerMapper.xml:222, 244`: `INTERVAL '${monthsAgo} months'` → `CURRENT_DATE - (#{monthsAgo} * INTERVAL '1 month')` 로 파라미터 바인딩 가능 형태로 변경
  - 전체 mapper에서 `${}` 사용처 일괄 grep → 모든 사용처를 화이트리스트/안전 형태로 전환 (ORDER BY는 별도 처리 필요)
- **예상**: 1일
- **검증**: `grep -rn '\${' src/main/resources/mapper/` 결과를 모두 검토하여 화이트리스트 검증 코드 첨부

### P0-3. UserController 접근 통제 도입 (C-04)
- **작업 내용**:
  - 클래스 레벨 `@PreAuthorize("isAuthenticated()")`
  - `GET /api/users`, `GET /api/users/search`, `GET /api/users/email/{email}`: `@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")`
  - `GET /api/users/{userId}`, `PATCH /api/users/{userId}`: 본인이거나 ADMIN/SUPER_ADMIN만 — `@PreAuthorize("#userId == authentication.principal.user.id or hasAnyRole('ADMIN','SUPER_ADMIN')")`
  - `GET /api/users/check-email`: 인증 + Rate Limit (P1-3에서 처리) — 또는 회원가입 플로우 내부에서만 호출하도록 변경
  - 회귀 방지를 위해 Controller 단위 통합 테스트 추가 (Customer 토큰으로 호출 시 403 검증)
- **예상**: 1일

### P0-4. Path Traversal 차단 (C-05)
- **작업 내용**:
  - `LocalFileStorageService.delete()`에서 `Path.normalize()` 호출 후, `uploadDir.toAbsolutePath().normalize()`의 자식 경로인지 `startsWith()`로 검증
  - 업로드 시에도 동일한 검증 추가
  - 단위 테스트: `/uploads/../etc/passwd`, `\\?\C:\windows\system32` 등 케이스에 대해 거부 확인
- **예상**: 0.5일

### P0-5. HTTP 보안 헤더 적용 (C-07)
- **작업 내용**: `SecurityConfig.filterChain()`에 추가
  ```java
  .headers(headers -> headers
      .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(true))
      .frameOptions(frame -> frame.deny())
      .contentTypeOptions(opts -> {})
      .referrerPolicy(rp -> rp.policy(SAME_ORIGIN))
      .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
  )
  ```
  - Swagger UI는 별도 경로에서 `frame-ancestors` 완화 검토
- **예상**: 0.5일

### P0-6. Swagger / API Docs 프로덕션 비활성화 (H-01)
- **작업 내용**:
  - `application-prod.yml`에 `springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false`
  - `SecurityConfig`에서 `@Profile`로 프로파일별 SecurityFilterChain 분리, prod에서는 swagger 경로 permitAll 미적용
  - `SwaggerConfig`에서 테스트 계정 정보 문구 제거
- **예상**: 0.5일

---

## P1 — 런칭 차단(High) (예상 6~8일)

### P1-1. Refresh Token 해시 저장 + 회전 도입 (C-06)
- **작업 내용**:
  - DB에 BCrypt 해시 저장 (검증 시 `matches()`)
  - 매 갱신 시 새 토큰 발급 + 이전 토큰 즉시 무효화 (jti 또는 row id 기반)
  - "재사용 감지(Replay Detection)" — 이미 회전된 토큰 재제출 시 해당 사용자 전체 세션 무효화
- **예상**: 2일

### P1-2. JWT 시크릿 강제 환경변수 (H-03)
- **작업 내용**:
  - `JwtProperties` 기본값 제거 + `@PostConstruct`에서 32바이트 이상 검증
  - 부팅 시 검증 실패 → 즉시 예외 (Fail Fast)
- **예상**: 0.5일

### P1-3. 인증/공개 엔드포인트 Rate Limiting (H-02, H-10)
- **작업 내용**:
  - Bucket4j 또는 Resilience4j 도입
  - `/api/auth/login`: IP당 15분에 5회, 계정당 15분에 5회 (실패 시 카운트, 성공 시 리셋)
  - `/api/auth/forgot-password`: IP당 시간당 3회
  - `/api/users/check-email`: IP당 분당 10회
  - `/api/public/**` 조회: IP당 분당 60회 (캐시 + 모니터링)
  - 5회 실패 후 계정 5분 잠금 (로그인 한정)
- **예상**: 2일

### P1-4. Business / Dashboard 접근 통제 일관성 (H-04)
- **작업 내용**:
  - `User.canAccessBusiness(businessId)` 의미 정리 (ADMIN도 본인 매장만? SUPER_ADMIN만 전체?)
  - 결과를 `boolean` 반환에서 **위반 시 즉시 `AccessDeniedException`** 으로 통일 (호출 누락 시 막을 수 있도록)
  - 매장 목록 API에서 사용자 소속 매장 자동 필터 (SUPER_ADMIN 제외)
  - DashboardController, BusinessController 전체 재검토
- **예상**: 1.5일

### P1-5. XSS / 입력 검증 강화 (H-06)
- **작업 내용**:
  - 사용자 작성 텍스트(리뷰 content, 문의 content, 매장 설명 등) 저장 전 OWASP Java HTML Sanitizer 적용
  - 또는 응답 시점 escape (FE와 정책 합의 필요 — 어느 레이어에서 처리할지)
  - DTO에 `@Valid` 누락된 multipart 컨트롤러 보강 (`CustomerReviewController:107`)
- **예상**: 1.5일

### P1-6. LIKE 와일드카드 이스케이프 (H-05)
- **작업 내용**:
  - Service 레이어에서 `keyword = keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")`
  - mapper에서 `LIKE ... ESCAPE '\'` 명시
- **예상**: 0.5일

### P1-7. PII 로그 마스킹 (H-08)
- **작업 내용**:
  - `common/util/MaskingUtils` 생성: `maskPhone`, `maskEmail`, `maskName`
  - 식별된 로그 라인 일괄 치환
  - Logback 패턴 레이아웃에 PII 필터 추가 (e.g. 정규식 기반 자동 마스킹)
- **예상**: 1일

### P1-8. CORS allowedHeaders 화이트리스트 (H-09)
- **작업 내용**: `Authorization`, `Content-Type`, `Accept`, `X-Requested-With`만 허용
- **예상**: 0.5일 (코드 변경 + FE 확인)

### P1-9. originalFilename 미저장 / PG 거래번호 보호 (H-07, H-11)
- **작업 내용**:
  - `ReviewService.createReviewImage()`에서 `originalFilename` 저장 제거 또는 sanitize
  - `PaymentResponse`에서 `pgTransactionId` 노출은 OWNER/SUPER_ADMIN 한정, 고객에게는 마스킹
- **예상**: 0.5일

---

## P2 — 출시 직후 1개월 내 (예상 4일)

### P2-1. BCrypt strength 12 (M-01)
- `new BCryptPasswordEncoder(12)` 적용
- 기존 사용자 해시는 그대로 (다음 로그인 시 재해싱하는 마이그레이션 검토)
- 0.5일

### P2-2. Pagination 상한 강제 (M-02)
- 공통 `PageRequestValidator` 또는 `@Max(100)` 강제
- 모든 컨트롤러 `size` 파라미터 일괄 점검
- 1일

### P2-3. 환경별 로깅 프로필 분리 (M-03)
- `application-prod.yml`: root=WARN, com.moer.booking=INFO, MyBatis SQL 로그 OFF
- 0.5일

### P2-4. 비밀번호 재설정 로그 익명화 (M-04)
- 미등록 이메일 로깅 시 해시 또는 마스킹
- 0.3일

### P2-5. Actuator 명시적 설정 (M-05)
- `management.endpoints.web.exposure.include=health,info`
- `/actuator/**`는 ADMIN 인증 요구
- 0.5일

### P2-6. docker-compose healthcheck 수정 (M-06)
- `pg_isready -U moer -d moer_dev`로 정정
- `.env` 파일로 분리 + 예제 제공
- 0.5일

### P2-7. 의존성 버전 명시 + 취약점 스캔 (M-07)
- `build.gradle.kts`에 핵심 라이브러리 버전 명시
- `org.owasp.dependencycheck` 또는 GitHub Dependabot 활성화
- 0.5일

### P2-8. UPLOAD_DIR OS 중립화 (M-08)
- 컨테이너 기반: `/var/lib/moer/uploads`
- Windows 개발: 환경변수 기반
- 0.2일

---

## P3 — 백로그 (Low + 운영 고도화)

| ID | 항목 | 작업 |
|----|------|------|
| P3-1 | DB 스키마 ENUM 표준화 (L-01) | VARCHAR + CHECK 제약 일관 적용 또는 PG ENUM 통일 |
| P3-2 | Validation 오류 응답 정제 (L-02) | 프로덕션에서 내부 필드명 대신 사용자 친화 메시지 |
| P3-3 | CORS 정의 단일화 (L-03) | SecurityConfig 한곳에서만 관리, WebConfig CORS 제거 |
| P3-4 | 초기 비밀번호 강제 변경 (L-04) | SUPER_ADMIN 첫 로그인 시 비밀번호 변경 강제 + CLAUDE.md/문서에서 비밀번호 제거 |
| P3-5 | 감사 로그 정합성 강화 | 모든 권한 변경/삭제 액션이 AuditLog에 기록되는지 회귀 테스트 |
| P3-6 | WAF / DDoS 보호 | CloudFlare 등 Edge 레벨 도입 검토 |
| P3-7 | 비밀번호 정책 강화 | 최소 길이 10자, 사전 단어 차단, NIST 800-63B 기반 |
| P3-8 | 보안 헤더 CSP 정책 강화 | 'unsafe-inline' 제거, nonce 기반 |

---

## 작업 순서 권고

```
Day 1-2: P0-1 (시크릿 회수, 가장 위험)
Day 3:   P0-2, P0-3 (SQL Injection + 권한)
Day 4:   P0-4, P0-5, P0-6 (path traversal + 헤더 + swagger)
Day 5-6: P1-1 (Refresh token rotation)
Day 7:   P1-2, P1-3 (JWT + Rate Limit)
Day 8-9: P1-4, P1-5 (접근 통제 + XSS)
Day 10:  P1-6 ~ P1-9 (이스케이프, 마스킹, CORS, FilePath)
Day 11:  통합 테스트 + 보안 회귀 테스트 (E2E)
Day 12:  P2 항목 일괄 처리
```

**총 예상 공수: 약 12 영업일 (2.5주)**

---

## 진척 추적

각 항목 완료 시 [launch-checklist.md](./launch-checklist.md)의 해당 체크박스를 갱신할 것.
구현 PR마다 본 문서의 ID(P0-1, P1-3 등)를 PR 제목에 포함.
