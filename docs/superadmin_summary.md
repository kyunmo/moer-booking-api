# 슈퍼 관리자 기능 요약

> **구현 완료일**: 2024
> **Phase**: Phase 1 (Core Features) ✅

---

## 📌 빠른 링크

- **[프론트엔드 구현 가이드](./superadmin_frontend_guide.md)** - 화면 개발자용 상세 가이드
- **[백엔드 API 문서](./04_api/README.md)** - API 명세
- **[도메인 개발 패턴](./02_domain/development-pattern.md)** - 백엔드 구조

---

## 🎯 구현된 기능

### ✅ Phase 1: 핵심 기능 (완료)

#### 1. 권한 시스템
- `SUPER_ADMIN` 역할 추가
- 전체 매장 및 사용자 접근 권한
- 권한 체크 강화 (Business 도메인 보안)

#### 2. 감사 로그 시스템
- 모든 중요 액션 자동 기록
  - 매장 생성/수정/삭제/상태변경
  - 사용자 생성/역할변경/상태변경/삭제
  - 시스템 백업/복원/설정변경
- JSONB 메타데이터 저장 (변경 전/후 값)
- IP 주소, User-Agent 기록

#### 3. 슈퍼 관리자 대시보드
**시스템 통계**:
- 매장 통계 (전체/활성/비활성/정지)
- 사용자 통계 (역할별 카운트)
- 예약 통계 (오늘/이번달 매출 및 건수)
- 성장 지표 (신규 매장/사용자)

**매출 랭킹**:
- 기간별 매장 매출 순위
- 예약 건수 포함

**업종별 통계**:
- 업종별 매장 수
- 업종별 총 매출

#### 4. 매장 관리
- 전체 매장 목록 조회 (페이징, 필터링)
- 매장 강제 삭제 (소프트/하드)
- 매장 상태 일괄 변경

#### 5. 사용자 관리
- 전체 사용자 목록 조회 (페이징, 필터링)
- 사용자 역할 변경 (SUPER_ADMIN ↔ ADMIN ↔ OWNER ↔ STAFF)
- 사용자 강제 정지
- 사용자 강제 삭제

#### 6. 감사 로그 조회
- 전체 로그 목록 (페이징, 필터링)
- 로그 상세 조회
- 액션 타입별 필터링
- 날짜 범위 검색

---

## 🔧 기술 구현

### 백엔드

**새로운 도메인**:
```
domain/
├── auditlog/          # 감사 로그
│   ├── AuditLog.java
│   ├── AuditAction.java
│   ├── controller/
│   ├── dto/
│   ├── repository/
│   └── service/
└── superadmin/        # 슈퍼 관리자
    ├── controller/
    │   ├── SuperAdminBusinessController.java
    │   ├── SuperAdminUserController.java
    │   └── SuperAdminDashboardController.java
    ├── dto/
    │   ├── SystemStats.java
    │   ├── BusinessRevenueRank.java
    │   ├── BusinessTypeStats.java
    │   ├── ChangeRoleRequest.java
    │   └── BulkStatusUpdateRequest.java
    └── service/
        ├── SuperAdminBusinessService.java
        ├── SuperAdminUserService.java
        └── SuperAdminDashboardService.java
```

**확장된 Repository**:
- `BusinessRepository`: 통계 쿼리 추가 (상태별/업종별 카운트, 매출 랭킹)
- `UserRepository`: 통계 쿼리 추가 (역할별 카운트, 삭제 메서드)
- `ReservationRepository`: 통계 쿼리 추가 (날짜별/월별/업종별 매출)

**보안 강화**:
- 모든 Business 도메인 메서드에 권한 체크 추가
- SUPER_ADMIN 일반 회원가입 차단
- SUPER_ADMIN 계정 삭제/정지 차단

### 프론트엔드

**권장 화면 구조**:
```
슈퍼 관리자 메뉴
├── 대시보드
│   ├── 시스템 통계 카드 (6개)
│   ├── 매출 랭킹 테이블 (TOP 10)
│   └── 업종별 통계 차트
├── 매장 관리
│   ├── 검색/필터 (키워드, 업종, 상태)
│   ├── 매장 목록 테이블
│   ├── 일괄 작업 (상태 변경)
│   └── 매장 삭제 (소프트/하드)
├── 사용자 관리
│   ├── 검색/필터 (키워드, 역할, 상태)
│   ├── 사용자 목록 테이블
│   └── 액션 (역할 변경, 정지, 삭제)
└── 감사 로그
    ├── 로그 목록 테이블
    ├── 필터 (액션, 날짜 범위)
    └── 로그 상세 모달
```

---

## 🚀 시작하기

### 1. 초기 계정

```
Email: superadmin@moer.io
Password: Admin123!
```

⚠️ **중요**: 프로덕션 환경에서는 즉시 비밀번호를 변경하세요!

