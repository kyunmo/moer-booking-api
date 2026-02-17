# Phase 5: 통계 페이지 - 백엔드 API 요청

## 개요
프론트엔드 통계 분석 페이지 구현을 위해 아래 API 엔드포인트 개발을 요청합니다.

> **기존 API**: 아래 3개는 이미 구현되어 있으므로 그대로 유지합니다.
> - `GET /businesses/{businessId}/dashboard` (기본 대시보드)
> - `GET /businesses/{businessId}/dashboard/stats` (기간별 통계)
> - `GET /businesses/{businessId}/dashboard/goals` (목표 달성률)

---

## API 1: 매출 분석

### `GET /businesses/{businessId}/statistics/revenue`

**설명**: 지정 기간의 매출 상세 분석 데이터

**Query Parameters**:
| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| startDate | String | Y | 시작일 (YYYY-MM-DD) | 2026-02-01 |
| endDate | String | Y | 종료일 (YYYY-MM-DD) | 2026-02-28 |
| groupBy | String | N | 집계 단위 (기본: daily) | daily / weekly / monthly |
| compareWith | String | N | 비교 기준 | PREVIOUS_PERIOD / LAST_YEAR |

**Response Body**:
```json
{
  "success": true,
  "data": {
    // 핵심 지표
    "summary": {
      "totalRevenue": 15620000,
      "averageRevenue": 520666,
      "averageTransactionAmount": 45000,
      "completionRate": 85.2,
      "customerLTV": 320000,
      "averageServiceDuration": 65
    },

    // 전기간 대비 변화율 (compareWith 파라미터 사용 시)
    "comparison": {
      "revenueChange": 12.5,
      "averageRevenueChange": 8.3,
      "transactionAmountChange": 5.1,
      "completionRateChange": -2.0
    },

    // 매출 추이 (groupBy 단위)
    "revenueTrend": [
      {
        "date": "2026-02-01",
        "revenue": 520000,
        "reservationCount": 12,
        "completedCount": 10
      }
    ],

    // 서비스별 매출 비중 (TOP 10 + 기타)
    "revenueByService": [
      {
        "serviceId": 1,
        "serviceName": "커트",
        "categoryName": "헤어",
        "revenue": 3500000,
        "percentage": 22.4,
        "reservationCount": 78
      }
    ],

    // 결제수단별 비중
    "revenueByPaymentMethod": [
      {
        "method": "CARD",
        "methodName": "카드",
        "revenue": 12000000,
        "percentage": 76.8,
        "count": 267
      },
      {
        "method": "CASH",
        "methodName": "현금",
        "revenue": 2620000,
        "percentage": 16.8,
        "count": 58
      },
      {
        "method": "TRANSFER",
        "methodName": "계좌이체",
        "revenue": 1000000,
        "percentage": 6.4,
        "count": 22
      }
    ],

    // 목표 달성률 (해당 월)
    "goals": {
      "revenueGoal": 20000000,
      "revenueAchievementRate": 78.1,
      "projectedRevenue": 18500000,
      "reservationGoal": 400,
      "reservationAchievementRate": 72.5,
      "projectedReservations": 380,
      "daysElapsed": 15,
      "daysRemaining": 13,
      "totalDays": 28
    }
  },
  "timestamp": "2026-02-15T10:00:00"
}
```

---

## API 2: 예약 분석

### `GET /businesses/{businessId}/statistics/reservations`

**설명**: 지정 기간의 예약 상세 분석 데이터

**Query Parameters**:
| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| startDate | String | Y | 시작일 (YYYY-MM-DD) | 2026-02-01 |
| endDate | String | Y | 종료일 (YYYY-MM-DD) | 2026-02-28 |
| groupBy | String | N | 집계 단위 (기본: daily) | daily / weekly / monthly |
| compareWith | String | N | 비교 기준 | PREVIOUS_PERIOD / LAST_YEAR |

