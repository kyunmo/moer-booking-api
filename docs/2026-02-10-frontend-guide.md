# 2026-02-10 감사로그 기능 - 프론트엔드 작업 가이드

## 개요

백엔드에서 감사로그 기능이 구현 및 적용 완료되었습니다. 주요 API(회원가입, 사용자 생성/수정, 매장 생성/수정/삭제, 슈퍼 관리자 작업)에서 자동으로 감사로그를 기록합니다.

프론트엔드에서는 **기존 API 호출 방식을 변경할 필요가 없으며**, 감사로그는 백엔드에서 자동으로 기록됩니다.

## 기존 API 호출 영향 없음

### 변경 사항 없음 ✅
다음 API들은 **프론트엔드 코드 수정 없이** 그대로 사용 가능합니다:
- `POST /api/auth/register` - 회원가입
- `POST /api/businesses` - 매장 생성
- `PATCH /api/businesses/{id}` - 매장 수정
- `DELETE /api/businesses/{id}` - 매장 삭제
- `PATCH /api/businesses/{id}/status` - 매장 상태 변경
- `PATCH /api/users/{userId}/status` - 사용자 상태 변경
- 슈퍼 관리자 전체 API

**이유**: 감사로그는 백엔드에서 `@AuthenticationPrincipal`로 자동으로 현재 사용자 정보를 가져와서 기록하므로, 프론트엔드는 JWT 토큰만 헤더에 포함하여 요청하면 됩니다.

## 프론트엔드 작업 목록

### 1. 감사로그 조회 화면 추가 (선택적)

슈퍼 관리자 또는 매장 오너가 시스템 로그를 확인할 수 있는 화면을 추가할 수 있습니다.

#### API 엔드포인트
```
GET /api/audit-logs
```

#### 요청 파라미터
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| userId | Long | X | 특정 사용자의 로그만 조회 |
| action | String | X | 특정 액션 타입 필터 (예: USER_CREATED) |
| entityType | String | X | 엔티티 타입 필터 (예: Business, User) |
| startDate | LocalDate | X | 시작 날짜 (YYYY-MM-DD) |
| endDate | LocalDate | X | 종료 날짜 (YYYY-MM-DD) |
| page | int | X | 페이지 번호 (기본값: 1) |
| size | int | X | 페이지 크기 (기본값: 20) |

#### 응답 예시
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 123,
        "userId": 45,
        "userEmail": "owner@example.com",
        "userRole": "OWNER",
        "action": "BUSINESS_CREATED",
        "entityType": "Business",
        "entityId": 10,
        "description": "매장 생성: 홍길동 미용실 (업종: 미용실)",
        "metadata": {
          "businessName": "홍길동 미용실",
          "businessType": "SALON",
          "ownerId": 45,
          "ownerEmail": "owner@example.com"
        },
        "ipAddress": null,
        "userAgent": null,
        "createdAt": "2026-02-10T15:30:00"
      }
    ],
    "pageInfo": {
      "page": 1,
      "size": 20,
      "totalElements": 150,
      "totalPages": 8
    }
  }
}
```

#### 화면 구성 예시

**1) 필터 섹션**
```jsx
<div className="audit-log-filter">
  <DateRangePicker
    startDate={startDate}
    endDate={endDate}
    onChange={handleDateChange}
  />
  <Select
    placeholder="액션 타입"
    options={[
      { value: 'USER_CREATED', label: '사용자 생성' },
      { value: 'USER_DELETED', label: '사용자 삭제' },
      { value: 'BUSINESS_CREATED', label: '매장 생성' },
      { value: 'BUSINESS_DELETED', label: '매장 삭제' },
      { value: 'BUSINESS_STATUS_CHANGED', label: '매장 상태 변경' }
    ]}
    onChange={handleActionFilter}
  />
  <Select
    placeholder="엔티티 타입"
    options={[
      { value: 'User', label: '사용자' },
      { value: 'Business', label: '매장' }
    ]}
    onChange={handleEntityTypeFilter}
  />