### 2. 로그인

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "superadmin@moer.io",
  "password": "Admin123!"
}
```

### 3. API 호출

모든 슈퍼 관리자 API는 JWT 토큰 필요:

```http
GET /api/superadmin/dashboard/stats
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 4. 권한 체크

```javascript
const user = getCurrentUser();

if (user.role === 'SUPER_ADMIN') {
  // 슈퍼 관리자 기능 표시
} else {
  // 일반 사용자 기능만 표시
}
```

---

## 📡 주요 API 엔드포인트

### 대시보드
- `GET /api/superadmin/dashboard/stats` - 시스템 통계
- `GET /api/superadmin/dashboard/business-ranking` - 매출 랭킹
- `GET /api/superadmin/dashboard/stats-by-type` - 업종별 통계

### 매장 관리
- `GET /api/superadmin/businesses` - 전체 매장 목록
- `DELETE /api/superadmin/businesses/{id}` - 매장 삭제
- `PATCH /api/superadmin/businesses/bulk-status` - 상태 일괄 변경

### 사용자 관리
- `GET /api/superadmin/users` - 전체 사용자 목록
- `PATCH /api/superadmin/users/{id}/role` - 역할 변경
- `PATCH /api/superadmin/users/{id}/suspend` - 사용자 정지
- `DELETE /api/superadmin/users/{id}` - 사용자 삭제

### 감사 로그
- `GET /api/audit-logs` - 로그 목록
- `GET /api/audit-logs/{id}` - 로그 상세

**📖 상세한 API 명세는 [프론트엔드 구현 가이드](./superadmin_frontend_guide.md)를 참고하세요.**

---

## 🎨 화면 구현 가이드

프론트엔드 개발자를 위한 **완전한 구현 가이드**가 준비되어 있습니다:

### [📘 슈퍼 관리자 화면 구현 가이드](./superadmin_frontend_guide.md)

**포함 내용**:
- ✅ 모든 화면 레이아웃 및 와이어프레임
- ✅ 완전한 API 명세 및 요청/응답 예시
- ✅ React 샘플 코드 (컴포넌트, 훅, 서비스)
- ✅ 에러 처리 가이드
- ✅ 페이지네이션 구현
- ✅ 스타일 가이드 (색상, 타이포그래피, 버튼)
- ✅ 배포 체크리스트

---

## 🔒 보안 기능

### 1. 권한 체크
- 모든 슈퍼 관리자 API는 `SUPER_ADMIN` 역할 확인
- 권한 없으면 `403 Forbidden` (에러 코드: `SA001`)

### 2. SUPER_ADMIN 보호
- 일반 회원가입으로 SUPER_ADMIN 생성 불가
- SUPER_ADMIN 계정 삭제 불가 (에러 코드: `SA002`)
- SUPER_ADMIN 계정 정지 불가 (에러 코드: `SA002`)

### 3. 감사 로그
- 모든 중요 액션 자동 기록
- 수행자 정보 (ID, 이메일, 역할)
- 변경 내역 (메타데이터 JSONB)
- IP 주소 및 User-Agent 기록

### 4. Business 도메인 보안
- 모든 메서드에 권한 체크 추가
- OWNER는 자신의 매장만 접근 가능
- SUPER_ADMIN/ADMIN은 전체 접근 가능

---

## 📊 데이터베이스

### 새로운 테이블