**Response Body**:
```json
{
  "success": true,
  "data": {
    // 핵심 지표
    "summary": {
      "totalReservations": 347,
      "completedReservations": 295,
      "cancelledReservations": 32,
      "noShowReservations": 15,
      "pendingReservations": 5,
      "completionRate": 85.0,
      "cancellationRate": 9.2,
      "noShowRate": 4.3,
      "lostRevenue": 1420000
    },

    // 전기간 대비 변화율
    "comparison": {
      "totalChange": 8.5,
      "completedChange": 12.3,
      "cancelledChange": -15.2,
      "noShowChange": -5.0
    },

    // 예약 추이 (일별/주별/월별)
    "reservationTrend": [
      {
        "date": "2026-02-01",
        "total": 12,
        "completed": 10,
        "cancelled": 1,
        "noShow": 1,
        "pending": 0
      }
    ],

    // 시간대별 예약 히트맵 (요일 x 시간)
    "hourlyHeatmap": [
      {
        "dayOfWeek": 1,
        "dayName": "월",
        "hours": [
          { "hour": 9, "count": 3 },
          { "hour": 10, "count": 8 },
          { "hour": 11, "count": 12 },
          { "hour": 12, "count": 5 },
          { "hour": 13, "count": 7 },
          { "hour": 14, "count": 15 },
          { "hour": 15, "count": 18 },
          { "hour": 16, "count": 14 },
          { "hour": 17, "count": 10 },
          { "hour": 18, "count": 8 },
          { "hour": 19, "count": 5 },
          { "hour": 20, "count": 3 },
          { "hour": 21, "count": 1 }
        ]
      },
      {
        "dayOfWeek": 2,
        "dayName": "화",
        "hours": [...]
      }
    ],

    // 예약 상태 분포 (Donut 차트용)
    "statusDistribution": [
      { "status": "COMPLETED", "statusName": "완료", "count": 295, "percentage": 85.0 },
      { "status": "CANCELLED", "statusName": "취소", "count": 32, "percentage": 9.2 },
      { "status": "NO_SHOW", "statusName": "노쇼", "count": 15, "percentage": 4.3 },
      { "status": "PENDING", "statusName": "대기", "count": 5, "percentage": 1.5 }
    ],

    // 요일별 예약 분포
    "dailyDistribution": [
      { "dayOfWeek": 1, "dayName": "월", "averageCount": 12.5, "totalCount": 50 },
      { "dayOfWeek": 2, "dayName": "화", "averageCount": 14.0, "totalCount": 56 },
      { "dayOfWeek": 3, "dayName": "수", "averageCount": 11.0, "totalCount": 44 },
      { "dayOfWeek": 4, "dayName": "목", "averageCount": 13.5, "totalCount": 54 },
      { "dayOfWeek": 5, "dayName": "금", "averageCount": 16.0, "totalCount": 64 },
      { "dayOfWeek": 6, "dayName": "토", "averageCount": 18.0, "totalCount": 72 },
      { "dayOfWeek": 7, "dayName": "일", "averageCount": 1.8, "totalCount": 7 }
    ]
  },
  "timestamp": "2026-02-15T10:00:00"
}
```

---

## API 3: 고객 분석

### `GET /businesses/{businessId}/statistics/customers`

**설명**: 지정 기간의 고객 상세 분석 데이터

**Query Parameters**:
| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| startDate | String | Y | 시작일 (YYYY-MM-DD) | 2026-02-01 |
| endDate | String | Y | 종료일 (YYYY-MM-DD) | 2026-02-28 |
| compareWith | String | N | 비교 기준 | PREVIOUS_PERIOD / LAST_YEAR |

**Response Body**:
```json
{
  "success": true,
  "data": {
    // 핵심 지표
    "summary": {
      "totalCustomers": 245,
      "newCustomers": 32,
      "returningRate": 68.5,
      "averageVisitCount": 3.2,
      "averageLTV": 320000,
      "churnRate": 8.5
    },

    // 전기간 대비 변화율
    "comparison": {
      "totalCustomersChange": 5.2,
      "newCustomersChange": 15.0,
      "returningRateChange": 2.3,
      "averageVisitCountChange": -0.5
    },

    // 고객 증감 추이 (월별)
    "customerTrend": [
      {
        "date": "2026-01",
        "newCustomers": 28,
        "returningCustomers": 156,
        "totalActive": 184,
        "churned": 12
      }
    ],

    // 고객 세그먼트
    "segments": [
      {
        "segment": "VIP",
        "segmentName": "VIP",
        "description": "10회 이상 방문",
        "count": 15,
        "percentage": 6.1,
        "totalRevenue": 4800000,
        "averageRevenue": 320000
      },
      {
        "segment": "REGULAR",
        "segmentName": "단골",
        "description": "3~9회 방문",
        "count": 68,
        "percentage": 27.8,
        "totalRevenue": 8160000,
        "averageRevenue": 120000
      },
      {
        "segment": "NEW",
        "segmentName": "신규",
        "description": "1회 방문",
        "count": 120,
        "percentage": 49.0,
        "totalRevenue": 5400000,
        "averageRevenue": 45000
      },
      {
        "segment": "INACTIVE",
        "segmentName": "이탈",
        "description": "3개월 미방문",
        "count": 42,
        "percentage": 17.1,
        "totalRevenue": 0,
        "averageRevenue": 0
      }
    ],

    // 재방문율 추이 (월별)
    "returningRateTrend": [
      { "date": "2025-09", "rate": 62.0 },
      { "date": "2025-10", "rate": 64.5 },
      { "date": "2025-11", "rate": 65.0 },
      { "date": "2025-12", "rate": 66.8 },
      { "date": "2026-01", "rate": 67.2 },
      { "date": "2026-02", "rate": 68.5 }
    ],

    // 고객 LTV 분포
    "ltvDistribution": [
      { "range": "0~5만", "min": 0, "max": 50000, "count": 85 },
      { "range": "5~10만", "min": 50000, "max": 100000, "count": 62 },
      { "range": "10~30만", "min": 100000, "max": 300000, "count": 55 },
      { "range": "30~50만", "min": 300000, "max": 500000, "count": 28 },
      { "range": "50만 이상", "min": 500000, "max": null, "count": 15 }
    ]
  },
  "timestamp": "2026-02-15T10:00:00"
}
```

