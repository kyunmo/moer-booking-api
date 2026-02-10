# 2026-02-10 매장 설정 기능 - 프론트엔드 작업 가이드

## 개요

백엔드에서 매장 설정 및 업종 변경 기능이 개선되었습니다. 기존 API는 하위 호환되며, 새로운 기능을 사용하려면 프론트엔드 작업이 필요합니다.

## 기존 기능 유지 (변경 불필요)

다음 기능들은 **프론트엔드 코드 수정 없이** 그대로 작동합니다:
- 매장 이름, 전화번호, 주소, 설명 수정
- 영업시간 수정
- 매장 설정 (예약/알림/결제 설정) 수정

## 프론트엔드 작업 목록

### 1. 업종 변경 기능 추가 ⭐ 신규

**엔드포인트**: `PATCH /api/businesses/{id}`

#### 요청 파라미터 추가
```typescript
interface BusinessUpdateRequest {
  name?: string;
  businessType?: 'BEAUTY_SHOP' | 'PILATES' | 'YOGA' | 'CAFE' |
                 'STUDY_CAFE' | 'WORKSHOP' | 'ACADEMY' | 'PET_SALON' | 'OTHER';  // 추가
  phone?: string;
  address?: string;
  description?: string;
  businessHours?: Record<string, any>;
  dailyRevenueGoal?: number;      // 추가
  monthlyRevenueGoal?: number;    // 추가
  monthlyNewCustomerGoal?: number; // 추가
}
```

#### UI 구현 예시

**1) 업종 선택 드롭다운**
```jsx
<FormControl fullWidth>
  <InputLabel>업종</InputLabel>
  <Select
    value={businessType}
    onChange={(e) => setBusinessType(e.target.value)}
  >
    <MenuItem value="BEAUTY_SHOP">미용실</MenuItem>
    <MenuItem value="PILATES">필라테스</MenuItem>
    <MenuItem value="YOGA">요가</MenuItem>
    <MenuItem value="CAFE">카페</MenuItem>
    <MenuItem value="STUDY_CAFE">스터디카페</MenuItem>
    <MenuItem value="WORKSHOP">공방</MenuItem>
    <MenuItem value="ACADEMY">학원</MenuItem>
    <MenuItem value="PET_SALON">애견미용</MenuItem>
    <MenuItem value="OTHER">기타</MenuItem>
  </Select>
</FormControl>
```

**2) 목표 설정 입력 필드**
```jsx
<Box sx={{ mt: 3 }}>
  <Typography variant="h6">목표 설정</Typography>

  <TextField
    label="일일 매출 목표"
    type="number"
    value={dailyRevenueGoal}
    onChange={(e) => setDailyRevenueGoal(Number(e.target.value))}
    InputProps={{
      startAdornment: <InputAdornment position="start">₩</InputAdornment>,
    }}
    fullWidth
    margin="normal"
  />

  <TextField
    label="월간 매출 목표"
    type="number"
    value={monthlyRevenueGoal}
    onChange={(e) => setMonthlyRevenueGoal(Number(e.target.value))}
    InputProps={{
      startAdornment: <InputAdornment position="start">₩</InputAdornment>,
    }}
    fullWidth
    margin="normal"
  />

  <TextField
    label="월간 신규 고객 목표"
    type="number"
    value={monthlyNewCustomerGoal}
    onChange={(e) => setMonthlyNewCustomerGoal(Number(e.target.value))}
    InputProps={{
      endAdornment: <InputAdornment position="end">명</InputAdornment>,
    }}
    fullWidth
    margin="normal"
  />
</Box>
```

**3) API 호출**
```typescript
const updateBusiness = async (id: number, data: BusinessUpdateRequest) => {
  const response = await axios.patch(`/api/businesses/${id}`, data, {
    headers: {
      Authorization: `Bearer ${getAccessToken()}`
    }
  });
  return response.data;
};

// 사용 예시
await updateBusiness(1, {
  businessType: 'PILATES',
  dailyRevenueGoal: 500000,
  monthlyRevenueGoal: 15000000,
  monthlyNewCustomerGoal: 50
});
```

### 2. 매장 설정 UI 개선 (선택적)

**엔드포인트**: `PATCH /api/businesses/{id}/settings`

#### 현재 상태
- 모든 설정 필드를 한 번에 전송
- 일부 설정만 변경 시에도 전체 객체 전송 필요

#### 개선 후
- 변경된 필드만 전송 가능
- 서버에서 자동으로 기존 값 유지

#### 구현 예시

