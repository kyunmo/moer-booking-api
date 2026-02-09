# 📊 대시보드 Phase 3: 고급 인사이트 API 문서

## 개요

예약 관리 시스템 대시보드에 매출 트렌드, 시간대 분석, 목표 달성률 기능이 추가되었습니다.

---

## 🆕 Phase 3 추가 기능

### 1. **매출 트렌드 분석** (`revenueTrend`)

#### 포함 지표
- **오늘 vs 전일**
  - 오늘 매출
  - 전일 매출
  - 전일 대비 증감률 (%)

- **이번 주 vs 전주**
  - 이번 주 매출
  - 전주 매출
  - 전주 대비 증감률 (%)

- **이번 달 vs 전월**
  - 이번 달 매출
  - 전월 매출
  - 전월 대비 증감률 (%)

- **올해 vs 작년 동월**
  - 올해 동월 매출
  - 작년 동월 매출
  - 전년 대비 증감률 (%)

- **최근 6개월 월별 매출 그래프 데이터**
  - 년월 (yyyy-MM)
  - 매출
  - 예약 건수

#### 활용 방안
✅ 성장률 추적
✅ 계절성 파악
✅ 목표 설정 근거
✅ 투자 의사결정

---

### 2. **시간대별 분석** (`timeSlotAnalysis`)

#### 포함 지표
- **시간대별 예약 분포**
  - 09:00 ~ 21:00 (1시간 단위)
  - 시간대별 예약 건수

- **요일별 예약 분포**
  - 월요일 ~ 일요일
  - 요일별 예약 건수

- **피크 타임**
  - 가장 바쁜 시간대
  - 해당 시간대 예약 수

- **한산한 시간대**
  - 가장 여유로운 시간대
  - 해당 시간대 예약 수

#### 활용 방안
✅ 직원 스케줄 최적화
✅ 피크 타임 인력 배치
✅ 한산한 시간 할인 정책
✅ 마케팅 타이밍 결정

---

### 3. **목표 달성률** (`goalProgress`)

#### 포함 지표
- **일일 매출 목표**
  - 목표 금액
  - 오늘 매출
  - 달성률 (%)

- **월간 매출 목표**
  - 목표 금액
  - 이번 달 매출
  - 달성률 (%)

- **월간 신규 고객 목표**
  - 목표 고객 수
  - 이번 달 신규 고객
  - 달성률 (%)

#### 활용 방안
✅ 목표 관리 및 동기부여
✅ 진행 상황 실시간 추적
✅ 팀 성과 시각화
✅ 경영 의사결정

---

## 📡 API 응답 구조

### **GET** `/api/businesses/{businessId}/dashboard?date=2026-02-09`

```json
{
  "success": true,
  "data": {
    // Phase 1 & 2 기존 데이터...

    // ✨ Phase 3 신규 추가
    "revenueTrend": {
      "todayRevenue": 850000,
      "yesterdayRevenue": 720000,
      "dailyGrowthRate": 18.06,

      "thisWeekRevenue": 4200000,
      "lastWeekRevenue": 3800000,
      "weeklyGrowthRate": 10.53,

      "thisMonthRevenue": 18500000,
      "lastMonthRevenue": 16200000,
      "monthlyGrowthRate": 14.20,

      "thisYearMonthRevenue": 18500000,
      "lastYearMonthRevenue": 14300000,
      "yearlyGrowthRate": 29.37,

      "monthlyRevenues": [
        {
          "yearMonth": "2025-09",
          "revenue": 14500000,
          "reservationCount": 198
        },
        {
          "yearMonth": "2025-10",
          "revenue": 15200000,
          "reservationCount": 210
        },
        // ... (6개월)
      ]
    },

    "timeSlotAnalysis": {
      "hourlyDistribution": [
        { "hour": "09:00", "count": 8 },
        { "hour": "10:00", "count": 15 },
        { "hour": "11:00", "count": 22 },
        // ...
      ],
      "weekdayDistribution": [
        { "dayOfWeek": 1, "dayName": "월", "count": 42 },
        { "dayOfWeek": 2, "dayName": "화", "count": 38 },
        // ...
      ],
      "peakHour": "14:00",
      "peakHourCount": 32,
      "offPeakHour": "09:00",
      "offPeakHourCount": 8
    },

    "goalProgress": {
      "dailyRevenueGoal": 1000000,
      "todayRevenue": 850000,
      "dailyRevenueAchievement": 85.0,

      "monthlyRevenueGoal": 30000000,
      "thisMonthRevenue": 18500000,
      "monthlyRevenueAchievement": 61.67,

      "monthlyNewCustomerGoal": 50,
      "thisMonthNewCustomers": 32,
      "monthlyNewCustomerAchievement": 64.0
    }
  }
}
```

