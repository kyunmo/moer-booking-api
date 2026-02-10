# 2026-02-10 감사로그 기능 적용 작업 기록

## 작업 개요

감사로그(Audit Log) 기능이 구현되어 있었으나 슈퍼 관리자 서비스에서만 사용되고, 일반 API에서는 호출되지 않아 기록이 되지 않는 문제를 해결했습니다.

## 작업 내용

### 1. AuditLogService 개선

**파일**: `src/main/java/io/moer/booking/domain/auditlog/service/AuditLogService.java`

- 기존: `AuditLogCreateRequest` 객체를 받는 `log()` 메서드만 존재
- 개선: `User` 객체와 파라미터로 직접 호출 가능한 간편 메서드 추가

```java
public void log(User user, AuditAction action, String entityType, Long entityId,
                String description, Map<String, Object> metadata)
```

### 2. Business Service에 감사로그 적용

**파일**: `src/main/java/io/moer/booking/domain/business/service/BusinessService.java`

**적용된 메서드**:
1. **createBusiness()** - 매장 생성 시 `BUSINESS_CREATED` 기록
   - 매장명, 업종, 오너 정보 메타데이터 포함

2. **updateBusiness()** - 매장 수정 시 `BUSINESS_UPDATED` 기록
   - 매장명 변경 시 이전/이후 값 메타데이터 포함

3. **deleteBusiness()** - 매장 삭제 시 `BUSINESS_DELETED` 기록
   - 삭제 전 매장 정보를 메타데이터로 기록

4. **changeBusinessStatus()** - 매장 상태 변경 시 `BUSINESS_STATUS_CHANGED` 기록
   - 이전/이후 상태 값 메타데이터 포함

**Controller 수정**:
- `BusinessController.createBusiness()` - `@AuthenticationPrincipal` 추가하여 현재 사용자 정보 전달

### 3. User Service에 감사로그 적용

**파일**: `src/main/java/io/moer/booking/domain/user/service/UserService.java`

**적용된 메서드**:
1. **createUser()** - 사용자 생성 시 `USER_CREATED` 기록
   - 사용자 이메일, 이름, 역할, 소속 매장 정보 메타데이터 포함

2. **updateUserStatus()** - 사용자 상태 변경 시 `USER_STATUS_CHANGED` 기록
   - 이전/이후 상태 값 메타데이터 포함

**Controller 수정**:
- `UserController.updateUserStatus()` - `@AuthenticationPrincipal` 추가하여 현재 사용자 정보 전달

### 4. Auth Service에 감사로그 적용

**파일**: `src/main/java/io/moer/booking/domain/auth/service/AuthService.java`

**적용된 메서드**:
1. **register()** - 회원가입 시 감사로그 2건 기록
   - `USER_CREATED`: 사용자 생성 이력 (이메일, 이름, 체험판 만료일 등)
   - `BUSINESS_CREATED`: 매장 생성 이력 (매장명, 업종, 오너 정보 등)

## 감사로그 기록 대상 정리

### 현재 적용된 API

| 도메인 | 액션 | AuditAction | 메서드 |
|--------|------|-------------|--------|
| **Auth** | 회원가입 | USER_CREATED, BUSINESS_CREATED | `register()` |
| **User** | 사용자 생성 | USER_CREATED | `createUser()` |
| **User** | 상태 변경 | USER_STATUS_CHANGED | `updateUserStatus()` |
| **Business** | 매장 생성 | BUSINESS_CREATED | `createBusiness()` |
| **Business** | 매장 수정 | BUSINESS_UPDATED | `updateBusiness()` |
| **Business** | 매장 삭제 | BUSINESS_DELETED | `deleteBusiness()` |
| **Business** | 상태 변경 | BUSINESS_STATUS_CHANGED | `changeBusinessStatus()` |
| **SuperAdmin** | 사용자 역할 변경 | USER_ROLE_CHANGED | `changeUserRole()` |
| **SuperAdmin** | 사용자 정지 | USER_STATUS_CHANGED | `suspendUser()` |
| **SuperAdmin** | 사용자 삭제 | USER_DELETED | `forceDeleteUser()` |
| **SuperAdmin** | 매장 삭제 | BUSINESS_DELETED | `forceDeleteBusiness()` |
| **SuperAdmin** | 매장 상태 일괄 변경 | BUSINESS_STATUS_CHANGED | `bulkUpdateStatus()` |

## 메타데이터 구조 예시

### 사용자 생성
```json
{
  "userEmail": "user@example.com",
  "userName": "홍길동",
  "userRole": "OWNER",
  "businessId": 123
}
```

