# Backend API 요구사항 구현 설계서

**작성일**: 2026-03-04
**근거 문서**: `docs/백엔드-API-요구사항-2026-03-04.md`

---

## 구현 범위

전체 P0~P4 (12개 API)

## 결정 사항

| 항목 | 결정 |
|------|------|
| 이미지 업로드 | 로컬 파일 시스템 (기존 FileStorageService 활용) |
| 리뷰 이미지 | 방안 A: multipart 동시 업로드 |
| 고객 태그 | 기존 customers.tags 컬럼 활용 |
| 카카오 알림톡 | 설정 저장 + 발송 인터페이스만 (외부 API 미연동) |
| 도움말 | help_articles 테이블 + SuperAdmin CRUD |

---

## 구현 그룹

### 그룹 A: 기존 코드 확장 (최소 변경)

#### A1. FREE 플랜 기본 통계 API (P0)
- `DashboardController`에 `GET /dashboard/basic-stats` 추가
- `DashboardService.getBasicStats()` 신규 메서드
- 플랜 체크 없이 모든 사용자 허용
- 기존 ReservationRepository 쿼리 재사용

#### A2. 회원가입 플로우 변경 (P0)
- `AuthService.register()` 수정: plan/billingCycle 파라미터 무시
- 자동으로 TRIAL 플랜 + 30일 체험 적용
- RegisterRequest에서 plan/billingCycle optional 처리 (하위 호환)
- RegisterResponse에 subscription 정보 추가

#### A3. SSE 이벤트 타입 확장 (P1)
- `SseEmitterService`에 이벤트 타입 상수 추가
- HEARTBEAT 스케줄러 (30초 간격)
- 예약 생성/취소, 리뷰 생성 시 SSE 이벤트 발송 코드 추가
- `sendEventToBusinessOwner()` 구현 (현재 stub)

#### A4. 리뷰 이미지 개선 (P1)
- `CustomerReviewController` stub를 FileStorageService 연동으로 교체
- 리뷰 생성 API를 multipart 지원으로 변경
- 기존 별도 이미지 업로드 API도 유지 (하위 호환)

### 그룹 B: 신규 기능 추가

#### B1. 서비스 이미지 4종 API (P1)
- 테이블: `service_images` (id, service_id, business_id, image_url, thumbnail_url, original_filename, file_size, sort_order, caption, created_at)
- Entity: `ServiceImage`
- Repository: `ServiceImageRepository`
- Mapper: `mapper/service/ServiceImageMapper.xml`
- Controller: `ServiceImageController` (관리자) + Public 엔드포인트
- 에러코드: IMG001~IMG005

#### B2. 예약 reschedule API (P2)
- `ReservationController`에 `PATCH /{id}/reschedule` 추가
- `ReservationService.reschedule()` 신규
- 시간 충돌/근무시간/휴무일 검증 재사용
- RescheduleRequest, RescheduleResponse DTO

#### B3. 직원 주간 스케줄 조회 API (P2)
- `StaffController`에 `GET /{staffId}/schedule` 추가
- `StaffService.getStaffSchedule()` 신규
- 날짜 범위 예약 조회 + 근무 스케줄 + 블록 슬롯 통합
- StaffScheduleResponse DTO

#### B4. 고객 알림 발송 API (P2)
- `NotificationController`에 `POST /send` 추가
- `NotificationService.sendToCustomers()` 신규
- 세그먼트별 고객 조회 (기존 CustomerRepository 활용)
- notification_logs 테이블 활용
- 에러코드: NTF001~NTF002

#### B5. 카카오 알림톡 설정 API (P3)
- `BusinessSettingsController`에 GET/PUT kakao-alimtalk 추가
- businesses.settings JSONB에 kakao_alimtalk 키로 저장
- 실제 카카오 API 연동은 인터페이스만 정의
- 에러코드: NTF003

#### B6. 고객 CRM API 4종 (P3)
- **메모**: `customer_notes` 테이블 신규, CRUD API
- **태그**: 기존 customers.tags 활용, PUT으로 전체 교체
- **CSV 내보내기**: StreamingResponseBody로 CSV 스트리밍
- **중복 병합**: 전화번호 기반 중복 감지, 트랜잭션 내 병합
- 에러코드: CRM001~CRM005

#### B7. 랜딩 페이지 통계 API (P3)
- `PublicBusinessController`에 `GET /platform-stats` 추가
- 전체 매장/예약/리뷰 집계 (캐시 1시간 권장)
- 에러코드: STAT001

#### B8. 인앱 도움말 API (P4)
- 테이블: `help_articles` (id, category, title, content, related_feature, sort_order, lang, is_published, created_at, updated_at)
- Public 조회 API + SuperAdmin CRUD
- 에러코드: PUB001

---

## 신규 DB 테이블

### service_images
```sql
CREATE TABLE service_images (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES services(id),
    business_id BIGINT NOT NULL REFERENCES businesses(id),
    image_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    original_filename VARCHAR(255),
    file_size BIGINT,
    sort_order INTEGER DEFAULT 0,
    caption VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### customer_notes
```sql
CREATE TABLE customer_notes (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    business_id BIGINT NOT NULL REFERENCES businesses(id),
    content TEXT NOT NULL,
    is_private BOOLEAN DEFAULT false,
    author_id BIGINT REFERENCES users(id),
    author_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### help_articles
```sql
CREATE TABLE help_articles (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    related_feature VARCHAR(100),
    sort_order INTEGER DEFAULT 0,
    lang VARCHAR(10) DEFAULT 'ko',
    is_published BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 신규 에러코드

| 코드 | 메시지 | 도메인 |
|------|--------|--------|
| IMG001 | 지원하지 않는 파일 형식입니다 | 이미지 |
| IMG002 | 파일 크기가 초과되었습니다 (최대 10MB) | 이미지 |
| IMG003 | 이미지는 최대 N장까지 등록 가능합니다 | 이미지 |
| IMG004 | 이미지를 찾을 수 없습니다 | 이미지 |
| IMG005 | 이미지 ID 목록이 일치하지 않습니다 | 이미지 |
| NTF001 | 발송 대상이 없습니다 | 알림 |
| NTF002 | 유효하지 않은 발송 채널입니다 | 알림 |
| NTF003 | 카카오 채널 ID 인증에 실패했습니다 | 알림 |
| CRM001 | 메모를 찾을 수 없습니다 | CRM |
| CRM002 | 태그는 최대 10개까지 등록 가능합니다 | CRM |
| CRM003 | 태그 이름은 최대 20자까지 입력 가능합니다 | CRM |
| CRM004 | 주 고객 ID가 병합 목록에 포함될 수 없습니다 | CRM |
| CRM005 | 병합할 고객이 없습니다 | CRM |
| STAT001 | 통계 데이터를 일시적으로 사용할 수 없습니다 | 통계 |
| PUB001 | 도움말 콘텐츠를 찾을 수 없습니다 | 도움말 |

---

## 프론트엔드 요청 사항 (구현 중 발생 예상)

1. **이미지 URL 형식 협의**: 로컬 저장 시 URL이 `/uploads/services/{uuid}.webp` 형태. CDN URL 아님
2. **SSE 이벤트 데이터 구조**: 위 설계의 이벤트 페이로드 구조 확인 필요
3. **CSV BOM 처리**: UTF-8 BOM 포함 여부 프론트엔드 확인
4. **회원가입 Response 구조 변경**: subscription 필드 추가에 따른 프론트엔드 수정