</div>
```

**2) 테이블**
```jsx
<table className="audit-log-table">
  <thead>
    <tr>
      <th>시간</th>
      <th>사용자</th>
      <th>액션</th>
      <th>설명</th>
      <th>상세</th>
    </tr>
  </thead>
  <tbody>
    {logs.map(log => (
      <tr key={log.id}>
        <td>{formatDateTime(log.createdAt)}</td>
        <td>
          {log.userEmail} ({log.userRole})
        </td>
        <td>
          <Badge color={getActionColor(log.action)}>
            {getActionLabel(log.action)}
          </Badge>
        </td>
        <td>{log.description}</td>
        <td>
          <Button onClick={() => showLogDetail(log)}>
            상세보기
          </Button>
        </td>
      </tr>
    ))}
  </tbody>
</table>
```

**3) 상세 모달**
```jsx
<Modal isOpen={isDetailOpen} onClose={closeDetail}>
  <h2>감사로그 상세</h2>
  <div className="log-detail">
    <div className="detail-row">
      <span className="label">ID:</span>
      <span className="value">{selectedLog.id}</span>
    </div>
    <div className="detail-row">
      <span className="label">수행자:</span>
      <span className="value">
        {selectedLog.userEmail} ({selectedLog.userRole})
      </span>
    </div>
    <div className="detail-row">
      <span className="label">액션:</span>
      <span className="value">{selectedLog.action}</span>
    </div>
    <div className="detail-row">
      <span className="label">대상 엔티티:</span>
      <span className="value">
        {selectedLog.entityType} (ID: {selectedLog.entityId})
      </span>
    </div>
    <div className="detail-row">
      <span className="label">설명:</span>
      <span className="value">{selectedLog.description}</span>
    </div>
    <div className="detail-row">
      <span className="label">메타데이터:</span>
      <pre className="metadata">
        {JSON.stringify(selectedLog.metadata, null, 2)}
      </pre>
    </div>
    <div className="detail-row">
      <span className="label">시간:</span>
      <span className="value">
        {formatDateTime(selectedLog.createdAt)}
      </span>
    </div>
  </div>
</Modal>
```

### 2. 헬퍼 함수 예시

```typescript
// 액션 타입별 한글 레이블
export const getActionLabel = (action: string): string => {
  const labels: Record<string, string> = {
    'BUSINESS_CREATED': '매장 생성',
    'BUSINESS_UPDATED': '매장 수정',
    'BUSINESS_DELETED': '매장 삭제',
    'BUSINESS_STATUS_CHANGED': '매장 상태 변경',
    'USER_CREATED': '사용자 생성',
    'USER_ROLE_CHANGED': '사용자 역할 변경',
    'USER_STATUS_CHANGED': '사용자 상태 변경',
    'USER_DELETED': '사용자 삭제',
  };
  return labels[action] || action;
};

// 액션 타입별 색상
export const getActionColor = (action: string): string => {
  if (action.includes('CREATED')) return 'green';
  if (action.includes('DELETED')) return 'red';
  if (action.includes('CHANGED')) return 'blue';
  return 'gray';
};

// 날짜 포맷
export const formatDateTime = (dateTime: string): string => {
  return new Date(dateTime).toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};
```

### 3. API 호출 예시

```typescript
// 감사로그 목록 조회
export const fetchAuditLogs = async (params: AuditLogSearchParams) => {
  const response = await axios.get('/api/audit-logs', {
    params,
    headers: {
      Authorization: `Bearer ${getAccessToken()}`
    }
  });
  return response.data;
};

// 특정 사용자의 로그만 조회
export const fetchUserAuditLogs = async (userId: number) => {
  return fetchAuditLogs({ userId, page: 1, size: 20 });
};

// 특정 매장 관련 로그 조회
export const fetchBusinessAuditLogs = async (businessId: number) => {
  return fetchAuditLogs({
    entityType: 'Business',
    entityId: businessId,
    page: 1,
    size: 20
  });
};

// 최근 7일간의 로그 조회
export const fetchRecentAuditLogs = async () => {
  const endDate = new Date().toISOString().split('T')[0];
  const startDate = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
    .toISOString()
    .split('T')[0];

  return fetchAuditLogs({ startDate, endDate, page: 1, size: 50 });
};
```

### 4. 타입 정의 (TypeScript)

```typescript
export interface AuditLog {
  id: number;
  userId: number | null;
  userEmail: string;
  userRole: string;
  action: string;
  entityType: string;
  entityId: number | null;
  description: string;
  metadata: Record<string, any>;
  ipAddress: string | null;
  userAgent: string | null;
  createdAt: string;
}

