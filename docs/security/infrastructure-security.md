# 인프라/Edge 레벨 보안 가이드

- **작성일**: 2026-05-12
- **대상**: 운영 인프라 담당자 / DevOps
- **참고**: 코드 레벨 방어는 [improvement-plan.md](./improvement-plan.md) 와 [launch-checklist.md](./launch-checklist.md) 에 정리됨. 본 문서는 인프라/Edge 영역만 다룸.

## 1. WAF (Web Application Firewall)

### 권장 솔루션
| 후보 | 장점 | 단점 | 비용(월) |
|------|------|------|---------|
| **CloudFlare Pro/Business** | 글로벌 CDN + DDoS + WAF 통합, 한국 PoP | OWASP rules 일부 유료 | $20–$200 |
| **AWS WAF + CloudFront** | AWS 생태계 통합, 세밀한 룰 제어 | 룰 작성/운영 부담 | ~$10 + 룰별 |
| **NHN Cloud Smart WAF** | 국내 사업자, 한국어 지원 | 글로벌 CDN 부재 | 견적 |

**MVP 단계 권장**: **CloudFlare Pro** — 빠른 도입, 글로벌 CDN, 충분한 WAF/DDoS

### 적용 룰셋
- **OWASP Core Rule Set 3.x**: SQL Injection, XSS, RCE, LFI, RFI, Scanner 차단
- **국가 차단**: 운영 대상 외 국가(예: 특정 위협 인텔리전스 등록 국가) 차단
- **봇 관리**: CloudFlare Bot Fight Mode 또는 AWS Bot Control
- **레이트 제한**: `/api/auth/*` 추가 차단 (app 레벨 P1-3 외 Edge 레벨 보강)

### 우회 방지
- Origin 서버는 WAF/CDN을 거치지 않은 직접 IP 접근 차단
  - AWS: Security Group 으로 CloudFront IP 만 허용
  - 일반: nginx/ALB 에서 CF-Connecting-IP 헤더 또는 source CIDR 검증
- WAF 정책 변경 이력은 Git 또는 별도 저장소 관리 (롤백 가능성)

## 2. DDoS 방어

### Layer 3/4 (네트워크/전송 계층)
- 대부분의 CDN/Edge 가 기본 제공 (CloudFlare Magic Transit, AWS Shield Standard)
- AWS Shield Advanced ($3,000/월) 는 24/7 대응 + 비용 보호 — 트래픽이 클 때만 고려

### Layer 7 (애플리케이션 계층)
- WAF + Rate Limiting 으로 대응
- 추가: HTTP/2 Rapid Reset (CVE-2023-44487) 패치 적용 여부 — Spring Boot 4 / Tomcat 최신은 OK

### 알림/대응 플레이북
- CloudFlare/AWS 에서 임계치 초과 시 Slack/PagerDuty 알림 설정
- 사고 대응 IR(Incident Response) 문서 작성 + 시뮬레이션 1회/분기

## 3. 추가 Edge 보호

### 3.1 HTTPS / TLS
- TLS 1.2 이상 강제, 1.0/1.1 비활성
- 인증서: Let's Encrypt 자동 갱신 또는 ACM (AWS) 활용
- HSTS 헤더는 코드 레벨에서 이미 설정 (P0-5)

### 3.2 IP 평판 / Threat Intelligence
- CloudFlare WAF Managed Rules > "OWASP" + "CloudFlare Specials" 활성화
- AbuseIPDB / Spamhaus 연동 검토

### 3.3 Rate Limiting (Edge 보강)
- 코드 레벨 (P1-3 Bucket4j) 은 단일 인스턴스 한정. Edge 레벨에서 글로벌 한도 추가:
  - `/api/auth/login` — Edge 에서 IP/계정당 분당 10회 (보강)
  - 전체 API — IP당 초당 100 req

### 3.4 Geolocation 차단
- 운영 대상이 국내라면 해외 IP의 admin/superadmin 라우트 접근 차단 검토
- 단, OAuth 콜백/외부 결제 등 합법적 해외 IP 호출은 예외 처리

## 4. 모니터링 / 로깅

### 권장 도구
- **APM**: Datadog APM, New Relic, Elastic APM
- **로그**: ELK / Grafana Loki / Datadog Logs
- **에러 추적**: Sentry

### 보안 핵심 메트릭
- 인증 실패율 (403/401) 급증 → 브루트포스 알림
- 5xx 비율 → 서비스 안정성
- WAF 차단 건수 → 공격 패턴 모니터링
- DB connection pool 사용률 → 자원 고갈 조기 감지

## 5. 비밀 관리 (Secrets Management)

### 운영 시크릿 저장소
- **AWS**: AWS Secrets Manager + Parameter Store
- **GCP**: Secret Manager
- **HashiCorp Vault** (자체 운영)
- **CloudFlare Workers Secrets** (Edge 단)

### 회전 정책
- DB/JWT 시크릿: 6개월 또는 인사 변동 시
- OAuth Client Secret: 12개월 또는 노출 의심 시 즉시
- 코드/문서에 평문 저장 금지 (P0-1 에서 환경변수화 완료)

## 6. 백업 / 재해복구 (DR)

- DB 자동 백업: 매일 전체 + 시간별 증분 (AWS RDS / Aurora 기본 제공)
- 백업 복구 테스트: 분기 1회 (실제 복구 가능 여부 검증)
- 업로드 파일(/uploads): S3 등 객체 스토리지로 이전 권장 (로컬 디스크 의존성 제거)
- 재해 발생 시 RTO/RPO 목표 명시:
  - RTO(복구 시간): 4시간 이하 권장
  - RPO(데이터 손실): 1시간 이하 권장

---

## 적용 우선순위 (런칭 전)

| 우선순위 | 항목 | 비고 |
|---------|------|------|
| P1 | CloudFlare Pro + DNS 위임 | DDoS + WAF + HTTPS 일괄 |
| P1 | TLS 1.2+ 강제 + HSTS 활성 | 인증서 자동 갱신 |
| P1 | Origin 직접 접근 차단 | Security Group / firewall |
| P2 | APM/Sentry 도입 | 운영 가시성 |
| P2 | 시크릿 매니저 도입 | AWS Secrets Manager 권장 |
| P3 | Geo 차단 + Bot 관리 | 트래픽 패턴 보고 결정 |

본 문서는 코드 레벨 변경이 아닌 **인프라/운영 영역**의 권장사항입니다.