### 매장 상태 변경
```json
{
  "businessName": "홍길동 미용실",
  "oldStatus": "ACTIVE",
  "newStatus": "INACTIVE"
}
```

### 회원가입
```json
{
  "userEmail": "newuser@example.com",
  "userName": "김철수",
  "businessName": "김철수 필라테스",
  "businessType": "PILATES",
  "trialExpiresAt": "2026-03-12T10:30:00"
}
```

## 기술적 세부사항

### AuditAction Enum
현재 정의된 액션 타입:
- `BUSINESS_CREATED`, `BUSINESS_UPDATED`, `BUSINESS_DELETED`, `BUSINESS_STATUS_CHANGED`
- `USER_CREATED`, `USER_ROLE_CHANGED`, `USER_STATUS_CHANGED`, `USER_DELETED`
- `SYSTEM_BACKUP`, `SYSTEM_RESTORE`, `SYSTEM_CONFIG_CHANGED`

### 데이터베이스 스키마
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
    metadata JSONB,
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### MyBatis 매퍼
- **파일**: `src/main/resources/mapper/auditlog/AuditLogMapper.xml`
- **주요 쿼리**:
  - `save`: 감사로그 저장 (JSONB 타입 핸들러 사용)
  - `findById`: ID로 조회
  - `findByCondition`: 검색 조건으로 조회 (페이징)
  - `countByCondition`: 검색 결과 개수 조회

## 테스트 방법

### 1. 회원가입 시 감사로그 확인
```bash
# 1. 회원가입 API 호출
POST /api/auth/register
{
  "email": "test@example.com",
  "password": "Test123!",
  "name": "테스트",
  "phone": "010-1234-5678",
  "businessName": "테스트 매장",
  "businessType": "SALON"
}

# 2. 감사로그 조회
GET /api/audit-logs?entityType=User&action=USER_CREATED
```

### 2. 매장 삭제 시 감사로그 확인
```bash
# 1. 매장 삭제 API 호출
DELETE /api/businesses/{id}
Authorization: Bearer {token}

# 2. 감사로그 조회
GET /api/audit-logs?entityType=Business&action=BUSINESS_DELETED
```

### 3. 슈퍼 관리자 작업 감사로그 확인
```bash
# 1. 사용자 역할 변경
PATCH /api/superadmin/users/{userId}/role
Authorization: Bearer {super_admin_token}

# 2. 감사로그 조회
GET /api/audit-logs?action=USER_ROLE_CHANGED
```

## 빌드 결과

```
BUILD SUCCESSFUL in 1m 31s
6 actionable tasks: 6 executed
```

모든 컴파일 에러 없이 정상 빌드 완료.

## 향후 개선 사항 (선택적)

### 1. 추가 도메인에 감사로그 적용 (우선순위 낮음)
- **Reservation** - 예약 생성/취소/완료
- **Staff** - 직원 생성/삭제
- **Service** - 서비스 메뉴 생성/삭제
- **Customer** - 고객 생성/삭제

### 2. IP 주소 및 User-Agent 자동 수집
현재는 `null`로 저장되고 있으며, HTTP 요청 정보를 자동으로 수집하려면:
- Spring `HttpServletRequest`를 AOP 또는 Interceptor로 처리
- `X-Forwarded-For` 헤더 처리 (Proxy/Load Balancer 환경)

```java
// 예시: Interceptor에서 처리
String ipAddress = request.getHeader("X-Forwarded-For");
if (ipAddress == null) {
    ipAddress = request.getRemoteAddr();
}
String userAgent = request.getHeader("User-Agent");
```

### 3. 감사로그 조회 필터 강화
현재 지원하는 필터:
- userId, action, entityType, startDate, endDate

추가 가능한 필터:
- userRole (특정 역할의 사용자 액션만 조회)
- businessId (특정 매장 관련 로그만 조회)
- ipAddress (특정 IP에서 발생한 액션만 조회)

### 4. 감사로그 백업 및 보관 정책
- 오래된 로그 아카이빙 (예: 1년 이상 된 로그는 별도 테이블로 이동)
- 로그 압축 및 외부 스토리지 저장
- 법적 요구사항에 따른 보관 기간 설정

## 참고 문서

- [감사로그 API 명세](../docs/04_api/README.md)
- [AuditAction Enum](../src/main/java/io/moer/booking/domain/auditlog/AuditAction.java)
- [감사로그 테이블 스키마](../src/main/resources/db/schema.sql#L372-L408)

---

**작업 완료일**: 2026-02-10
**담당**: Claude Code
**상태**: ✅ 완료