**탭별 설정 저장**
```typescript
// 예약 설정만 저장
const updateBookingSettings = async (businessId: number) => {
  await axios.patch(`/api/businesses/${businessId}/settings`, {
    bookingInterval: 60,
    autoConfirm: 'Y',
    allowOnlineBooking: 'Y'
    // 나머지 필드는 생략 가능 (기존 값 유지)
  });
};

// 알림 설정만 저장
const updateNotificationSettings = async (businessId: number) => {
  await axios.patch(`/api/businesses/${businessId}/settings`, {
    sendConfirmationSms: 'Y',
    sendReminderSms: 'Y',
    reminderHoursBefore: 24
    // 나머지 필드는 생략 가능
  });
};
```

## 화면 레이아웃 제안

### 매장 정보 수정 화면

```
┌─────────────────────────────────────┐
│ 매장 정보 수정                       │
├─────────────────────────────────────┤
│                                     │
│ [매장명] ___________________        │
│                                     │
│ [업종]   [미용실 ▼]   ⭐ 신규       │
│                                     │
│ [전화번호] ___________________      │
│                                     │
│ [주소] _________________________    │
│                                     │
│ [소개] _________________________    │
│       _________________________    │
│                                     │
├─────────────────────────────────────┤
│ 목표 설정 ⭐ 신규                   │
├─────────────────────────────────────┤
│                                     │
│ [일일 매출 목표]  ₩ _________       │
│                                     │
│ [월간 매출 목표]  ₩ _________       │
│                                     │
│ [월간 신규 고객]  _____ 명          │
│                                     │
├─────────────────────────────────────┤
│            [취소]  [저장]            │
└─────────────────────────────────────┘
```

### 매장 설정 화면 (탭 구조)

```
┌─────────────────────────────────────┐
│ [예약설정] [알림설정] [결제설정] ... │
├─────────────────────────────────────┤
│                                     │
│ 예약 시간 간격                       │
│ ○ 15분  ○ 30분  ● 60분             │
│                                     │
│ ☑ 예약 자동 확정                     │
│ ☑ 온라인 예약 허용                   │
│                                     │
│ 최대 사전 예약 [30] 일              │
│ 최소 사전 예약 [2] 시간             │
│                                     │
├─────────────────────────────────────┤
│              [저장]                  │
└─────────────────────────────────────┘
```

## TypeScript 타입 정의

```typescript
// 업종 타입
export type BusinessType =
  | 'BEAUTY_SHOP'
  | 'PILATES'
  | 'YOGA'
  | 'CAFE'
  | 'STUDY_CAFE'
  | 'WORKSHOP'
  | 'ACADEMY'
  | 'PET_SALON'
  | 'OTHER';

// 업종 한글 레이블
export const BUSINESS_TYPE_LABELS: Record<BusinessType, string> = {
  BEAUTY_SHOP: '미용실',
  PILATES: '필라테스',
  YOGA: '요가',
  CAFE: '카페',
  STUDY_CAFE: '스터디카페',
  WORKSHOP: '공방',
  ACADEMY: '학원',
  PET_SALON: '애견미용',
  OTHER: '기타'
};

// 매장 정보 수정 요청
export interface BusinessUpdateRequest {
  name?: string;
  businessType?: BusinessType;
  phone?: string;
  address?: string;
  description?: string;
  businessHours?: Record<string, {
    open: string;
    close: string;
  }>;
  dailyRevenueGoal?: number;
  monthlyRevenueGoal?: number;
  monthlyNewCustomerGoal?: number;
}

// 매장 설정 수정 요청
export interface BusinessSettingsUpdateRequest {
  // 예약 설정
  bookingInterval?: number;
  autoConfirm?: 'Y' | 'N';
  allowOnlineBooking?: 'Y' | 'N';
  maxAdvanceBookingDays?: number;
  minAdvanceBookingHours?: number;

  // 알림 설정
  sendConfirmationSms?: 'Y' | 'N';
  sendReminderSms?: 'Y' | 'N';
  reminderHoursBefore?: number;
  sendCancelSms?: 'Y' | 'N';

  // 카카오톡 설정
  kakaoChannelId?: string;
  kakaoApiKey?: string;
  kakaoEnabled?: 'Y' | 'N';

  // 결제 설정
  paymentMethods?: string;
  requireDeposit?: 'Y' | 'N';
  depositAmount?: number;

  // 취소 정책
  allowCancellation?: 'Y' | 'N';
  cancelDeadlineHours?: number;
  noShowPenaltyEnabled?: 'Y' | 'N';

  // 기타
  timezone?: string;
  language?: string;
}

// 매장 응답
export interface BusinessResponse {
  id: number;
  ownerId: number;
  name: string;
  businessType: BusinessType;
  phone?: string;
  address?: string;
  description?: string;
  businessHours?: Record<string, any>;
  status: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
  dailyRevenueGoal?: number;
  monthlyRevenueGoal?: number;
  monthlyNewCustomerGoal?: number;
  settings?: BusinessSettingsResponse;
  createdAt: string;
  updatedAt: string;
}

// 매장 설정 응답
export interface BusinessSettingsResponse {
  id: number;
  businessId: number;
  bookingInterval: number;
  autoConfirm: 'Y' | 'N';
  allowOnlineBooking: 'Y' | 'N';
  maxAdvanceBookingDays: number;
  minAdvanceBookingHours: number;
  sendConfirmationSms: 'Y' | 'N';
  sendReminderSms: 'Y' | 'N';
  reminderHoursBefore: number;
  sendCancelSms: 'Y' | 'N';
  kakaoChannelId?: string;
  kakaoApiKey?: string;
  kakaoEnabled: 'Y' | 'N';
  paymentMethods: string;
  requireDeposit: 'Y' | 'N';
  depositAmount: number;
  allowCancellation: 'Y' | 'N';
  cancelDeadlineHours: number;
  noShowPenaltyEnabled: 'Y' | 'N';
  timezone: string;
  language: string;
  createdAt: string;
  updatedAt: string;
}
```