export interface AuditLogSearchParams {
  userId?: number;
  action?: string;
  entityType?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

export interface AuditLogResponse {
  success: boolean;
  data: {
    content: AuditLog[];
    pageInfo: {
      page: number;
      size: number;
      totalElements: number;
      totalPages: number;
    };
  };
}
```

## 라우팅 예시

```typescript
// 슈퍼 관리자용 감사로그 화면
{
  path: '/superadmin/audit-logs',
  component: SuperAdminAuditLogPage,
  meta: { requiresAuth: true, role: 'SUPER_ADMIN' }
}

// 매장 오너용 감사로그 화면 (자신의 매장 관련 로그만)
{
  path: '/dashboard/audit-logs',
  component: BusinessAuditLogPage,
  meta: { requiresAuth: true, role: ['OWNER', 'ADMIN'] }
}
```

## 권한 제어

### 슈퍼 관리자 (SUPER_ADMIN)
- **전체 시스템 로그 조회 가능**
- 모든 사용자, 모든 매장의 감사로그 조회

### 매장 오너 (OWNER)
- **자신의 매장 관련 로그만 조회 가능**
- 백엔드에서 `businessId` 필터 자동 적용
- 본인이 수행한 액션과 본인 매장 관련 액션만 조회

### 일반 사용자 (STAFF)
- **감사로그 조회 불가**
- UI에서 메뉴 숨김 처리

## 테스트 시나리오

### 1. 회원가입 후 로그 확인
1. 회원가입 API 호출 (`POST /api/auth/register`)
2. 감사로그 조회 (`GET /api/audit-logs?action=USER_CREATED`)
3. 생성된 사용자의 로그가 2건 있는지 확인:
   - `USER_CREATED`: 사용자 생성 로그
   - `BUSINESS_CREATED`: 매장 생성 로그

### 2. 매장 삭제 후 로그 확인
1. 매장 삭제 API 호출 (`DELETE /api/businesses/{id}`)
2. 감사로그 조회 (`GET /api/audit-logs?action=BUSINESS_DELETED`)
3. 삭제된 매장 정보가 메타데이터에 포함되어 있는지 확인

### 3. 슈퍼 관리자 작업 후 로그 확인
1. 사용자 역할 변경 (`PATCH /api/superadmin/users/{userId}/role`)
2. 감사로그 조회 (`GET /api/audit-logs?action=USER_ROLE_CHANGED`)
3. 이전/이후 역할 값이 메타데이터에 포함되어 있는지 확인

## 주의사항

1. **JWT 토큰 필수**: 모든 API 요청 시 `Authorization: Bearer {token}` 헤더 필수
2. **권한 확인**: 감사로그 조회는 관리자 권한이 필요할 수 있음 (백엔드 정책에 따라)
3. **페이징 처리**: 감사로그는 많은 데이터가 쌓일 수 있으므로 페이징 필수
4. **날짜 필터**: 기간을 제한하여 조회하는 것을 권장 (성능 최적화)

## 향후 확장 가능성

### 1. 실시간 알림
- 중요한 액션(매장 삭제, 사용자 삭제 등) 발생 시 슬랙/이메일 알림
- WebSocket을 통한 실시간 로그 스트리밍

### 2. 로그 분석
- 액션별 통계 차트 (예: 일별 회원가입 수, 매장 생성 수)
- 사용자별 활동 히스토리
- 이상 행동 감지 (예: 짧은 시간에 다수 매장 삭제)

### 3. 로그 내보내기
- CSV/Excel 다운로드
- PDF 리포트 생성

## 참고 자료

- [백엔드 작업 기록](./2026-02-10-audit-log-implementation.md)
- [감사로그 API 명세](../docs/04_api/README.md)
- [AuditAction 타입 목록](../src/main/java/io/moer/booking/domain/auditlog/AuditAction.java)

---

**작성일**: 2026-02-10
**상태**: 📝 프론트엔드 작업 대기 중
