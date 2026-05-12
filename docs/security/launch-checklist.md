# moer-booking 프로덕션 런칭 보안 체크리스트

- **작성일**: 2026-05-12
- **사용 시점**: 프로덕션 배포 직전, 배포 PR 머지 전
- **연관 문서**: [audit-2026-05-12.md](./audit-2026-05-12.md), [improvement-plan.md](./improvement-plan.md)

> 모든 항목이 ☑️ 되어야 운영 환경 배포 가능.
> 한 항목이라도 미체크면 배포 보류.

---

## 0. 사전 준비

- [ ] 보안 감사 보고서 검토 완료 (`audit-2026-05-12.md`)
- [ ] 개선 계획서의 P0/P1 항목 100% 머지 완료 (`improvement-plan.md`)
- [ ] 프로덕션 배포 책임자(이름/이메일) 명시
- [ ] 사고 대응(IR) 연락 체인 확정

---

## 1. 시크릿 / 자격증명

### 1.1 git 이력 정리
- [ ] `git log -p -- src/main/resources/application.yml | grep -iE "password|secret"` 결과가 **공백**
- [ ] `git log -p -- docker-compose.yml | grep -iE "password|secret"` 결과가 **공백**
- [ ] BFG / `git filter-repo` 실행 후 force-push 완료 + 팀 전체 재클론 안내
- [ ] GitHub Secret Scanning 알림 모두 resolved

### 1.2 노출된 시크릿 전면 회전
- [ ] PostgreSQL `moer` 계정 비밀번호 변경 (구 `moer2026@` 폐기)
- [ ] PostgreSQL `root` 계정 비밀번호 변경 (구 `root2025@` 폐기)
- [ ] Google OAuth Client Secret 콘솔에서 재발급 (구 `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` 무효화)
- [ ] Naver OAuth Client Secret 재발급 (구 `JcUZkcUPhe` 무효화)
- [ ] Kakao OAuth Client Secret 재발급 (구 `8zOJXees3y4v1Zz2vckIfmhjCVYRNVcU` 무효화)
- [ ] SMTP `kkm@moer.io` 비밀번호 재발급 (구 `U3SXTOXHzkPX` 무효화)
- [ ] JWT 시크릿 32바이트 이상 랜덤 재생성 (`openssl rand -base64 48`)

### 1.3 환경변수 설정 (운영 환경)
- [ ] `SPRING_DATASOURCE_PASSWORD`
- [ ] `JWT_SECRET` (32바이트 이상)
- [ ] `GOOGLE_CLIENT_SECRET`, `NAVER_CLIENT_SECRET`, `KAKAO_CLIENT_SECRET`
- [ ] `MAIL_USERNAME`, `MAIL_PASSWORD`
- [ ] `CORS_ALLOWED_ORIGINS` (운영 도메인만, localhost 없음)
- [ ] `FRONTEND_URL`, `OAUTH2_REDIRECT_URI`, `OAUTH2_CUSTOMER_REDIRECT_URI`
- [ ] `UPLOAD_DIR` (운영 파일시스템 경로)
- [ ] 부팅 시 환경변수 누락하면 즉시 실패하도록 검증 로직 동작 확인

### 1.4 시크릿 관리 인프라
- [ ] 시크릿 관리 솔루션 사용 중 (AWS Secrets Manager / Vault / GCP Secret Manager 등)
- [ ] CI/CD 빌드 로그에 시크릿이 echo 되지 않는지 확인
- [ ] `.env`, `application-prod.yml`이 `.gitignore`에 포함되어 있음

---

## 2. 인증 / 인가

