# 환경변수 / 프로필 설정 가이드

부팅에 필요한 시크릿/설정 주입 방법 및 Spring Profile 운영 가이드.

## 빠른 시작 (로컬 개발)

리포지토리에는 `application-local.yml`, `.env` 가 이미 존재합니다 (`.gitignore` 로 추적 제외).
운영 환경/외부 시크릿 회전이 끝났다면 그 파일들 내부 값을 새 값으로 교체하세요.

```bash
docker-compose up -d         # PostgreSQL 컨테이너 (.env 자동 로드)
./gradlew bootRun            # SPRING_PROFILES_ACTIVE 기본값 = local
```

부팅 시 `[SecretsValidator] 시크릿 검증 완료 (profile=local)` 로그 확인.

## 프로필 구조

| 파일 | 활성 조건 | 용도 | 추적 |
|------|----------|------|------|
| `src/main/resources/application.yml` | 항상 | 공통 설정. **비밀 없음.** 모든 시크릿은 `${VAR:}` 빈 fallback. | git 추적 |
| `src/main/resources/application-local.yml` | `SPRING_PROFILES_ACTIVE=local` (기본) | 로컬 개발용. 노출됐던 시크릿 평문 (`[LEAKED-SECRET / TODO-ROTATE]` 마킹) | `.gitignore` |
| `src/main/resources/application-prod.yml` | `SPRING_PROFILES_ACTIVE=prod` | 운영용. 모든 시크릿 환경변수 강제. Swagger OFF, WARN 로깅 | `.gitignore` |
| `.env` | docker-compose up 시 자동 로드 | PostgreSQL 컨테이너 환경변수 (`POSTGRES_*`) | `.gitignore` |

## 필수 환경변수 (운영 prod)

미설정 시 `SecretsValidator` 가 `IllegalStateException` 으로 부팅 차단.

| 변수 | 설명 |
|------|------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL 접속 정보 |
| `JWT_SECRET` | JWT 서명 키 (32바이트 이상) |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | SMTP 인증 |
| `MAIL_FROM` | 발신 메일 주소 |
| `GOOGLE_CLIENT_ID/SECRET` | (옵션) Google OAuth, 미설정 시 SNS 비활성 |
| `NAVER_CLIENT_ID/SECRET` | (옵션) Naver OAuth |
| `KAKAO_CLIENT_ID/SECRET` | (옵션) Kakao OAuth |
| `CORS_ALLOWED_ORIGINS` | 운영 프론트 도메인 (콤마 구분) |
| `FRONTEND_URL` | 비밀번호 재설정 메일의 base URL |
| `OAUTH2_REDIRECT_URI` | OAuth2 콜백 |
| `UPLOAD_DIR` | (기본 `/var/lib/moer/uploads`) |

## JWT 시크릿 생성 예시

```bash
# Linux / macOS
openssl rand -base64 48

# Windows PowerShell
$bytes = New-Object byte[] 48
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

## 운영 배포 시

- 시크릿 매니저 권장 (AWS Secrets Manager, HashiCorp Vault, GCP Secret Manager)
- CI/CD 파이프라인 시크릿 변수로 환경변수 주입
- `application-prod.yml` 은 `.gitignore` 되어 있으므로 배포 시 별도 채널로 운영 서버에 배치 (또는 Spring Cloud Config 등)

## 부팅 검증 로직 (SecretsValidator)

`io.moer.booking.common.config.SecretsValidator` 가 `@PostConstruct` 시점에 검증:
- `JWT_SECRET` 32바이트 미만 → 부팅 실패
- `DB_PASSWORD` 빈 값 → 부팅 실패
- prod 프로필에서 `MAIL_PASSWORD` 빈 값 → 부팅 실패
- OAuth client secret 누락 → 경고 로그만 (해당 SNS 로그인 비활성)

## 트러블슈팅

**Q. 부팅 시 `IllegalStateException: 필수 시크릿/설정이 누락되었습니다` 발생**
- A. 메시지에 나열된 환경변수를 설정하고 재시작. local 프로필 사용 시 `application-local.yml` 의 해당 값이 비어있지 않은지 확인.

**Q. PostgreSQL 컨테이너 시작 실패**
- A. `.env` 파일에 `POSTGRES_PASSWORD` 가 설정되어 있는지 확인. `docker-compose.yml` 은 환경변수 필수 (`${POSTGRES_PASSWORD:?...}`).

**Q. 기존 노출된 시크릿은 어떻게?**
- A. [audit-2026-05-12.md C-01](./audit-2026-05-12.md#c-01-운영-시크릿이-소스코드에-평문-커밋) 참조. git 이력 정리 + 외부 콘솔에서 시크릿 회전 필요. 현재 `application-local.yml` 의 `[LEAKED-SECRET / TODO-ROTATE]` 코멘트가 있는 항목들이 회전 대상.

**Q. local 프로필이 아닌 prod 로 부팅하려면?**
- A. `SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun` 또는 운영 환경변수 설정.
