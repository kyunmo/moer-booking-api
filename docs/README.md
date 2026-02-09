# moer 예약 시스템 백엔드 문서

moer 예약 시스템 백엔드의 기술 문서입니다.

## 프로젝트 개요

**프로젝트명**: moer-booking
**목적**: 다업종(미용실, 필라테스, 카페 등) 예약 관리 시스템 백엔드 API
**기술 스택**: Spring Boot 4.0.1, Java 17, PostgreSQL 16, MyBatis 4.0.0

## 문서 구조

### [01. 아키텍처](./01_architecture/README.md)
시스템 전체 아키텍처 설명

- [패키지 구조](./01_architecture/package-structure.md)
- [레이어 아키텍처](./01_architecture/layered-architecture.md)
- [보안 구조](./01_architecture/security.md)
- [예외 처리](./01_architecture/exception-handling.md)

**핵심 내용**:
- Layered Architecture + DDD 스타일
- JWT 기반 인증/인가
- 전역 예외 처리 (`GlobalExceptionHandler`)
- 통합 API 응답 포맷 (`ApiResponse<T>`)

### [02. 도메인](./02_domain/README.md)
도메인별 상세 가이드 및 개발 패턴

- [Reservation 도메인](./02_domain/reservation.md) - 가장 복잡한 비즈니스 로직
- [도메인 개발 패턴](./02_domain/development-pattern.md) - 새 도메인 추가 가이드

**핵심 내용**:
- 표준 개발 흐름 (Entity → DTO → Repository → Service → Controller)
- 9개 도메인 (auth, user, business, staff, service, customer, reservation, holiday, dashboard)
- 실제 코드 예시 포함

### [03. 데이터베이스](./03_database/README.md)
PostgreSQL 스키마 및 MyBatis 매핑

**핵심 내용**:
- 11개 테이블 설계
- PostgreSQL Enum 타입 활용
- JSONB 타입 핸들러 (`JsonTypeHandler`)
- 동적 쿼리 패턴
- 인덱스 전략

### [04. API](./04_api/README.md)
REST API 엔드포인트 및 사용 가이드

**핵심 내용**:
- 모든 API 엔드포인트 목록
- 요청/응답 예시
- 인증 방식 (JWT Bearer Token)
- 에러 코드 체계
- Swagger UI 사용법

### [05. 개발](./05_development/README.md)
개발 환경 설정 및 개발 가이드

**핵심 내용**:
- 개발 환경 설정 (IDE, Lombok, Docker)
- 로컬 실행 방법
- 코딩 컨벤션 (네이밍, 주석, 스타일)
- Git 워크플로우
- 디버깅 및 문제 해결

### [06. 배포](./06_deployment/README.md)
프로덕션 환경 배포 가이드

**핵심 내용**:
- Gradle 빌드
- 환경 변수 설정
- Docker 배포 (Dockerfile, docker-compose)
- 프로덕션 설정 (보안, 로깅, 모니터링)
- CI/CD 예시

## 빠른 시작

### 1. 프로젝트 클론

```bash
git clone https://github.com/your-repo/moer-booking.git
cd moer-booking
```

### 2. PostgreSQL 시작

```bash
docker-compose up -d
```

### 3. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 4. API 확인

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health Check: http://localhost:8080/api/health

## 주요 기능

### 인증/인가
- JWT Access Token (1시간) + Refresh Token (7일)
- BCrypt 비밀번호 암호화
- Spring Security 통합

### 예약 관리
- 예약 생성 (고객 자동 생성 지원)
- 시간 충돌 검증
- 휴무일 체크
- 상태 관리 (PENDING → CONFIRMED → COMPLETED)
- 예약 완료 시 자동으로 고객 이력 생성

### 고객 관리
- 방문 횟수, 총 결제 금액 자동 집계
- 고객 시술 이력 관리
- 태그 관리 (VIP, 단골 등)

### 매장 관리
- 매장 정보 관리
- 영업시간 설정 (JSONB)
- 예약 설정 (자동 확정, 예약 간격 등)

### 직원 관리
- 직원 CRUD
- 포트폴리오 관리

### 대시보드
- 오늘/주간/월간 통계
- 일별 예약 건수 그래프
- 최근 예약/고객 목록

### 슈퍼 관리자 (SUPER_ADMIN) ⭐ NEW
- **시스템 전체 관리**: 모든 매장 및 사용자 조회/관리
- **대시보드**: 시스템 통계, 매출 랭킹, 업종별 통계
- **매장 관리**: 전체 매장 조회/삭제/상태 변경
- **사용자 관리**: 역할 변경, 사용자 정지/삭제
- **감사 로그**: 중요 액션 이력 조회 (삭제, 역할 변경 등)

**📖 문서**:
- **[슈퍼 관리자 요약](./superadmin_summary.md)** - 빠른 개요
- **[화면 구현 가이드](./superadmin_frontend_guide.md)** - 프론트엔드 개발자용 완전 가이드

## 기술적 특징

### 1. JSONB 활용
PostgreSQL의 JSONB를 사용하여 유연한 데이터 구조 지원:
- 매장 영업시간 (요일별 설정)
- 예약 서비스 목록
- 고객 시술 상세 정보

### 2. MyBatis 동적 쿼리
복잡한 검색 조건을 MyBatis의 `<if>`, `<where>` 태그로 처리

### 3. 계층화된 예외 처리
- `ErrorCode` Enum으로 에러 코드 체계화
- `BaseException` 추상 클래스 상속
- `GlobalExceptionHandler`로 전역 예외 처리

### 4. 도메인 중심 설계
각 도메인별로 독립적인 패키지 구조 (DDD 스타일)

## 개발 팀을 위한 가이드

### 새 기능 개발 시
1. [도메인 개발 패턴](./02_domain/development-pattern.md) 참고
2. Entity → DTO → Repository → Service → Controller 순서로 개발
3. ErrorCode 추가
4. Swagger 문서 확인
5. Git 커밋 및 PR 생성

### 코드 리뷰 시 체크사항
- [ ] 코딩 컨벤션 준수
- [ ] 비즈니스 규칙 검증 로직 포함
- [ ] 예외 처리 적절성
- [ ] DTO 사용 (Entity 직접 노출 금지)
- [ ] 트랜잭션 관리 (`@Transactional`)
- [ ] 로깅 적절성

### 문제 발생 시
1. [개발 가이드 - 문제 해결](./05_development/README.md#문제-해결) 참고
2. 로그 확인 (`application.log`)
3. Swagger UI에서 API 테스트
4. 디버거 사용

## 문의 및 기여

프로젝트에 기여하고 싶으시거나 문의사항이 있으시면 이슈를 생성해주세요.

## 라이선스

MIT License

---

**문서 작성일**: 2026-02-08
**최종 수정일**: 2026-02-08
**작성자**: Claude Code
