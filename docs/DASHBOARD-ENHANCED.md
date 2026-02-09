# 📊 향상된 대시보드 API 문서

## 개요

예약 관리 시스템의 대시보드에 6개 카테고리의 새로운 통계 기능이 추가되었습니다.

## 🎯 추가된 기능

### Phase 1: 즉시 유용한 통계

#### 1. **취소/노쇼 현황** (`cancellationStats`)
- 이번 달 취소 건수 및 취소율
- 이번 달 노쇼 건수 및 노쇼율
- 취소/노쇼로 인한 매출 손실액

#### 2. **실시간 액션 알림** (`actionAlerts`)
- 확정 대기중 예약 수 (PENDING 상태)
- 1시간 이내 시작 예약 수
- 오늘 생일 고객 수
- 재방문 유도 대상 고객 수 (1개월 이상 미방문)
- 전체 알림 수 (자동 계산)

#### 3. **고객 세그먼트 분석** (`customerSegments`)
- VIP 고객 수 (10회 이상)
- 단골 고객 수 (3~9회)
- 신규 고객 수 (1회)
- 이탈 고객 수 (3개월 이상 미방문)
- 전체 고객 수
- 고객 재방문율

### Phase 2: 핵심 분석

#### 4. **직원별 성과 TOP 3** (`topStaffPerformances`)
- 직원 ID, 이름
- 이번 달 예약 수
- 이번 달 매출 (완료 예약 기준)
- 평균 서비스 시간

#### 5. **인기 서비스 TOP 5** (`popularServices`)
- 서비스 ID, 이름
- 이번 달 예약 수
- 이번 달 매출
- 평균 가격
- 매출 비중 (%)

#### 6. **평균 지표** (`averageMetrics`)
- 평균 예약 금액 (완료 예약 기준)
- 평균 서비스 시간
- 고객당 평균 방문 횟수
- 고객당 평균 결제액 (LTV)
- 예약 → 완료 전환율 (최근 1개월)

---

## 📡 API 엔드포인트

### **GET** `/api/businesses/{businessId}/dashboard`

#### Query Parameters
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `date` | LocalDate | X | 오늘 | 기준 날짜 (yyyy-MM-dd) |

#### Response

```json
{
  "success": true,
  "data": {
    // 기존 통계
    "todayStats": {
      "totalReservations": 12,
      "pendingReservations": 3,
      "confirmedReservations": 5,
      "completedReservations": 4,
      "expectedRevenue": 850000
    },
    "weekStats": {
      "totalReservations": 68,
      "totalRevenue": 4200000,
      "dailyCounts": [
        { "date": "2026-02-03", "count": 10 },
        { "date": "2026-02-04", "count": 12 }
      ]
    },
    "monthStats": {
      "totalReservations": 245,
      "totalRevenue": 18500000,
      "newCustomers": 32
    },

    // 신규 추가 - Phase 1
    "cancellationStats": {
      "cancelledCount": 5,
      "noShowCount": 2,
      "cancellationRate": 3.2,
      "noShowRate": 1.3,
      "lostRevenue": 350000
    },
    "actionAlerts": {
      "pendingReservations": 3,
      "upcomingReservations": 2,
      "birthdayCustomers": 1,
      "inactiveCustomers": 8
    },
    "customerSegments": {
      "vipCount": 45,
      "regularCount": 128,
      "newCount": 32,
      "inactiveCount": 18,
      "totalCustomers": 256,
      "returningRate": 68.5
    },

    // 신규 추가 - Phase 2
    "topStaffPerformances": [
      {
        "staffId": 1,
        "staffName": "김철수",
        "reservationCount": 68,
        "totalRevenue": 5200000,
        "averageDuration": 75
      },
      {
        "staffId": 2,
        "staffName": "이영희",
        "reservationCount": 52,
        "totalRevenue": 4100000,
        "averageDuration": 80
      }
    ],
    "popularServices": [
      {
        "serviceId": 1,
        "serviceName": "커트",
        "reservationCount": 120,
        "totalRevenue": 6000000,
        "averagePrice": 50000,
        "revenuePercentage": 32.4
      },
      {
        "serviceId": 2,
        "serviceName": "펌",
        "reservationCount": 68,
        "totalRevenue": 5440000,
        "averagePrice": 80000,
        "revenuePercentage": 29.4
      }
    ],
    "averageMetrics": {
      "averageReservationAmount": 75400,
      "averageServiceDuration": 78,
      "averageVisitCount": 4.2,
      "averageCustomerLifetimeValue": 316800,
      "completionRate": 82.5
    },

    // 기존 실시간 데이터
    "recentReservations": [...],
    "recentCustomers": [...]
  }
}
```