---

## 🗄️ DB 마이그레이션

### 필수 사전 작업

Phase 3 기능을 사용하려면 Business 테이블에 목표 필드를 추가해야 합니다.

#### **방법 1: SQL 파일 실행**
```bash
docker exec moer-postgresql psql -U moer -d moer_dev < apply-dashboard-migration.sql
```

#### **방법 2: 직접 쿼리 실행**
```sql
ALTER TABLE businesses
ADD COLUMN IF NOT EXISTS daily_revenue_goal INTEGER DEFAULT NULL,
ADD COLUMN IF NOT EXISTS monthly_revenue_goal INTEGER DEFAULT NULL,
ADD COLUMN IF NOT EXISTS monthly_new_customer_goal INTEGER DEFAULT NULL;

-- 테스트 데이터 (선택)
UPDATE businesses
SET daily_revenue_goal = 1000000,        -- 일일 100만원
    monthly_revenue_goal = 30000000,    -- 월간 3천만원
    monthly_new_customer_goal = 50      -- 월간 50명
WHERE id = 1;
```

---

## 🎨 UI 표시 권장사항

### 매출 트렌드 차트

```
📈 매출 추이 (최근 6개월)

 20M ┤                           ●
 18M ┤                       ●   ●
 16M ┤                   ●
 14M ┤       ●   ●   ●
 12M ┤   ●
     └─────────────────────────────
      9월  10월 11월 12월 1월  2월

전월 대비: ▲ 14.2% 📈
전년 대비: ▲ 29.4% 🚀
```

### 시간대별 히트맵

```
⏰ 시간대별 예약 분포 (이번 달)

시간  │ 예약 수 │ 막대 그래프
─────┼────────┼──────────────────────
09:00 │   8건  │ ▓▓░░░░░░░░ (25%)
10:00 │  15건  │ ▓▓▓▓▓░░░░░ (47%)
11:00 │  22건  │ ▓▓▓▓▓▓▓░░░ (69%)
12:00 │  18건  │ ▓▓▓▓▓▓░░░░ (56%)
13:00 │  20건  │ ▓▓▓▓▓▓░░░░ (62%)
14:00 │  32건  │ ▓▓▓▓▓▓▓▓▓▓ (100%) 🔥 피크
15:00 │  28건  │ ▓▓▓▓▓▓▓▓▓░ (88%)
16:00 │  24건  │ ▓▓▓▓▓▓▓▓░░ (75%)

💡 피크 타임: 14:00 (32건)
💡 한산한 시간: 09:00 (8건) - 할인 정책 추천
```

### 목표 달성률 프로그레스 바

```
🎯 목표 달성 현황

📌 일일 매출 목표
목표: 1,000,000원
현재:   850,000원
[████████████████░░░░] 85.0%
🟡 목표 근접 (80% 이상)

📌 월간 매출 목표
목표: 30,000,000원
현재: 18,500,000원
[████████████░░░░░░░░] 61.7%
🔴 목표 미달

📌 월간 신규 고객 목표
목표: 50명
현재: 32명
[████████████▓░░░░░░░] 64.0%
🔴 목표 미달
```

### 색상 가이드

| 달성률 | 색상 | 아이콘 | 의미 |
|--------|------|--------|------|
| ≥ 100% | 🟢 초록 | ✅ | 목표 달성 |
| 80~99% | 🟡 노랑 | 🔥 | 목표 근접 |
| < 80% | 🔴 빨강 | 📉 | 목표 미달 |

---

## 🧪 테스트 방법

### 1. DB 마이그레이션 적용
```bash
docker exec moer-postgresql psql -U moer -d moer_dev < apply-dashboard-migration.sql
```

### 2. API 테스트
```bash
bash test-dashboard-phase3.sh 1
```