---

## API 4: 직원 성과

### `GET /businesses/{businessId}/statistics/staff`

**설명**: 지정 기간의 직원별 성과 분석 데이터

**Query Parameters**:
| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| startDate | String | Y | 시작일 (YYYY-MM-DD) | 2026-02-01 |
| endDate | String | Y | 종료일 (YYYY-MM-DD) | 2026-02-28 |
| staffId | Long | N | 특정 직원 필터 | 5 |
| compareWith | String | N | 비교 기준 | PREVIOUS_PERIOD / LAST_YEAR |

**Response Body**:
```json
{
  "success": true,
  "data": {
    // 전체 직원 성과 비교
    "staffPerformances": [
      {
        "staffId": 1,
        "staffName": "김디자이너",
        "positionName": "원장",
        "profileImageUrl": "/uploads/staff/1/profile.jpg",
        "reservationCount": 45,
        "completedCount": 42,
        "cancelledCount": 2,
        "noShowCount": 1,
        "totalRevenue": 3500000,
        "averageRevenue": 83333,
        "averageDuration": 65,
        "completionRate": 93.3,
        "customerCount": 38
      },
      {
        "staffId": 2,
        "staffName": "이실장",
        "positionName": "실장",
        "profileImageUrl": null,
        "reservationCount": 38,
        "completedCount": 35,
        "cancelledCount": 2,
        "noShowCount": 1,
        "totalRevenue": 2850000,
        "averageRevenue": 81428,
        "averageDuration": 58,
        "completionRate": 92.1,
        "customerCount": 32
      }
    ],

    // 전기간 대비 전체 변화율
    "comparison": {
      "totalReservationsChange": 8.5,
      "totalRevenueChange": 12.0
    },

    // 직원별 매출 추이 (staffId 지정 시 또는 전체)
    "staffRevenueTrend": [
      {
        "staffId": 1,
        "staffName": "김디자이너",
        "trend": [
          { "date": "2025-09", "revenue": 2800000, "reservationCount": 35 },
          { "date": "2025-10", "revenue": 3100000, "reservationCount": 39 },
          { "date": "2025-11", "revenue": 3200000, "reservationCount": 41 },
          { "date": "2025-12", "revenue": 3400000, "reservationCount": 43 },
          { "date": "2026-01", "revenue": 3300000, "reservationCount": 42 },
          { "date": "2026-02", "revenue": 3500000, "reservationCount": 45 }
        ]
      }
    ],

    // 직원별 역량 지표 (Radar 차트용, 0~100 정규화)
    "staffRadar": [
      {
        "staffId": 1,
        "staffName": "김디자이너",
        "metrics": {
          "reservationVolume": 95,
          "revenue": 92,
          "completionRate": 88,
          "customerSatisfaction": 90,
          "efficiency": 78
        }
      }
    ]
  },
  "timestamp": "2026-02-15T10:00:00"
}
```

---

## API 5: 서비스 분석

### `GET /businesses/{businessId}/statistics/services`

**설명**: 지정 기간의 서비스별 분석 데이터

**Query Parameters**:
| 파라미터 | 타입 | 필수 | 설명 | 예시 |
|---------|------|------|------|------|
| startDate | String | Y | 시작일 (YYYY-MM-DD) | 2026-02-01 |
| endDate | String | Y | 종료일 (YYYY-MM-DD) | 2026-02-28 |
| categoryId | Long | N | 카테고리 필터 | 3 |
| compareWith | String | N | 비교 기준 | PREVIOUS_PERIOD / LAST_YEAR |