## API 호출 함수

```typescript
import axios from 'axios';

const API_BASE_URL = '/api';

// 매장 정보 수정
export const updateBusiness = async (
  id: number,
  data: BusinessUpdateRequest
): Promise<BusinessResponse> => {
  const response = await axios.patch(
    `${API_BASE_URL}/businesses/${id}`,
    data,
    {
      headers: {
        Authorization: `Bearer ${getAccessToken()}`
      }
    }
  );
  return response.data.data;
};

// 매장 설정 수정
export const updateBusinessSettings = async (
  id: number,
  data: BusinessSettingsUpdateRequest
): Promise<BusinessResponse> => {
  const response = await axios.patch(
    `${API_BASE_URL}/businesses/${id}/settings`,
    data,
    {
      headers: {
        Authorization: `Bearer ${getAccessToken()}`
      }
    }
  );
  return response.data.data;
};

// 매장 조회
export const getBusiness = async (id: number): Promise<BusinessResponse> => {
  const response = await axios.get(
    `${API_BASE_URL}/businesses/${id}`,
    {
      headers: {
        Authorization: `Bearer ${getAccessToken()}`
      }
    }
  );
  return response.data.data;
};
```

## 테스트 시나리오

### 1. 업종 변경 테스트
1. 매장 정보 수정 화면 접근
2. 업종 드롭다운에서 다른 업종 선택
3. 저장 버튼 클릭
4. 매장 상세 조회 시 변경된 업종 확인

### 2. 목표 설정 테스트
1. 매장 정보 수정 화면 접근
2. 일일/월간 목표 입력
3. 저장 버튼 클릭
4. 대시보드에서 목표 대비 달성률 확인 (대시보드 구현 시)

### 3. 매장 설정 부분 업데이트 테스트
1. 매장 설정 화면 접근
2. 예약 설정 탭에서 일부 항목만 수정
3. 저장 버튼 클릭
4. 다른 탭(알림 설정 등)으로 이동
5. 다른 탭의 설정이 기존 값 그대로 유지되는지 확인

## 주의사항

### 1. 하위 호환성
- 기존 API 호출 방식은 모두 유지됨
- 새 필드는 선택적으로 추가 가능

### 2. 업종 변경 제한 (추후 고려)
- 현재는 자유롭게 변경 가능
- 향후 예약 내역이 있는 경우 업종 변경 제한 필요할 수 있음

### 3. 목표 설정 활용
- 대시보드에서 목표 대비 달성률 표시
- 통계 차트에서 목표선 표시

### 4. 설정 저장 UX
- 각 탭마다 저장 버튼 제공
- 변경 사항이 있을 때만 저장 버튼 활성화
- 저장 성공 시 토스트 메시지 표시

## 참고 자료

- [백엔드 수정 내역](./2026-02-10-business-settings-fix.md)
- [Business API 명세](../docs/04_api/README.md)

---

**작성일**: 2026-02-10
**상태**: 📝 프론트엔드 작업 필요