**audit_logs**:
```sql
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    user_email VARCHAR(100),
    user_role VARCHAR(20),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    description TEXT,
    metadata JSONB,                -- 변경 전/후 값
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**초기 데이터**:
```sql
-- 슈퍼 관리자 계정
INSERT INTO users (email, password, name, role, status, email_verified)
VALUES (
    'superadmin@moer.io',
    '$2a$10$...',  -- BCrypt(Admin123!)
    '시스템 관리자',
    'SUPER_ADMIN',
    'ACTIVE',
    'Y'
);
```

---

## 🧪 테스트 시나리오

### 1. 대시보드
- [ ] 로그인 → 슈퍼 관리자 메뉴 표시 확인
- [ ] 시스템 통계 카드 6개 정상 표시
- [ ] 매출 랭킹 TOP 10 조회
- [ ] 업종별 통계 차트 표시

### 2. 매장 관리
- [ ] 전체 매장 목록 조회 (페이징)
- [ ] 키워드 검색 (매장명)
- [ ] 업종별 필터링
- [ ] 상태별 필터링
- [ ] 매장 소프트 삭제
- [ ] 매장 하드 삭제
- [ ] 일괄 상태 변경 (여러 매장 선택)

### 3. 사용자 관리
- [ ] 전체 사용자 목록 조회
- [ ] 키워드 검색 (이름, 이메일)
- [ ] 역할별 필터링
- [ ] 사용자 역할 변경 (OWNER → ADMIN)
- [ ] 사용자 정지
- [ ] 사용자 삭제
- [ ] SUPER_ADMIN 삭제 시도 → 에러 확인

### 4. 감사 로그
- [ ] 전체 로그 목록 조회
- [ ] 액션 타입 필터링
- [ ] 날짜 범위 검색
- [ ] 로그 상세 모달 표시
- [ ] 메타데이터 JSON 포맷 확인

### 5. 보안
- [ ] OWNER 계정으로 슈퍼 관리자 API 호출 → 403 Forbidden
- [ ] JWT 토큰 없이 API 호출 → 401 Unauthorized
- [ ] 만료된 토큰으로 API 호출 → 403 Forbidden

---

## 🐛 에러 코드

| 코드 | 메시지 | 설명 |
|------|--------|------|
| `SA001` | 슈퍼 관리자 권한이 필요합니다 | SUPER_ADMIN이 아닌 사용자의 API 호출 |
| `SA002` | 슈퍼 관리자는 삭제할 수 없습니다 | SUPER_ADMIN 계정 삭제/정지 시도 |
| `SA003` | 슈퍼 관리자만 수행할 수 있는 작업입니다 | 특정 액션 권한 부족 |
| `AL001` | 감사 로그를 찾을 수 없습니다 | 존재하지 않는 로그 ID |
| `B003` | 해당 매장에 접근 권한이 없습니다 | Business 도메인 권한 체크 실패 |

---

## 📈 향후 계획

### Phase 2: 고급 관리 기능 (예정)
- [ ] **Support Ticket**: 사용자 지원 시스템
- [ ] **Report**: 보고서 생성 (PDF, Excel)
- [ ] **System Config**: 시스템 설정 관리

### Phase 3: 데이터 관리 (예정)
- [ ] **Backup & Restore**: 데이터 백업 및 복원
- [ ] **Monitoring**: 시스템 모니터링 및 느린 쿼리 추적

---

## 📚 관련 문서

### 프론트엔드
- **[슈퍼 관리자 화면 구현 가이드](./superadmin_frontend_guide.md)** ⭐ 필독

### 백엔드
- [백엔드 API 문서](./04_api/README.md)
- [도메인 개발 패턴](./02_domain/development-pattern.md)
- [보안 구조](./01_architecture/security.md)
- [예외 처리](./01_architecture/exception-handling.md)

### 아키텍처
- [패키지 구조](./01_architecture/package-structure.md)
- [레이어 아키텍처](./01_architecture/layered-architecture.md)

---

## 💡 개발 팁

### 1. 권한 체크 패턴
```javascript
// 컴포넌트 레벨에서 권한 체크
if (!currentUser.isSuperAdmin()) {
  return <AccessDenied />;
}
```

### 2. API 호출 패턴
```javascript
// 서비스 레이어 사용 권장
import { superAdminService } from './services/superadmin';

const stats = await superAdminService.getSystemStats();
```

### 3. 에러 처리 패턴
```javascript
try {
  await superAdminService.deleteUser(userId);
} catch (error) {
  if (error.code === 'SA002') {
    alert('슈퍼 관리자는 삭제할 수 없습니다.');
  } else {
    alert(error.message);
  }
}
```

### 4. 페이지네이션 패턴
```javascript
const [page, setPage] = useState(1);
const [data, setData] = useState(null);

useEffect(() => {
  loadData(page);
}, [page]);
```

---

## 🎓 학습 자료

### React 프로젝트
1. `superadmin_frontend_guide.md` 전체 읽기
2. 샘플 코드 (8장) 참고하여 구현
3. API 명세 (4장) 보며 테스트

### Vue/Angular 프로젝트
1. API 명세 (4장) 참고
2. 화면 구조 (3장) 참고
3. 프레임워크에 맞게 컴포넌트 변환

---

## ✅ 체크리스트

프론트엔드 개발 시작 전:
- [ ] [프론트엔드 구현 가이드](./superadmin_frontend_guide.md) 읽기
- [ ] 초기 SUPER_ADMIN 계정으로 로그인 테스트
- [ ] API 엔드포인트 확인 (Swagger/Postman)
- [ ] 개발 환경 설정 (.env)

화면 구현:
- [ ] 레이아웃 및 사이드바
- [ ] 대시보드 (통계, 랭킹, 차트)
- [ ] 매장 관리 (목록, 검색, 삭제)
- [ ] 사용자 관리 (목록, 역할 변경, 정지, 삭제)
- [ ] 감사 로그 (목록, 상세)

배포 전:
- [ ] 모든 API 테스트
- [ ] 에러 처리 확인
- [ ] 권한 체크 확인
- [ ] 페이지네이션 테스트
- [ ] SUPER_ADMIN 보호 기능 테스트

---

## 🆘 지원

**문서 관련**:
- 프론트엔드: [superadmin_frontend_guide.md](./superadmin_frontend_guide.md)
- 백엔드: [04_api/README.md](./04_api/README.md)

**이슈 및 버그**:
- GitHub Issues
- 이메일: support@example.com

---

**문서 버전**: 1.0
**마지막 업데이트**: 2024-01-15
**구현 상태**: Phase 1 완료 ✅