### 3. 수동 테스트
```bash
# 전체 대시보드 조회
curl http://localhost:8080/api/businesses/1/dashboard | jq '.data | {
  "매출트렌드": .revenueTrend,
  "시간대분석": .timeSlotAnalysis,
  "목표달성률": .goalProgress
}'

# 매출 트렌드만 조회
curl http://localhost:8080/api/businesses/1/dashboard | jq '.data.revenueTrend'

# 시간대 분석만 조회
curl http://localhost:8080/api/businesses/1/dashboard | jq '.data.timeSlotAnalysis'

# 목표 달성률만 조회
curl http://localhost:8080/api/businesses/1/dashboard | jq '.data.goalProgress'
```

---

## 🔧 목표 설정 방법

### Business 업데이트 API로 목표 설정

```bash
# PATCH /api/businesses/{businessId}
curl -X PATCH "http://localhost:8080/api/businesses/1" \
  -H "Content-Type: application/json" \
  -d '{
    "dailyRevenueGoal": 1000000,
    "monthlyRevenueGoal": 30000000,
    "monthlyNewCustomerGoal": 50
  }'
```

### 직접 DB 업데이트
```sql
UPDATE businesses
SET daily_revenue_goal = 1500000,
    monthly_revenue_goal = 40000000,
    monthly_new_customer_goal = 60
WHERE id = 1;
```

---

## 📊 비즈니스 인사이트 활용 예시

### 1. **매출 추세 분석**
```
전월 대비: ▲ 14.2%
전년 대비: ▲ 29.4%

💡 인사이트:
- 전년 대비 높은 성장률 → 사업 확장 고려
- 월별 트렌드 상승 → 마케팅 효과 검증
```

### 2. **피크 타임 최적화**
```
피크 타임: 14:00 (32건)
한산한 시간: 09:00 (8건)

💡 액션 아이템:
- 14:00 직원 추가 배치
- 09:00~11:00 조조 할인 (20% OFF)
- 점심 시간 쿠폰 발행
```

### 3. **목표 기반 동기부여**
```
월간 매출 달성률: 61.7%
남은 기간: 20일

💡 액션 아이템:
- 일평균 57만원 추가 필요
- VIP 고객 재방문 유도 캠페인
- 패키지 상품 프로모션
```

---

## 📁 생성/수정된 파일

### DTO (6개 신규)
```
✓ RevenueTrend.java           - 매출 트렌드
✓ MonthlyRevenue.java          - 월별 매출 데이터
✓ TimeSlotAnalysis.java        - 시간대 분석
✓ HourlyCount.java             - 시간대별 건수
✓ DayOfWeekCount.java          - 요일별 건수
✓ GoalProgress.java            - 목표 달성률
```

### Entity & Repository
```
✓ Business.java (수정)         - 목표 필드 3개 추가
✓ ReservationRepository.java   - 5개 메서드 추가
✓ BusinessMapper.xml (수정)    - 목표 필드 매핑
✓ ReservationMapper.xml        - 5개 쿼리 추가
```

### Service
```
✓ DashboardService.java        - 3개 메서드 추가
```

### DB Migration
```
✓ V2__add_goal_fields_to_businesses.sql
✓ apply-dashboard-migration.sql
```

### 문서 & 테스트
```
✓ DASHBOARD-PHASE3.md
✓ test-dashboard-phase3.sh
```

---

## ⚠️ 주의사항

### 목표 미설정 시
- 목표가 `NULL`인 경우 `goalProgress`의 달성률도 `null` 반환
- 프론트엔드에서 `null` 체크 필수

```javascript
if (goalProgress.dailyRevenueAchievement !== null) {
  // 달성률 표시
} else {
  // "목표 미설정" 메시지 표시
}
```

### 데이터 없을 때
- 신규 매장이거나 데이터가 없는 경우 0 또는 빈 배열 반환
- 그래프는 "데이터 없음" 상태 처리 필요

---

## 🚀 다음 단계 (옵션)

원하시면 추가 구현 가능:

1. **실시간 대시보드**
   - WebSocket으로 실시간 업데이트
   - 현재 진행중인 예약 표시

2. **AI 기반 예측**
   - 다음 달 매출 예측
   - 예약 수요 예측

3. **경쟁 분석**
   - 지역 평균 대비 성과
   - 업종 벤치마크

---

**구현 완료 일시**: 2026-02-09
**버전**: v1.2.0
**작성자**: Claude Sonnet 4.5