- [ ] `UserController` 전체 메서드에 `@PreAuthorize` 적용
- [ ] `BusinessController`, `DashboardController` 권한 검증 일관성 확인
- [ ] `User.canAccessBusiness()` 호출 시 결과 무시 케이스 0건
- [ ] CUSTOMER 토큰으로 OWNER/ADMIN 전용 API 호출 시 403 응답 회귀 테스트 통과
- [ ] OWNER A 토큰으로 OWNER B의 매장/예약/매출 조회 시 403 회귀 테스트 통과
- [ ] SUPER_ADMIN 회원가입/삭제/정지 차단 동작 확인
- [ ] Refresh Token DB 저장 시 BCrypt 해시
- [ ] Refresh Token 회전(rotation) 동작 확인 (재사용 시 세션 무효화)
- [ ] JWT 시크릿 256비트 이상, 환경변수 미설정 시 부팅 실패
- [ ] BCrypt strength 12 이상 적용

---

## 3. 입력 검증 / 인젝션

- [ ] `grep -rn '\${' src/main/resources/mapper/`의 모든 결과가 화이트리스트 검증되거나 안전한 정렬 키
- [ ] `BusinessSettingsMapper.xml`의 컬럼명 화이트리스트 검증 코드 존재
- [ ] `CustomerMapper.xml`의 `INTERVAL` 절이 파라미터 바인딩으로 전환됨
- [ ] LIKE 검색에서 `%`, `_`, `\` 이스케이프 처리
- [ ] 모든 `@RequestBody` DTO에 `@Valid` 적용
- [ ] `@RequestParam`에 `@Min/@Max/@Size` 또는 컨트롤러 내부 검증 존재
- [ ] 파일 업로드: 확장자 화이트리스트, MIME 검증, 사이즈 제한
- [ ] 파일 삭제: `Path.normalize()` + `startsWith(uploadDir)` 검증

---

## 4. 네트워크 / API

- [ ] HTTPS 강제 (Reverse Proxy 또는 Spring `requires-channel`)
- [ ] HSTS 헤더 (`max-age=31536000; includeSubDomains`)
- [ ] `X-Frame-Options: DENY` 또는 CSP `frame-ancestors`
- [ ] `X-Content-Type-Options: nosniff`
- [ ] `Content-Security-Policy` 정의됨
- [ ] `Referrer-Policy: same-origin` 또는 `strict-origin-when-cross-origin`
- [ ] CORS `allowedOrigins`이 운영 도메인 화이트리스트 (와일드카드 없음)
- [ ] CORS `allowedHeaders`가 명시적 화이트리스트
- [ ] Rate Limiting 동작 확인:
  - [ ] 로그인: IP/계정당 15분 5회
  - [ ] 비밀번호 재설정 요청: IP당 시간당 3회
  - [ ] 이메일 중복 확인: IP당 분당 10회
  - [ ] 공개 검색 API: IP당 분당 60회
- [ ] 로그인 5회 실패 시 계정 잠금 동작

---

## 5. 데이터 / 로깅

- [ ] `application-prod.yml` 로그 레벨 INFO 이상 (`com.moer.booking=INFO`, root=WARN)
- [ ] MyBatis SQL 로깅 OFF (`mybatis.configuration.log-impl=NoLoggingImpl`)
- [ ] 전화번호/이메일/이름이 로그에 평문 출력되는 라인 0건 (`grep "log\.\(info\|debug\)" | grep -E "phone|email|name"` 결과 검토)
- [ ] PII 마스킹 유틸 적용 (`MaskingUtils.maskPhone/Email`)
- [ ] Logback 패턴에 PII 자동 마스킹 필터
- [ ] 비밀번호 재설정 시 미등록 이메일이 로그에 평문 기록되지 않음
- [ ] `PaymentResponse`에서 `pgTransactionId` 권한별 노출
- [ ] 로그 보관 정책 / 회전 정책 수립 (개인정보보호법 준수)

---

## 6. 응답 / 에러 처리

- [ ] `GlobalExceptionHandler`가 스택 트레이스 미노출 (확인됨)
- [ ] 사용자 열거 방지: 로그인 실패 시 "이메일 없음" vs "비밀번호 틀림" 구분 없음 (확인됨)
- [ ] 비밀번호 재설정 응답이 미등록 이메일에도 동일 (확인됨)
- [ ] Validation 에러 메시지가 내부 컬럼/구조 누설하지 않음

---

## 7. 의존성 / 빌드

- [ ] `./gradlew dependencyCheckAnalyze` 또는 OWASP Dependency-Check 실행 결과 Critical/High CVE 0건
- [ ] GitHub Dependabot 알림 모두 resolved
- [ ] Spring Boot, MyBatis, jackson-databind, postgresql JDBC, snakeyaml 등 최신 안정 버전
- [ ] 사용하지 않는 의존성 제거
- [ ] 빌드 산출물에 디버그 심볼/소스맵 없음

---

## 8. 인프라 / 배포

- [ ] Docker 이미지가 non-root 사용자로 실행
- [ ] `docker-compose.yml`의 healthcheck가 실제 동작 (`pg_isready -U moer -d moer_dev`)
- [ ] PostgreSQL 포트(5432)는 내부 네트워크에서만 접근 가능 (운영)
- [ ] DB 백업 정책 수립, 복구 테스트 1회 완료
- [ ] 업로드 디렉토리 권한: 애플리케이션 사용자만 read/write
- [ ] Actuator: `/actuator/health`, `/actuator/info`만 노출, 나머지 차단
- [ ] Actuator health 상세는 ADMIN 인증 시에만
- [ ] Swagger UI / API Docs 프로덕션 비활성화 확인 (브라우저로 `/swagger-ui.html` 접근 시 404 또는 403)

---

## 9. 운영 준비

- [ ] 보안 사고 대응 절차(IR) 문서화
- [ ] 보안 모니터링: 비정상 로그인 시도, 권한 위반(403 비율) 알림
- [ ] WAF / DDoS 보호 (CloudFlare 등) 적용 여부 결정
- [ ] 초기 SUPER_ADMIN 계정 비밀번호 변경 (CLAUDE.md 노출 `Admin123!` 폐기)
- [ ] `CLAUDE.md`, `docs/superadmin_summary.md` 등 문서에서 운영 비밀번호 모두 제거
- [ ] Swagger 테스트 계정(`admin@moer.io/password123` 등)은 프로덕션에 존재하지 않음
- [ ] 개인정보처리방침 / 약관 페이지 공개
- [ ] 개인정보보호법(PIPA) 신고 필요 여부 확인 (5만명 이상 시)

---

## 10. 회귀 / 부하 / 침투 테스트

- [ ] 보안 통합 테스트 스위트 통과 (권한, 인증, 인젝션)
- [ ] OWASP ZAP 또는 Burp Suite Active Scan 실행 후 High 0건
- [ ] 외부 모의해킹 또는 코드 리뷰 1회 (선택, 권고)
- [ ] 부하 테스트: 동시 사용자 N명에서 Rate Limit 정상 동작
- [ ] Path Traversal, SQL Injection, XSS 페이로드에 대한 음성 테스트

---

## 검증 명령 모음 (참고)

```bash
# 시크릿 노출 점검
git log --all -p | grep -iE "password|secret|api[_-]?key" | head -50

# MyBatis 위험 패턴
grep -rn '\${' src/main/resources/mapper/

# PII 로그 패턴
grep -rn "log\.\(info\|debug\)" src/main/java | grep -iE "phone|email"

# 권한 어노테이션 누락 확인
grep -L "@PreAuthorize" src/main/java/io/moer/booking/domain/*/controller/*.java
```

---

## 최종 결재

- 배포 책임자: ______________________ (서명/날짜)
- 보안 검토자: ______________________ (서명/날짜)
- 운영 책임자: ______________________ (서명/날짜)

모든 체크 완료 후, [audit-2026-05-12.md](./audit-2026-05-12.md)에 후속 감사 일정(예: 6개월 후 재감사)을 기재할 것.
