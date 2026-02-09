# 슈퍼 관리자 화면 구현 가이드

> **작성일**: 2024
> **대상**: 프론트엔드 개발자
> **백엔드 API 버전**: v1.0
> **기술 스택**: React/Vue/Angular (범용)

---

## 📋 목차

1. [개요](#1-개요)
2. [인증 및 권한](#2-인증-및-권한)
3. [화면 구조](#3-화면-구조)
4. [API 명세](#4-api-명세)
5. [화면별 구현 가이드](#5-화면별-구현-가이드)
6. [컴포넌트 설계](#6-컴포넌트-설계)
7. [에러 처리](#7-에러-처리)
8. [샘플 코드](#8-샘플-코드)

---

## 1. 개요

### 1.1 슈퍼 관리자란?

**슈퍼 관리자(SUPER_ADMIN)**는 moer-booking 시스템 전체를 관리할 수 있는 최상위 권한입니다.

**권한 계층**:
```
SUPER_ADMIN (슈퍼 관리자)
  └─ 모든 매장 조회/수정/삭제
  └─ 모든 사용자 관리 (역할 변경, 정지, 삭제)
  └─ 시스템 통계 조회
  └─ 감사 로그 조회

ADMIN (시스템 관리자)
  └─ 모든 매장 조회

OWNER (매장 사장님)
  └─ 자신의 매장만 관리

STAFF (직원)
  └─ 자신의 매장만 조회
```

### 1.2 주요 기능

| 기능 | 설명 |
|------|------|
| **대시보드** | 시스템 전체 통계 (매장 수, 사용자 수, 매출 등) |
| **매장 관리** | 전체 매장 조회/검색/삭제/상태 변경 |
| **사용자 관리** | 전체 사용자 조회/역할 변경/정지/삭제 |
| **감사 로그** | 중요 액션 이력 조회 (삭제, 역할 변경 등) |
| **매출 랭킹** | 매장별 매출 순위 |
| **업종별 통계** | 업종별 매장 수 및 매출 |

---

## 2. 인증 및 권한

### 2.1 로그인

**초기 계정**:
```
Email: superadmin@moer.io
Password: Admin123!
```

**로그인 API**:
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "superadmin@moer.io",
  "password": "Admin123!"
}
```

**응답**:
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "email": "superadmin@moer.io",
      "name": "시스템 관리자",
      "role": "SUPER_ADMIN",
      "status": "ACTIVE"
    }
  }
}
```

### 2.2 JWT 토큰 사용

모든 API 요청 시 헤더에 JWT 토큰 포함:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### 2.3 권한 체크

프론트엔드에서 현재 사용자의 `role`을 확인하여 슈퍼 관리자 메뉴 표시:

```javascript
const user = getCurrentUser();

if (user.role === 'SUPER_ADMIN') {
  // 슈퍼 관리자 메뉴 표시
  showSuperAdminMenu();
} else {
  // 일반 메뉴만 표시
  showNormalMenu();
}
```

**403 Forbidden 에러 처리**:
- SUPER_ADMIN이 아닌 사용자가 슈퍼 관리자 API 호출 시 `403 Forbidden` 응답
- 에러 코드: `SA001` (SUPER_ADMIN_REQUIRED)

---

## 3. 화면 구조

### 3.1 메뉴 구조

```
슈퍼 관리자 (SUPER_ADMIN 전용)
├── 대시보드
│   ├── 시스템 통계
│   ├── 매출 랭킹
│   └── 업종별 통계
├── 매장 관리
│   ├── 전체 매장 목록
│   ├── 매장 상세
│   └── 매장 삭제
├── 사용자 관리
│   ├── 전체 사용자 목록
│   ├── 역할 변경
│   ├── 사용자 정지
│   └── 사용자 삭제
└── 감사 로그
    ├── 로그 목록
    └── 로그 상세
```

### 3.2 라우팅 예시 (React Router)

```javascript
// routes/superadmin.js
const superAdminRoutes = [
  {
    path: '/superadmin',
    element: <SuperAdminLayout />,
    children: [
      { path: 'dashboard', element: <Dashboard /> },
      { path: 'businesses', element: <BusinessList /> },
      { path: 'businesses/:id', element: <BusinessDetail /> },
      { path: 'users', element: <UserList /> },
      { path: 'users/:id', element: <UserDetail /> },
      { path: 'audit-logs', element: <AuditLogList /> },
      { path: 'audit-logs/:id', element: <AuditLogDetail /> },
    ]
  }
];
```

### 3.3 레이아웃

```
┌─────────────────────────────────────────────────────────┐
│  Header (로고, 사용자 정보, 로그아웃)                      │
├──────────┬──────────────────────────────────────────────┤
│          │                                              │
│  Sidebar │  Main Content                                │
│          │                                              │
│  - 대시보드│  (각 화면의 내용)                             │
│  - 매장   │                                              │
│  - 사용자 │                                              │
│  - 로그   │                                              │
│          │                                              │
└──────────┴──────────────────────────────────────────────┘
```

---

## 4. API 명세

### 4.1 공통 응답 형식

**성공 응답**:
```json
{
  "success": true,
  "data": { /* 실제 데이터 */ },
  "timestamp": "2024-01-15T10:30:00"
}
```

**에러 응답**:
```json
{
  "success": false,
  "error": {
    "code": "SA001",
    "message": "슈퍼 관리자 권한이 필요합니다",
    "details": null
  },
  "timestamp": "2024-01-15T10:30:00"
}
```

**페이징 응답**:
```json
{
  "success": true,
  "data": {
    "content": [ /* 데이터 배열 */ ],
    "page": 1,
    "size": 20,
    "totalElements": 150,
    "totalPages": 8,
    "first": true,
    "last": false
  }
}
```

### 4.2 대시보드 API

#### 4.2.1 시스템 통계 조회

```http
GET /api/superadmin/dashboard/stats
Authorization: Bearer {token}
```

**응답**:
```json
{
  "success": true,
  "data": {
    "totalBusinesses": 245,
    "activeBusinesses": 220,
    "inactiveBusinesses": 15,
    "suspendedBusinesses": 10,
    "totalUsers": 1250,
    "superAdminCount": 2,
    "adminCount": 5,
    "ownerCount": 245,
    "staffCount": 998,
    "totalReservationsToday": 342,
    "totalRevenueToday": 15420000,
    "totalRevenueThisMonth": 456780000,
    "newBusinessesThisMonth": 12,
    "newUsersThisMonth": 78
  }
}
```

#### 4.2.2 매출 랭킹 조회

```http
GET /api/superadmin/dashboard/business-ranking?startDate=2024-01-01&endDate=2024-01-31&limit=10
Authorization: Bearer {token}
```

**파라미터**:
- `startDate`: 시작일 (yyyy-MM-dd)
- `endDate`: 종료일 (yyyy-MM-dd)
- `limit`: 조회 개수 (기본값: 10)

**응답**:
```json
{
  "success": true,
  "data": [
    {
      "businessId": 15,
      "businessName": "강남 헤어살롱",
      "ownerName": "김사장",
      "totalRevenue": 45600000,
      "reservationCount": 234,
      "rank": 1
    },
    {
      "businessId": 42,
      "businessName": "신논현 필라테스",
      "ownerName": "이원장",
      "totalRevenue": 38900000,
      "reservationCount": 189,
      "rank": 2
    }
  ]
}
```

#### 4.2.3 업종별 통계 조회

```http
GET /api/superadmin/dashboard/stats-by-type
Authorization: Bearer {token}
```

**응답**:
```json
{
  "success": true,
  "data": [
    {
      "businessType": "SALON",
      "count": 120,
      "totalRevenue": 567800000
    },
    {
      "businessType": "PILATES",
      "count": 85,
      "totalRevenue": 423500000
    },
    {
      "businessType": "STUDY_CAFE",
      "count": 40,
      "totalRevenue": 128900000
    }
  ]
}
```

### 4.3 매장 관리 API

#### 4.3.1 전체 매장 목록 조회

```http
GET /api/superadmin/businesses?page=1&size=20&keyword=강남&status=ACTIVE
Authorization: Bearer {token}
```

**파라미터**:
- `page`: 페이지 번호 (1부터 시작, 기본값: 1)
- `size`: 페이지당 개수 (기본값: 20)
- `keyword`: 검색어 (매장명)
- `businessType`: 업종 (SALON, PILATES, STUDY_CAFE)
- `status`: 상태 (ACTIVE, INACTIVE, SUSPENDED)

**응답**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 15,
        "ownerId": 23,
        "name": "강남 헤어살롱",
        "businessType": "SALON",
        "phone": "02-1234-5678",
        "address": "서울시 강남구 테헤란로 123",
        "status": "ACTIVE",
        "createdAt": "2024-01-05T10:30:00",
        "updatedAt": "2024-01-15T14:20:00"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 245,
    "totalPages": 13,
    "first": true,
    "last": false
  }
}
```

#### 4.3.2 매장 강제 삭제

```http
DELETE /api/superadmin/businesses/{id}?hard=false
Authorization: Bearer {token}
```

**파라미터**:
- `hard`: 하드 삭제 여부 (기본값: false)
  - `false`: 매장 정보만 삭제 (Settings는 유지)
  - `true`: 매장 및 관련 모든 데이터 삭제

**응답**:
```json
{
  "success": true,
  "data": null
}
```

**⚠️ 주의**:
- 하드 삭제 시 복구 불가능
- 삭제 시 감사 로그에 자동 기록

#### 4.3.3 매장 상태 일괄 변경

```http
PATCH /api/superadmin/businesses/bulk-status
Authorization: Bearer {token}
Content-Type: application/json

{
  "businessIds": [15, 23, 42],
  "status": "SUSPENDED"
}
```

**요청 본문**:
- `businessIds`: 매장 ID 배열
- `status`: 변경할 상태 (ACTIVE, INACTIVE, SUSPENDED)

**응답**:
```json
{
  "success": true,
  "data": null
}
```

### 4.4 사용자 관리 API

#### 4.4.1 전체 사용자 목록 조회

```http
GET /api/superadmin/users?page=1&size=20&keyword=김&role=OWNER
Authorization: Bearer {token}
```

**파라미터**:
- `page`: 페이지 번호
- `size`: 페이지당 개수
- `keyword`: 검색어 (이름, 이메일)
- `role`: 역할 (SUPER_ADMIN, ADMIN, OWNER, STAFF)
- `status`: 상태 (ACTIVE, INACTIVE, SUSPENDED)
- `businessId`: 매장 ID

**응답**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 23,
        "email": "owner@example.com",
        "name": "김사장",
        "phone": "010-1234-5678",
        "role": "OWNER",
        "status": "ACTIVE",
        "businessId": 15,
        "emailVerified": "Y",
        "createdAt": "2024-01-05T10:30:00"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 1250,
    "totalPages": 63
  }
}
```

#### 4.4.2 사용자 역할 변경

```http
PATCH /api/superadmin/users/{id}/role
Authorization: Bearer {token}
Content-Type: application/json

{
  "role": "ADMIN"
}
```

**요청 본문**:
- `role`: 변경할 역할 (SUPER_ADMIN, ADMIN, OWNER, STAFF)

**응답**:
```json
{
  "success": true,
  "data": {
    "id": 23,
    "email": "user@example.com",
    "name": "김사장",
    "role": "ADMIN",
    "status": "ACTIVE"
  }
}
```

#### 4.4.3 사용자 정지

```http
PATCH /api/superadmin/users/{id}/suspend
Authorization: Bearer {token}
```

**응답**:
```json
{
  "success": true,
  "data": {
    "id": 23,
    "status": "SUSPENDED"
  }
}
```

**⚠️ 제약사항**:
- SUPER_ADMIN 계정은 정지할 수 없음
- 에러 코드: `SA002` (SUPER_ADMIN_CANNOT_BE_DELETED)

#### 4.4.4 사용자 강제 삭제

```http
DELETE /api/superadmin/users/{id}
Authorization: Bearer {token}
```

**응답**:
```json
{
  "success": true,
  "data": null
}
```

**⚠️ 제약사항**:
- SUPER_ADMIN 계정은 삭제할 수 없음
- 삭제 시 복구 불가능
- 감사 로그에 자동 기록

### 4.5 감사 로그 API

#### 4.5.1 감사 로그 목록 조회

```http
GET /api/audit-logs?page=1&size=20&action=USER_DELETED&startDate=2024-01-01&endDate=2024-01-31
Authorization: Bearer {token}
```

**파라미터**:
- `page`: 페이지 번호
- `size`: 페이지당 개수
- `userId`: 액션 수행 사용자 ID
- `action`: 액션 타입
  - `BUSINESS_CREATED`, `BUSINESS_UPDATED`, `BUSINESS_DELETED`, `BUSINESS_STATUS_CHANGED`
  - `USER_CREATED`, `USER_ROLE_CHANGED`, `USER_STATUS_CHANGED`, `USER_DELETED`
  - `SYSTEM_BACKUP`, `SYSTEM_RESTORE`, `SYSTEM_CONFIG_CHANGED`
- `entityType`: 대상 엔티티 타입 (Business, User, etc.)
- `startDate`: 시작일 (yyyy-MM-dd)
- `endDate`: 종료일 (yyyy-MM-dd)

**응답**:
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1523,
        "userId": 1,
        "userEmail": "superadmin@moer.io",
        "userRole": "SUPER_ADMIN",
        "action": "USER_DELETED",
        "entityType": "User",
        "entityId": 45,
        "description": "사용자 강제 삭제",
        "metadata": {
          "userEmail": "deleted@example.com",
          "userRole": "OWNER",
          "userStatus": "ACTIVE"
        },
        "ipAddress": "127.0.0.1",
        "userAgent": "Mozilla/5.0...",
        "createdAt": "2024-01-15T14:25:30"
      }
    ],
    "page": 1,
    "size": 20,
    "totalElements": 3450,
    "totalPages": 173
  }
}
```

#### 4.5.2 감사 로그 상세 조회

```http
GET /api/audit-logs/{id}
Authorization: Bearer {token}
```

**응답**:
```json
{
  "success": true,
  "data": {
    "id": 1523,
    "userId": 1,
    "userEmail": "superadmin@moer.io",
    "userRole": "SUPER_ADMIN",
    "action": "USER_ROLE_CHANGED",
    "entityType": "User",
    "entityId": 23,
    "description": "사용자 역할 변경: OWNER -> ADMIN",
    "metadata": {
      "userEmail": "user@example.com",
      "oldRole": "OWNER",
      "newRole": "ADMIN"
    },
    "ipAddress": "127.0.0.1",
    "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)...",
    "createdAt": "2024-01-15T14:25:30"
  }
}
```

---

## 5. 화면별 구현 가이드

### 5.1 대시보드 화면

#### 5.1.1 화면 구성

```
┌─────────────────────────────────────────────────────────┐
│  슈퍼 관리자 대시보드                                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐          │
│  │ 전체 매장  │  │ 활성 매장  │  │ 전체 사용자 │          │
│  │   245     │  │   220     │  │  1,250    │          │
│  └───────────┘  └───────────┘  └───────────┘          │
│                                                         │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐          │
│  │ 오늘 예약  │  │ 오늘 매출  │  │ 이번 달 매출│          │
│  │   342건   │  │ 15.4백만원 │  │ 456.8백만원│          │
│  └───────────┘  └───────────┘  └───────────┘          │
│                                                         │
│  [ 매출 랭킹 TOP 10 ]                                   │
│  ┌─────────────────────────────────────────────────┐   │
│  │ 1. 강남 헤어살롱        45.6백만원   (234건)    │   │
│  │ 2. 신논현 필라테스      38.9백만원   (189건)    │   │
│  │ 3. 역삼 스터디카페      32.1백만원   (512건)    │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  [ 업종별 통계 ]                                        │
│  ┌─────────────────────────────────────────────────┐   │
│  │ 미용실: 120개 (567.8백만원)                      │   │
│  │ 필라테스: 85개 (423.5백만원)                     │   │
│  │ 스터디카페: 40개 (128.9백만원)                   │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 5.1.2 구현 포인트

**데이터 로딩**:
```javascript
// 대시보드 화면 진입 시 실행
async function loadDashboard() {
  try {
    // 1. 시스템 통계 로드
    const statsResponse = await fetch('/api/superadmin/dashboard/stats', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const stats = await statsResponse.json();

    // 2. 매출 랭킹 로드 (최근 30일)
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 30);

    const rankingResponse = await fetch(
      `/api/superadmin/dashboard/business-ranking?` +
      `startDate=${formatDate(startDate)}&` +
      `endDate=${formatDate(endDate)}&limit=10`,
      { headers: { 'Authorization': `Bearer ${token}` } }
    );
    const ranking = await rankingResponse.json();

    // 3. 업종별 통계 로드
    const typeStatsResponse = await fetch('/api/superadmin/dashboard/stats-by-type', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const typeStats = await typeStatsResponse.json();

    // 4. 화면 렌더링
    renderDashboard(stats.data, ranking.data, typeStats.data);

  } catch (error) {
    handleError(error);
  }
}
```

**통계 카드 컴포넌트**:
```jsx
// StatCard.jsx (React 예시)
function StatCard({ title, value, icon, color }) {
  return (
    <div className={`stat-card ${color}`}>
      <div className="stat-icon">{icon}</div>
      <div className="stat-content">
        <h3>{title}</h3>
        <p className="stat-value">{value.toLocaleString()}</p>
      </div>
    </div>
  );
}

// 사용 예시
<StatCard title="전체 매장" value={245} icon="🏪" color="blue" />
<StatCard title="오늘 매출" value={15420000} icon="💰" color="green" />
```

**차트 라이브러리 추천**:
- Chart.js
- Recharts (React)
- ApexCharts

### 5.2 매장 관리 화면

#### 5.2.1 화면 구성

```
┌─────────────────────────────────────────────────────────┐
│  매장 관리                                               │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  [ 검색 ]                                               │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  [검색 버튼]   │
│  │ 키워드   │  │ 업종     │  │ 상태     │               │
│  └─────────┘  └─────────┘  └─────────┘               │
│                                                         │
│  [ 매장 목록 ]                              [일괄 작업▼]│
│  ┌─────────────────────────────────────────────────┐   │
│  │ □ ID  매장명        업종    상태    사장님   생성일 │   │
│  ├─────────────────────────────────────────────────┤   │
│  │ □ 15  강남헤어살롱   미용실  활성    김사장  2024-01│   │
│  │ □ 23  신논현필라...  필라테스 활성   이원장  2024-01│   │
│  │ □ 42  역삼스터디...  카페    비활성  박대표  2024-01│   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  [ 페이지네이션 ]  ◀ 1 2 3 4 5 ▶                       │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 5.2.2 구현 포인트

**검색 및 필터링**:
```javascript
// 매장 목록 조회
async function loadBusinesses(filters, page = 1) {
  const params = new URLSearchParams({
    page: page,
    size: 20,
    ...(filters.keyword && { keyword: filters.keyword }),
    ...(filters.businessType && { businessType: filters.businessType }),
    ...(filters.status && { status: filters.status })
  });

  const response = await fetch(
    `/api/superadmin/businesses?${params}`,
    { headers: { 'Authorization': `Bearer ${token}` } }
  );

  return await response.json();
}

// 검색 폼 제출
function handleSearch(event) {
  event.preventDefault();
  const filters = {
    keyword: document.getElementById('keyword').value,
    businessType: document.getElementById('businessType').value,
    status: document.getElementById('status').value
  };

  loadBusinesses(filters, 1);
}
```

**테이블 컴포넌트**:
```jsx
// BusinessTable.jsx
function BusinessTable({ businesses, onDelete, onStatusChange }) {
  return (
    <table>
      <thead>
        <tr>
          <th><input type="checkbox" onChange={handleSelectAll} /></th>
          <th>ID</th>
          <th>매장명</th>
          <th>업종</th>
          <th>상태</th>
          <th>사장님</th>
          <th>생성일</th>
          <th>액션</th>
        </tr>
      </thead>
      <tbody>
        {businesses.map(business => (
          <tr key={business.id}>
            <td><input type="checkbox" value={business.id} /></td>
            <td>{business.id}</td>
            <td>{business.name}</td>
            <td>{getBusinessTypeLabel(business.businessType)}</td>
            <td>
              <StatusBadge status={business.status} />
            </td>
            <td>{business.ownerName}</td>
            <td>{formatDate(business.createdAt)}</td>
            <td>
              <button onClick={() => onDelete(business.id)}>삭제</button>
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
```

**삭제 확인 다이얼로그**:
```javascript
async function handleDelete(businessId) {
  // 확인 다이얼로그 표시
  const confirmed = await showConfirmDialog({
    title: '매장 삭제',
    message: '정말로 이 매장을 삭제하시겠습니까?',
    options: [
      { label: '소프트 삭제', value: 'soft' },
      { label: '하드 삭제 (복구 불가)', value: 'hard', danger: true }
    ]
  });

  if (!confirmed) return;

  try {
    const hard = confirmed === 'hard';
    await fetch(`/api/superadmin/businesses/${businessId}?hard=${hard}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token}` }
    });

    showSuccessMessage('매장이 삭제되었습니다.');
    loadBusinesses(); // 목록 새로고침

  } catch (error) {
    showErrorMessage(error.message);
  }
}
```

**일괄 상태 변경**:
```javascript
async function handleBulkStatusChange() {
  const checkedIds = getCheckedBusinessIds();

  if (checkedIds.length === 0) {
    showWarningMessage('매장을 선택해주세요.');
    return;
  }

  const status = await showStatusSelectDialog();

  if (!status) return;

  try {
    await fetch('/api/superadmin/businesses/bulk-status', {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        businessIds: checkedIds,
        status: status
      })
    });

    showSuccessMessage(`${checkedIds.length}개 매장의 상태가 변경되었습니다.`);
    loadBusinesses();

  } catch (error) {
    showErrorMessage(error.message);
  }
}
```

### 5.3 사용자 관리 화면

#### 5.3.1 화면 구성

```
┌─────────────────────────────────────────────────────────┐
│  사용자 관리                                             │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  [ 검색 ]                                               │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  [검색 버튼]   │
│  │ 키워드   │  │ 역할     │  │ 상태     │               │
│  └─────────┘  └─────────┘  └─────────┘               │
│                                                         │
│  [ 사용자 목록 ]                                        │
│  ┌─────────────────────────────────────────────────┐   │
│  │ ID  이메일            이름    역할    상태   액션  │   │
│  ├─────────────────────────────────────────────────┤   │
│  │ 23  owner@ex.com      김사장  OWNER   활성  [▼]  │   │
│  │ 45  staff@ex.com      이직원  STAFF   활성  [▼]  │   │
│  │ 67  user@ex.com       박매니저 OWNER  정지  [▼]  │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  [ 페이지네이션 ]  ◀ 1 2 3 4 5 ▶                       │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 5.3.2 액션 드롭다운 메뉴

각 사용자 행의 액션 버튼 클릭 시 표시:
```
┌──────────────┐
│ 역할 변경     │
│ 사용자 정지   │
│ 사용자 삭제   │
└──────────────┘
```

#### 5.3.3 구현 포인트

**역할 변경 다이얼로그**:
```javascript
async function handleRoleChange(userId) {
  const newRole = await showRoleSelectDialog({
    title: '역할 변경',
    options: [
      { value: 'SUPER_ADMIN', label: '슈퍼 관리자' },
      { value: 'ADMIN', label: '시스템 관리자' },
      { value: 'OWNER', label: '매장 사장님' },
      { value: 'STAFF', label: '직원' }
    ]
  });

  if (!newRole) return;

  try {
    await fetch(`/api/superadmin/users/${userId}/role`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ role: newRole })
    });

    showSuccessMessage('역할이 변경되었습니다.');
    loadUsers();

  } catch (error) {
    if (error.code === 'SA002') {
      showErrorMessage('슈퍼 관리자는 역할을 변경할 수 없습니다.');
    } else {
      showErrorMessage(error.message);
    }
  }
}
```

**사용자 정지**:
```javascript
async function handleSuspend(userId) {
  const confirmed = await showConfirmDialog({
    title: '사용자 정지',
    message: '이 사용자를 정지하시겠습니까?',
    confirmText: '정지',
    cancelText: '취소',
    danger: true
  });

  if (!confirmed) return;

  try {
    await fetch(`/api/superadmin/users/${userId}/suspend`, {
      method: 'PATCH',
      headers: { 'Authorization': `Bearer ${token}` }
    });

    showSuccessMessage('사용자가 정지되었습니다.');
    loadUsers();

  } catch (error) {
    if (error.code === 'SA002') {
      showErrorMessage('슈퍼 관리자는 정지할 수 없습니다.');
    } else {
      showErrorMessage(error.message);
    }
  }
}
```

**사용자 삭제**:
```javascript
async function handleDeleteUser(userId) {
  // 2단계 확인
  const firstConfirm = await showConfirmDialog({
    title: '사용자 삭제',
    message: '이 사용자를 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다.',
    confirmText: '다음',
    cancelText: '취소',
    danger: true
  });

  if (!firstConfirm) return;

  const secondConfirm = await showConfirmDialog({
    title: '최종 확인',
    message: '정말로 삭제하시겠습니까? "DELETE"를 입력하세요.',
    requireInput: true,
    expectedInput: 'DELETE',
    confirmText: '삭제',
    cancelText: '취소',
    danger: true
  });

  if (!secondConfirm) return;

  try {
    await fetch(`/api/superadmin/users/${userId}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token}` }
    });

    showSuccessMessage('사용자가 삭제되었습니다.');
    loadUsers();

  } catch (error) {
    if (error.code === 'SA002') {
      showErrorMessage('슈퍼 관리자는 삭제할 수 없습니다.');
    } else {
      showErrorMessage(error.message);
    }
  }
}
```

### 5.4 감사 로그 화면

#### 5.4.1 화면 구성

```
┌─────────────────────────────────────────────────────────┐
│  감사 로그                                               │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  [ 필터 ]                                               │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  [검색 버튼]   │
│  │ 액션타입 │  │ 시작일   │  │ 종료일   │               │
│  └─────────┘  └─────────┘  └─────────┘               │
│                                                         │
│  [ 로그 목록 ]                                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │ 시간          사용자         액션          대상    │   │
│  ├─────────────────────────────────────────────────┤   │
│  │ 2024-01-15   superadmin@    USER_DELETED  User #45│ │
│  │ 14:25:30     moer.io                             │   │
│  │                                                   │   │
│  │ 2024-01-15   admin@moer.io  BUSINESS_     Business│ │
│  │ 10:15:20                    STATUS_CHANGED   #23 │   │
│  └─────────────────────────────────────────────────┘   │
│                                                         │
│  [ 페이지네이션 ]  ◀ 1 2 3 4 5 ▶                       │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

#### 5.4.2 로그 상세 모달

로그 행 클릭 시 상세 정보 표시:

```
┌─────────────────────────────────────────────────────────┐
│  감사 로그 상세                                    [X]   │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  로그 ID: 1523                                          │
│  시간: 2024-01-15 14:25:30                              │
│                                                         │
│  [ 수행자 정보 ]                                        │
│  - 사용자 ID: 1                                         │
│  - 이메일: superadmin@moer.io                           │
│  - 역할: SUPER_ADMIN                                    │
│                                                         │
│  [ 액션 정보 ]                                          │
│  - 액션: USER_ROLE_CHANGED (사용자 역할 변경)           │
│  - 대상 타입: User                                      │
│  - 대상 ID: 23                                          │
│  - 설명: 사용자 역할 변경: OWNER -> ADMIN               │
│                                                         │
│  [ 변경 내역 ]                                          │
│  {                                                      │
│    "userEmail": "user@example.com",                     │
│    "oldRole": "OWNER",                                  │
│    "newRole": "ADMIN"                                   │
│  }                                                      │
│                                                         │
│  [ 요청 정보 ]                                          │
│  - IP: 127.0.0.1                                        │
│  - User Agent: Mozilla/5.0 (Windows NT 10.0...)        │
│                                                         │
│  [닫기]                                                 │
└─────────────────────────────────────────────────────────┘
```

#### 5.4.3 구현 포인트

**액션 타입 필터 옵션**:
```javascript
const actionTypes = [
  { value: '', label: '전체' },
  { value: 'BUSINESS_CREATED', label: '매장 생성' },
  { value: 'BUSINESS_UPDATED', label: '매장 수정' },
  { value: 'BUSINESS_DELETED', label: '매장 삭제' },
  { value: 'BUSINESS_STATUS_CHANGED', label: '매장 상태 변경' },
  { value: 'USER_CREATED', label: '사용자 생성' },
  { value: 'USER_ROLE_CHANGED', label: '사용자 역할 변경' },
  { value: 'USER_STATUS_CHANGED', label: '사용자 상태 변경' },
  { value: 'USER_DELETED', label: '사용자 삭제' },
  { value: 'SYSTEM_BACKUP', label: '시스템 백업' },
  { value: 'SYSTEM_RESTORE', label: '시스템 복원' },
  { value: 'SYSTEM_CONFIG_CHANGED', label: '시스템 설정 변경' }
];
```

**로그 조회**:
```javascript
async function loadAuditLogs(filters, page = 1) {
  const params = new URLSearchParams({
    page: page,
    size: 20,
    ...(filters.action && { action: filters.action }),
    ...(filters.entityType && { entityType: filters.entityType }),
    ...(filters.startDate && { startDate: filters.startDate }),
    ...(filters.endDate && { endDate: filters.endDate })
  });

  const response = await fetch(
    `/api/audit-logs?${params}`,
    { headers: { 'Authorization': `Bearer ${token}` } }
  );

  return await response.json();
}
```

**메타데이터 포맷팅**:
```javascript
function formatMetadata(metadata) {
  if (!metadata) return '없음';

  // JSON을 보기 좋게 포맷팅
  return (
    <pre>
      {JSON.stringify(metadata, null, 2)}
    </pre>
  );
}
```

---

## 6. 컴포넌트 설계

### 6.1 공통 컴포넌트

#### 6.1.1 레이아웃 컴포넌트

```jsx
// components/SuperAdminLayout.jsx
function SuperAdminLayout({ children }) {
  const user = useCurrentUser();

  // SUPER_ADMIN이 아니면 접근 불가
  if (user.role !== 'SUPER_ADMIN') {
    return <Navigate to="/access-denied" />;
  }

  return (
    <div className="super-admin-layout">
      <Header user={user} />
      <div className="layout-body">
        <Sidebar />
        <main className="main-content">
          {children}
        </main>
      </div>
    </div>
  );
}
```

#### 6.1.2 사이드바 컴포넌트

```jsx
// components/Sidebar.jsx
function Sidebar() {
  const location = useLocation();

  const menuItems = [
    { path: '/superadmin/dashboard', icon: '📊', label: '대시보드' },
    { path: '/superadmin/businesses', icon: '🏪', label: '매장 관리' },
    { path: '/superadmin/users', icon: '👥', label: '사용자 관리' },
    { path: '/superadmin/audit-logs', icon: '📋', label: '감사 로그' }
  ];

  return (
    <nav className="sidebar">
      {menuItems.map(item => (
        <Link
          key={item.path}
          to={item.path}
          className={location.pathname === item.path ? 'active' : ''}
        >
          <span className="icon">{item.icon}</span>
          <span className="label">{item.label}</span>
        </Link>
      ))}
    </nav>
  );
}
```

#### 6.1.3 페이지네이션 컴포넌트

```jsx
// components/Pagination.jsx
function Pagination({ page, totalPages, onPageChange }) {
  const getPageNumbers = () => {
    const pages = [];
    const maxVisible = 5;

    let start = Math.max(1, page - Math.floor(maxVisible / 2));
    let end = Math.min(totalPages, start + maxVisible - 1);

    if (end - start < maxVisible - 1) {
      start = Math.max(1, end - maxVisible + 1);
    }

    for (let i = start; i <= end; i++) {
      pages.push(i);
    }

    return pages;
  };

  return (
    <div className="pagination">
      <button
        onClick={() => onPageChange(page - 1)}
        disabled={page === 1}
      >
        이전
      </button>

      {getPageNumbers().map(num => (
        <button
          key={num}
          className={num === page ? 'active' : ''}
          onClick={() => onPageChange(num)}
        >
          {num}
        </button>
      ))}

      <button
        onClick={() => onPageChange(page + 1)}
        disabled={page === totalPages}
      >
        다음
      </button>
    </div>
  );
}
```

#### 6.1.4 상태 뱃지 컴포넌트

```jsx
// components/StatusBadge.jsx
function StatusBadge({ status }) {
  const statusConfig = {
    'ACTIVE': { label: '활성', color: 'green' },
    'INACTIVE': { label: '비활성', color: 'gray' },
    'SUSPENDED': { label: '정지', color: 'red' }
  };

  const config = statusConfig[status] || { label: status, color: 'gray' };

  return (
    <span className={`badge badge-${config.color}`}>
      {config.label}
    </span>
  );
}
```

#### 6.1.5 확인 다이얼로그 컴포넌트

```jsx
// components/ConfirmDialog.jsx
function ConfirmDialog({
  open,
  title,
  message,
  confirmText = '확인',
  cancelText = '취소',
  onConfirm,
  onCancel,
  danger = false
}) {
  if (!open) return null;

  return (
    <div className="dialog-overlay">
      <div className="dialog">
        <h2>{title}</h2>
        <p>{message}</p>
        <div className="dialog-actions">
          <button onClick={onCancel}>
            {cancelText}
          </button>
          <button
            onClick={onConfirm}
            className={danger ? 'danger' : 'primary'}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  );
}
```

### 6.2 페이지 컴포넌트 구조

```
src/
├── pages/
│   └── superadmin/
│       ├── Dashboard/
│       │   ├── index.jsx
│       │   ├── StatCard.jsx
│       │   ├── RankingTable.jsx
│       │   └── TypeStatsChart.jsx
│       ├── Businesses/
│       │   ├── index.jsx
│       │   ├── BusinessTable.jsx
│       │   ├── BusinessFilters.jsx
│       │   └── DeleteDialog.jsx
│       ├── Users/
│       │   ├── index.jsx
│       │   ├── UserTable.jsx
│       │   ├── UserFilters.jsx
│       │   ├── RoleChangeDialog.jsx
│       │   └── DeleteDialog.jsx
│       └── AuditLogs/
│           ├── index.jsx
│           ├── LogTable.jsx
│           ├── LogFilters.jsx
│           └── LogDetailModal.jsx
├── components/
│   └── common/
│       ├── Layout/
│       ├── Pagination/
│       ├── StatusBadge/
│       ├── ConfirmDialog/
│       └── LoadingSpinner/
├── hooks/
│   ├── useAuth.js
│   ├── useFetch.js
│   └── usePermission.js
├── services/
│   ├── api.js
│   ├── auth.js
│   └── superadmin.js
└── utils/
    ├── formatters.js
    └── validators.js
```

---

## 7. 에러 처리

### 7.1 에러 코드 매핑

```javascript
// utils/errorMessages.js
const ERROR_MESSAGES = {
  // Super Admin 관련
  'SA001': '슈퍼 관리자 권한이 필요합니다.',
  'SA002': '슈퍼 관리자는 삭제할 수 없습니다.',
  'SA003': '슈퍼 관리자만 수행할 수 있는 작업입니다.',

  // Audit Log 관련
  'AL001': '감사 로그를 찾을 수 없습니다.',

  // 공통 에러
  'C001': '잘못된 입력값입니다.',
  'C005': '데이터를 찾을 수 없습니다.',
  'C006': '접근 권한이 없습니다.',

  // 인증 에러
  'A001': '로그인이 필요합니다.',
  'A002': '유효하지 않은 토큰입니다.',
  'A003': '토큰이 만료되었습니다.',

  // 기본 메시지
  'DEFAULT': '오류가 발생했습니다.'
};

export function getErrorMessage(errorCode) {
  return ERROR_MESSAGES[errorCode] || ERROR_MESSAGES.DEFAULT;
}
```

### 7.2 API 에러 핸들러

```javascript
// services/api.js
class ApiError extends Error {
  constructor(code, message, status) {
    super(message);
    this.code = code;
    this.status = status;
  }
}

async function fetchWithAuth(url, options = {}) {
  const token = getAuthToken();

  const response = await fetch(url, {
    ...options,
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
      ...options.headers
    }
  });

  const data = await response.json();

  if (!response.ok) {
    throw new ApiError(
      data.error?.code || 'UNKNOWN',
      data.error?.message || '알 수 없는 오류',
      response.status
    );
  }

  return data;
}
```

### 7.3 전역 에러 핸들러

```javascript
// hooks/useErrorHandler.js
function useErrorHandler() {
  const navigate = useNavigate();
  const { showToast } = useToast();

  const handleError = (error) => {
    console.error('Error:', error);

    if (error instanceof ApiError) {
      switch (error.code) {
        case 'A001':
        case 'A002':
        case 'A003':
          // 인증 에러: 로그인 페이지로 이동
          showToast(error.message, 'error');
          navigate('/login');
          break;

        case 'SA001':
          // 권한 에러: 접근 거부 페이지로 이동
          showToast(error.message, 'error');
          navigate('/access-denied');
          break;

        case 'SA002':
          // SUPER_ADMIN 삭제 불가
          showToast(error.message, 'warning');
          break;

        default:
          showToast(error.message, 'error');
      }
    } else {
      showToast('네트워크 오류가 발생했습니다.', 'error');
    }
  };

  return { handleError };
}
```

### 7.4 에러 바운더리 (React)

```jsx
// components/ErrorBoundary.jsx
class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="error-page">
          <h1>오류가 발생했습니다</h1>
          <p>페이지를 새로고침해주세요.</p>
          <button onClick={() => window.location.reload()}>
            새로고침
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
```

---

## 8. 샘플 코드

### 8.1 완전한 대시보드 페이지 (React)

```jsx
// pages/superadmin/Dashboard/index.jsx
import React, { useState, useEffect } from 'react';
import { fetchWithAuth } from '../../../services/api';
import { useErrorHandler } from '../../../hooks/useErrorHandler';
import StatCard from './StatCard';
import RankingTable from './RankingTable';
import TypeStatsChart from './TypeStatsChart';
import LoadingSpinner from '../../../components/common/LoadingSpinner';