**Response Body**:
```json
{
  "success": true,
  "data": {
    // 핵심 지표
    "summary": {
      "totalServiceCount": 347,
      "uniqueServiceTypes": 12,
      "averagePrice": 45000,
      "categoryCount": 4,
      "mostPopularService": "커트",
      "mostProfitableService": "펌"
    },

    // 전기간 대비 변화율
    "comparison": {
      "totalServiceCountChange": 8.5,
      "averagePriceChange": 2.1
    },

    // 서비스별 상세 (전체, 매출 순 정렬)
    "serviceRankings": [
      {
        "rank": 1,
        "serviceId": 1,
        "serviceName": "커트",
        "categoryId": 1,
        "categoryName": "헤어",
        "reservationCount": 78,
        "totalRevenue": 3500000,
        "averagePrice": 44871,
        "revenuePercentage": 22.4,
        "averageDuration": 35,
        "completionRate": 92.3
      },
      {
        "rank": 2,
        "serviceId": 3,
        "serviceName": "펌",
        "categoryId": 1,
        "categoryName": "헤어",
        "reservationCount": 42,
        "totalRevenue": 5040000,
        "averagePrice": 120000,
        "revenuePercentage": 32.3,
        "averageDuration": 120,
        "completionRate": 88.1
      }
    ],

    // 카테고리별 매출 비중
    "categoryDistribution": [
      {
        "categoryId": 1,
        "categoryName": "헤어",
        "serviceCount": 5,
        "reservationCount": 180,
        "revenue": 10500000,
        "percentage": 67.2
      },
      {
        "categoryId": 2,
        "categoryName": "네일",
        "serviceCount": 4,
        "reservationCount": 95,
        "revenue": 3325000,
        "percentage": 21.3
      },
      {
        "categoryId": 3,
        "categoryName": "속눈썹",
        "serviceCount": 3,
        "reservationCount": 72,
        "revenue": 1800000,
        "percentage": 11.5
      }
    ],

    // TOP 5 서비스 매출 추이 (월별)
    "serviceTrend": [
      {
        "serviceId": 1,
        "serviceName": "커트",
        "trend": [
          { "date": "2025-09", "revenue": 2800000, "count": 62 },
          { "date": "2025-10", "revenue": 3100000, "count": 69 },
          { "date": "2025-11", "revenue": 3000000, "count": 67 },
          { "date": "2025-12", "revenue": 3200000, "count": 71 },
          { "date": "2026-01", "revenue": 3400000, "count": 76 },
          { "date": "2026-02", "revenue": 3500000, "count": 78 }
        ]
      }
    ]
  },
  "timestamp": "2026-02-15T10:00:00"
}
```

---

## 기존 API 수정 사항

### `GET /businesses/{businessId}/dashboard` (수정 없음)
기존 대시보드 API는 그대로 유지합니다. 통계 페이지는 별도 엔드포인트를 사용합니다.

### `GET /businesses/{businessId}/dashboard/stats` (수정 없음)
기간별 통계도 기존 유지 (매출 분석 탭에서 재활용 가능).

### `GET /businesses/{businessId}/dashboard/goals` (수정 없음)
목표 달성률도 기존 유지 (매출 분석 탭에서 재활용 가능).

---

## 요약: 새로 개발 필요한 API 목록

| # | 엔드포인트 | 메서드 | 설명 |
|---|-----------|--------|------|
| 1 | `/businesses/{id}/statistics/revenue` | GET | 매출 분석 |
| 2 | `/businesses/{id}/statistics/reservations` | GET | 예약 분석 |
| 3 | `/businesses/{id}/statistics/customers` | GET | 고객 분석 |
| 4 | `/businesses/{id}/statistics/staff` | GET | 직원 성과 |
| 5 | `/businesses/{id}/statistics/services` | GET | 서비스 분석 |

### 공통 사항
- **인증**: Bearer Token (기존과 동일)
- **권한**: ADMIN, SUPER_ADMIN
- **기간 제한**: startDate/endDate 최대 1년 범위
- **캐싱**: 통계 데이터는 5분 캐시 권장 (실시간 불필요)
- **에러 응답**: 기존 형식 유지
  ```json
  {
    "success": false,
    "error": {
      "code": "ST001",
      "message": "시작일은 종료일보다 이전이어야 합니다"
    }
  }
  ```

### 에러 코드 (제안)
| 코드 | 설명 |
|------|------|
| ST001 | 날짜 범위 오류 (startDate > endDate) |
| ST002 | 기간 초과 (1년 초과) |
| ST003 | 유효하지 않은 groupBy 값 |
| ST004 | 존재하지 않는 staffId |
| ST005 | 존재하지 않는 categoryId |
