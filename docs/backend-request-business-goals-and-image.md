# 백엔드 요청: 매장 목표 설정 필드 및 프로필 이미지 API

## 1. 매장 목표 설정 필드 추가 요청

### 현재 문제

프론트엔드에서 `PATCH /api/businesses/{businessId}`로 매장 목표 정보를 전송하고 있지만,
백엔드에서 해당 필드를 처리하지 않는 것으로 보임.

### 필요한 필드 (Business 엔티티에 추가)

| 필드명 | 타입 | 설명 | NULL 허용 |
|--------|------|------|-----------|
| `dailyRevenueGoal` | INTEGER | 일일 매출 목표 (원) | YES |
| `monthlyRevenueGoal` | INTEGER | 월간 매출 목표 (원) | YES |
| `monthlyNewCustomerGoal` | INTEGER | 월간 신규 고객 목표 (명) | YES |

### 프론트엔드 전송 형식

```
PATCH /api/businesses/{businessId}
Content-Type: application/json
```

```json
{
  "name": "매장명",
  "businessType": "BEAUTY_SHOP",
  "dailyRevenueGoal": 500000,
  "monthlyRevenueGoal": 15000000,
  "monthlyNewCustomerGoal": 50
}
```

### 필요한 작업

1. `businesses` 테이블에 3개 컬럼 추가 (NULL 허용)
2. `BusinessEntity`에 필드 추가
3. `updateBusiness()` 메서드에서 해당 필드 처리
4. `GET /api/businesses/{businessId}` 응답에 해당 필드 포함
5. 통계 API에서 목표 값 참조 (대시보드 달성률 표시용)

### 마이그레이션 SQL

```sql
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS daily_revenue_goal INTEGER;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS monthly_revenue_goal INTEGER;
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS monthly_new_customer_goal INTEGER;
```

### 기대 응답 (GET /api/businesses/{businessId})

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "매장명",
    "businessType": "BEAUTY_SHOP",
    "dailyRevenueGoal": 500000,
    "monthlyRevenueGoal": 15000000,
    "monthlyNewCustomerGoal": 50
  }
}
```

---

## 2. 매장 프로필 이미지 업로드/삭제 API 추가 요청

### 현재 문제

매장 프로필 이미지(`profileImageUrl`)는 슬러그 예약 페이지에서 이미 표시되고 있지만,
관리자가 이미지를 업로드할 수 있는 API가 없음.

### 필요한 엔드포인트

#### POST /api/businesses/{businessId}/profile-image

- 설명: 매장 프로필 이미지 업로드
- Content-Type: multipart/form-data

요청 파라미터:

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|------|------|
| `image` | File | YES | 이미지 파일 (JPG, PNG, WebP) |

제약 조건:
- 최대 파일 크기: 5MB
- 허용 MIME 타입: `image/jpeg`, `image/png`, `image/webp`

성공 응답 (200):

```json
{
  "success": true,
  "data": {
    "profileImageUrl": "https://storage.example.com/businesses/1/profile.jpg"
  }
}
```

에러 응답:

| HTTP 코드 | 설명 |
|-----------|------|
| 400 | 지원하지 않는 파일 형식 |
| 400 | 파일 크기 초과 (5MB) |
| 404 | 매장 미존재 |

---

#### DELETE /api/businesses/{businessId}/profile-image

- 설명: 매장 프로필 이미지 삭제

성공 응답 (200):

```json
{
  "success": true,
  "data": null
}
```

에러 응답:

| HTTP 코드 | 설명 |
|-----------|------|
| 404 | 매장 미존재 |

---

### 참고 - 기존 사용자 프로필 이미지 API 패턴

이미 사용자 프로필 이미지 업로드 API(`POST /api/auth/profile/image`)가 구현되어 있으므로,
동일한 스토리지 패턴(S3 또는 로컬 파일)으로 구현하면 됨.

업로드 시 이전 이미지 자동 삭제 (덮어쓰기) 처리 필요.

---

### 프론트엔드 연동 포인트

아래 파일들이 해당 API를 호출하도록 이미 구현되어 있음.

**`src/api/business-settings.js`**

```javascript
// 매장 프로필 이미지 업로드
uploadBusinessImage(businessId, file) {
  const formData = new FormData()
  formData.append('image', file)
  return apiClient.post(`/businesses/${businessId}/profile-image`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
},

// 매장 프로필 이미지 삭제
deleteBusinessImage(businessId) {
  return apiClient.delete(`/businesses/${businessId}/profile-image`)
},
```

**`src/stores/business-settings.js`**

```javascript
// 업로드 성공 시 store 상태 업데이트
async uploadBusinessImage(file) {
  const { data } = await businessSettingsApi.uploadBusinessImage(businessId, file)
  if (this.business) {
    this.business.profileImageUrl = data.profileImageUrl
  }
  return data
},

// 삭제 성공 시 store 상태 초기화
async deleteBusinessImage() {
  await businessSettingsApi.deleteBusinessImage(businessId)
  if (this.business) {
    this.business.profileImageUrl = null
  }
},
```

**`src/pages/shop-admin/business-settings/index.vue`**

이미지 업로드 UI가 구현되어 있으며, 위 store 액션을 호출함.

---

## 3. 변경 이력

| 날짜 | 요청 내용 |
|------|-----------|
| 2026-02-18 | 매장 목표 설정 필드 3개 추가 요청 (`dailyRevenueGoal`, `monthlyRevenueGoal`, `monthlyNewCustomerGoal`) |
| 2026-02-18 | 매장 프로필 이미지 업로드/삭제 API 추가 요청 (`POST/DELETE /api/businesses/{businessId}/profile-image`) |