function SuperAdminDashboard() {
  const [stats, setStats] = useState(null);
  const [ranking, setRanking] = useState([]);
  const [typeStats, setTypeStats] = useState([]);
  const [loading, setLoading] = useState(true);
  const { handleError } = useErrorHandler();

  useEffect(() => {
    loadDashboard();
  }, []);

  const loadDashboard = async () => {
    try {
      setLoading(true);

      // 병렬로 데이터 로드
      const [statsRes, rankingRes, typeStatsRes] = await Promise.all([
        fetchWithAuth('/api/superadmin/dashboard/stats'),
        fetchWithAuth('/api/superadmin/dashboard/business-ranking?' +
          new URLSearchParams({
            startDate: getMonthStart(),
            endDate: getToday(),
            limit: 10
          })),
        fetchWithAuth('/api/superadmin/dashboard/stats-by-type')
      ]);

      setStats(statsRes.data);
      setRanking(rankingRes.data);
      setTypeStats(typeStatsRes.data);

    } catch (error) {
      handleError(error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <LoadingSpinner />;
  }

  if (!stats) {
    return <div>데이터를 불러올 수 없습니다.</div>;
  }

  return (
    <div className="dashboard">
      <h1>슈퍼 관리자 대시보드</h1>

      {/* 통계 카드 */}
      <div className="stats-grid">
        <StatCard
          title="전체 매장"
          value={stats.totalBusinesses}
          icon="🏪"
          color="blue"
        />
        <StatCard
          title="활성 매장"
          value={stats.activeBusinesses}
          icon="✅"
          color="green"
        />
        <StatCard
          title="전체 사용자"
          value={stats.totalUsers}
          icon="👥"
          color="purple"
        />
        <StatCard
          title="오늘 예약"
          value={stats.totalReservationsToday}
          suffix="건"
          icon="📅"
          color="orange"
        />
        <StatCard
          title="오늘 매출"
          value={stats.totalRevenueToday}
          format="currency"
          icon="💰"
          color="green"
        />
        <StatCard
          title="이번 달 매출"
          value={stats.totalRevenueThisMonth}
          format="currency"
          icon="📈"
          color="blue"
        />
      </div>

      {/* 매출 랭킹 */}
      <section className="ranking-section">
        <h2>매출 랭킹 TOP 10</h2>
        <RankingTable data={ranking} />
      </section>

      {/* 업종별 통계 */}
      <section className="type-stats-section">
        <h2>업종별 통계</h2>
        <TypeStatsChart data={typeStats} />
      </section>
    </div>
  );
}

// 헬퍼 함수
function getToday() {
  return new Date().toISOString().split('T')[0];
}

function getMonthStart() {
  const date = new Date();
  date.setDate(1);
  return date.toISOString().split('T')[0];
}

export default SuperAdminDashboard;
```

### 8.2 StatCard 컴포넌트

```jsx
// pages/superadmin/Dashboard/StatCard.jsx
import React from 'react';
import './StatCard.css';

function StatCard({ title, value, suffix, format, icon, color }) {
  const formatValue = (val) => {
    if (format === 'currency') {
      return new Intl.NumberFormat('ko-KR', {
        style: 'currency',
        currency: 'KRW',
        minimumFractionDigits: 0
      }).format(val);
    }

    return val.toLocaleString('ko-KR');
  };

  return (
    <div className={`stat-card stat-card-${color}`}>
      <div className="stat-icon">{icon}</div>
      <div className="stat-content">
        <h3 className="stat-title">{title}</h3>
        <p className="stat-value">
          {formatValue(value)}
          {suffix && <span className="suffix"> {suffix}</span>}
        </p>
      </div>
    </div>
  );
}

export default StatCard;
```

```css
/* pages/superadmin/Dashboard/StatCard.css */
.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 8px;
  background: white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.stat-icon {
  font-size: 48px;
  margin-right: 20px;
}

.stat-content {
  flex: 1;
}

.stat-title {
  margin: 0;
  font-size: 14px;
  color: #666;
  font-weight: normal;
}

.stat-value {
  margin: 8px 0 0;
  font-size: 28px;
  font-weight: bold;
}

.stat-card-blue { border-left: 4px solid #3b82f6; }
.stat-card-green { border-left: 4px solid #10b981; }
.stat-card-purple { border-left: 4px solid #8b5cf6; }
.stat-card-orange { border-left: 4px solid #f59e0b; }
```

### 8.3 API 서비스 레이어

```javascript
// services/superadmin.js
import { fetchWithAuth } from './api';

export const superAdminService = {
  // 대시보드
  getSystemStats: () =>
    fetchWithAuth('/api/superadmin/dashboard/stats'),

  getBusinessRanking: (startDate, endDate, limit = 10) =>
    fetchWithAuth(
      `/api/superadmin/dashboard/business-ranking?` +
      `startDate=${startDate}&endDate=${endDate}&limit=${limit}`
    ),

  getStatsByType: () =>
    fetchWithAuth('/api/superadmin/dashboard/stats-by-type'),

  // 매장 관리
  getBusinesses: (filters, page, size) => {
    const params = new URLSearchParams({
      page,
      size,
      ...filters
    });
    return fetchWithAuth(`/api/superadmin/businesses?${params}`);
  },

  deleteBusiness: (id, hard = false) =>
    fetchWithAuth(`/api/superadmin/businesses/${id}?hard=${hard}`, {
      method: 'DELETE'
    }),

  bulkUpdateBusinessStatus: (businessIds, status) =>
    fetchWithAuth('/api/superadmin/businesses/bulk-status', {
      method: 'PATCH',
      body: JSON.stringify({ businessIds, status })
    }),

  // 사용자 관리
  getUsers: (filters, page, size) => {
    const params = new URLSearchParams({
      page,
      size,
      ...filters
    });
    return fetchWithAuth(`/api/superadmin/users?${params}`);
  },

  changeUserRole: (userId, role) =>
    fetchWithAuth(`/api/superadmin/users/${userId}/role`, {
      method: 'PATCH',
      body: JSON.stringify({ role })
    }),

  suspendUser: (userId) =>
    fetchWithAuth(`/api/superadmin/users/${userId}/suspend`, {
      method: 'PATCH'
    }),

  deleteUser: (userId) =>
    fetchWithAuth(`/api/superadmin/users/${userId}`, {
      method: 'DELETE'
    }),

  // 감사 로그
  getAuditLogs: (filters, page, size) => {
    const params = new URLSearchParams({
      page,
      size,
      ...filters
    });
    return fetchWithAuth(`/api/audit-logs?${params}`);
  },

  getAuditLog: (id) =>
    fetchWithAuth(`/api/audit-logs/${id}`)
};
```

### 8.4 커스텀 훅 예시

```javascript
// hooks/usePagination.js
import { useState } from 'react';

export function usePagination(initialPage = 1, initialSize = 20) {
  const [page, setPage] = useState(initialPage);
  const [size, setSize] = useState(initialSize);

  const goToPage = (newPage) => {
    setPage(newPage);
  };

  const nextPage = () => {
    setPage(prev => prev + 1);
  };

  const prevPage = () => {
    setPage(prev => Math.max(1, prev - 1));
  };

  const reset = () => {
    setPage(1);
  };

  return {
    page,
    size,
    setPage,
    setSize,
    goToPage,
    nextPage,
    prevPage,
    reset
  };
}
```

```javascript
// hooks/useSuperAdmin.js
import { useState, useCallback } from 'react';
import { superAdminService } from '../services/superadmin';
import { useErrorHandler } from './useErrorHandler';

export function useSuperAdmin() {
  const [loading, setLoading] = useState(false);
  const { handleError } = useErrorHandler();

  const withLoading = useCallback(async (asyncFn) => {
    try {
      setLoading(true);
      return await asyncFn();
    } catch (error) {
      handleError(error);
      throw error;
    } finally {
      setLoading(false);
    }
  }, [handleError]);

  return {
    loading,

    // 대시보드
    getSystemStats: () =>
      withLoading(() => superAdminService.getSystemStats()),

    getBusinessRanking: (startDate, endDate, limit) =>
      withLoading(() => superAdminService.getBusinessRanking(startDate, endDate, limit)),

    // 매장 관리
    getBusinesses: (filters, page, size) =>
      withLoading(() => superAdminService.getBusinesses(filters, page, size)),

    deleteBusiness: (id, hard) =>
      withLoading(() => superAdminService.deleteBusiness(id, hard)),

    // 사용자 관리
    getUsers: (filters, page, size) =>
      withLoading(() => superAdminService.getUsers(filters, page, size)),

    changeUserRole: (userId, role) =>
      withLoading(() => superAdminService.changeUserRole(userId, role)),

    suspendUser: (userId) =>
      withLoading(() => superAdminService.suspendUser(userId)),

    deleteUser: (userId) =>
      withLoading(() => superAdminService.deleteUser(userId))
  };
}
```

---

## 9. 스타일 가이드

### 9.1 색상 팔레트

```css
:root {
  /* Primary Colors */
  --primary-blue: #3b82f6;
  --primary-green: #10b981;
  --primary-purple: #8b5cf6;
  --primary-orange: #f59e0b;
  --primary-red: #ef4444;

  /* Status Colors */
  --status-active: #10b981;
  --status-inactive: #6b7280;
  --status-suspended: #ef4444;

  /* Text Colors */
  --text-primary: #111827;
  --text-secondary: #6b7280;
  --text-disabled: #9ca3af;

  /* Background Colors */
  --bg-page: #f9fafb;
  --bg-card: #ffffff;
  --bg-hover: #f3f4f6;

  /* Border Colors */
  --border-light: #e5e7eb;
  --border-medium: #d1d5db;
}
```

### 9.2 타이포그래피

```css
/* Typography */
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI',
               'Roboto', 'Helvetica', 'Arial', sans-serif;
  font-size: 14px;
  line-height: 1.5;
  color: var(--text-primary);
}

h1 { font-size: 32px; font-weight: 700; margin-bottom: 24px; }
h2 { font-size: 24px; font-weight: 600; margin-bottom: 16px; }
h3 { font-size: 18px; font-weight: 600; margin-bottom: 12px; }
```

### 9.3 버튼 스타일

```css
/* Buttons */
.btn {
  padding: 8px 16px;
  border-radius: 6px;
  border: none;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary {
  background: var(--primary-blue);
  color: white;
}

.btn-primary:hover {
  background: #2563eb;
}

.btn-danger {
  background: var(--primary-red);
  color: white;
}

.btn-danger:hover {
  background: #dc2626;
}

.btn-secondary {
  background: #e5e7eb;
  color: var(--text-primary);
}

.btn-secondary:hover {
  background: #d1d5db;
}
```

### 9.4 테이블 스타일

```css
/* Table */
.table {
  width: 100%;
  background: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.table th {
  background: #f9fafb;
  padding: 12px 16px;
  text-align: left;
  font-weight: 600;
  color: var(--text-secondary);
  border-bottom: 2px solid var(--border-light);
}

.table td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-light);
}

.table tr:hover {
  background: var(--bg-hover);
}
```

---

## 10. 배포 체크리스트

### 10.1 개발 환경

- [ ] API 엔드포인트 설정 확인
- [ ] JWT 토큰 저장 방식 결정 (localStorage vs sessionStorage)
- [ ] 환경 변수 설정 (.env)
- [ ] CORS 설정 확인

### 10.2 기능 테스트

- [ ] 로그인 및 인증 테스트
- [ ] 대시보드 데이터 로딩 테스트
- [ ] 매장 목록 조회 및 필터링 테스트
- [ ] 매장 삭제 기능 테스트
- [ ] 사용자 목록 조회 테스트
- [ ] 역할 변경 기능 테스트
- [ ] 사용자 정지/삭제 테스트
- [ ] 감사 로그 조회 테스트
- [ ] 페이지네이션 테스트
- [ ] 에러 처리 테스트

### 10.3 UI/UX

- [ ] 반응형 디자인 확인
- [ ] 로딩 상태 표시
- [ ] 에러 메시지 표시
- [ ] 성공 알림 표시
- [ ] 키보드 네비게이션
- [ ] 접근성 (ARIA labels)

### 10.4 성능

- [ ] API 요청 최적화 (병렬 처리)
- [ ] 이미지 최적화
- [ ] 코드 스플리팅
- [ ] 캐싱 전략

### 10.5 보안

- [ ] XSS 방어
- [ ] CSRF 토큰
- [ ] 민감한 정보 마스킹
- [ ] HTTPS 사용

---

## 11. FAQ

### Q1: SUPER_ADMIN 계정을 추가로 만들 수 있나요?

A: 네, 슈퍼 관리자 권한으로 사용자의 역할을 `SUPER_ADMIN`으로 변경할 수 있습니다.

```javascript
// 기존 사용자의 역할을 SUPER_ADMIN으로 변경
await superAdminService.changeUserRole(userId, 'SUPER_ADMIN');
```

### Q2: 삭제된 데이터를 복구할 수 있나요?

A: 하드 삭제(`hard=true`)의 경우 복구가 불가능합니다. 소프트 삭제(`hard=false`)는 DB에서 직접 복구 가능합니다.

### Q3: 감사 로그는 얼마나 보관되나요?

A: 백엔드 설정에 따라 다릅니다. 기본적으로 무제한 보관되며, 필요시 백엔드에서 보관 정책을 설정할 수 있습니다.

### Q4: 모바일에서도 사용 가능한가요?

A: 슈퍼 관리자 기능은 데스크톱 환경에 최적화되어 있지만, 반응형 디자인을 적용하면 모바일에서도 사용 가능합니다.

### Q5: 다국어 지원이 되나요?

A: 현재는 한국어만 지원합니다. 다국어 지원이 필요한 경우 i18n 라이브러리를 통합해야 합니다.

---

## 12. 참고 자료

### 12.1 추천 라이브러리

**React**:
- React Router (라우팅)
- Axios (HTTP 클라이언트)
- React Query (데이터 페칭)
- Zustand (상태 관리)
- React Hook Form (폼 관리)
- Chart.js / Recharts (차트)
- date-fns (날짜 처리)

**Vue**:
- Vue Router
- Axios
- Pinia (상태 관리)
- VeeValidate (폼 검증)
- Chart.js / Vue-Chartjs

**공통**:
- TailwindCSS (스타일링)
- Headless UI (접근성)
- date-fns (날짜 처리)

### 12.2 관련 문서

- [백엔드 API 문서](./04_api/README.md)
- [도메인 개발 패턴](./02_domain/development-pattern.md)
- [보안 구조](./01_architecture/security.md)
- [예외 처리](./01_architecture/exception-handling.md)

---

## 13. 지원

문의사항이나 이슈가 있으면 다음으로 연락주세요:
- GitHub Issues: [프로젝트 저장소]
- 이메일: [support@example.com]

---

**마지막 업데이트**: 2024-01-15
**문서 버전**: 1.0