---

## 🧪 테스트 방법

### 1. **Swagger UI**
```
http://localhost:8080/swagger-ui.html

Dashboard Controller → GET /api/businesses/{businessId}/dashboard
```

### 2. **curl**
```bash
curl http://localhost:8080/api/businesses/1/dashboard | jq '.'
```

### 3. **테스트 스크립트**
```bash
bash test-dashboard-enhanced.sh 1
```

---

## 📊 UI 표시 권장사항

### 레이아웃 예시

```
┌─────────────────────────────────────────────────────────┐
│  📊 대시보드 - 2026년 2월 9일                           │
├─────────────────────────────────────────────────────────┤
│  ⚠️ 처리 필요 (14건)                                    │
│    • 확정 대기 3건  • 1시간 내 시작 2건                │
│    • 생일 고객 1건  • 재방문 유도 8건                  │
├─────────────────────────────────────────────────────────┤
│  📈 오늘 통계                    🚫 이번 달 취소/노쇼   │
│  예약 12건 | 완료 4건            취소 5건 (3.2%)       │
│  매출 850K | 예상 850K           노쇼 2건 (1.3%)       │
│                                   손실 350K            │
├─────────────────────────────────────────────────────────┤
│  👥 고객 현황          📋 인기 서비스                   │
│  VIP: 45명             1. 커트 (120건, 32.4%)          │
│  단골: 128명           2. 펌 (68건, 29.4%)             │
│  신규: 32명            3. 염색 (45건, 18.2%)           │
│  이탈: 18명                                            │
│  재방문율: 68.5%                                       │
├─────────────────────────────────────────────────────────┤
│  👤 직원 성과 TOP 3                                     │
│  1. 김철수 (68건, 5.2M)  2. 이영희 (52건, 4.1M)       │
├─────────────────────────────────────────────────────────┤
│  📊 평균 지표                                           │
│  예약 금액: 75K | 서비스 시간: 78분 | LTV: 317K       │
│  방문 횟수: 4.2회 | 완료 전환율: 82.5%                │
└─────────────────────────────────────────────────────────┘
```

### 색상 가이드

- **빨강** (위험): 노쇼율 5% 이상, 취소율 10% 이상
- **노랑** (주의): 확정 대기 5건 이상, 이탈 고객 20명 이상
- **초록** (좋음): 완료 전환율 80% 이상, 재방문율 70% 이상

---

## 🔧 확장 가능 기능

### 향후 추가 고려사항

1. **매출 트렌드**
   - 전월 대비 증감률
   - 전년 대비 증감률
   - 월별 그래프 데이터

2. **시간대별 분석**
   - 시간대별 예약 분포
   - 피크 타임 표시

3. **목표 달성률**
   - 월간 매출 목표 대비
   - 일일 예약 목표 대비

---

## 📝 변경 파일 목록

### DTO (7개 신규 파일)
- `CancellationStats.java`
- `ActionAlerts.java`
- `CustomerSegments.java`
- `StaffPerformance.java`
- `ServiceStats.java`
- `AverageMetrics.java`
- `DashboardResponse.java` (수정)

### Repository
- `ReservationRepository.java` (10개 메서드 추가)
- `CustomerRepository.java` (6개 메서드 추가)

### MyBatis Mapper
- `ReservationMapper.xml` (7개 쿼리 추가)
- `CustomerMapper.xml` (6개 쿼리 추가)

### Service
- `DashboardService.java` (6개 메서드 추가, getDashboardStats 수정)

---

## ✅ 테스트 체크리스트

- [ ] 취소/노쇼 통계 정상 출력
- [ ] 액션 알림 총 개수 계산 정확
- [ ] 고객 세그먼트 합계 일치
- [ ] 직원 성과 매출 순 정렬
- [ ] 인기 서비스 매출 비중 100% 근접
- [ ] 평균 지표 소수점 처리 정상
- [ ] 생일 고객 날짜 정확
- [ ] 1시간 내 예약 시간 필터링 정상

---

**구현 완료 일시**: 2026-02-09
**버전**: v1.1.0
**작성자**: Claude Sonnet 4.5
