#!/bin/bash

# 향상된 대시보드 API 테스트 스크립트

BUSINESS_ID=${1:-1}
DATE=${2:-$(date +%Y-%m-%d)}

echo "=========================================="
echo "📊 향상된 대시보드 테스트"
echo "=========================================="
echo "Business ID: $BUSINESS_ID"
echo "Date: $DATE"
echo ""

# 대시보드 API 호출
echo "📌 대시보드 전체 데이터 조회"
echo "GET /api/businesses/$BUSINESS_ID/dashboard?date=$DATE"
echo ""

RESPONSE=$(curl -s "http://localhost:8080/api/businesses/$BUSINESS_ID/dashboard?date=$DATE")

echo "=========================================="
echo "✅ 응답 데이터"
echo "=========================================="
echo "$RESPONSE" | jq '.'

echo ""
echo "=========================================="
echo "📈 주요 통계 요약"
echo "=========================================="

# 취소/노쇼 통계
echo ""
echo "🚫 취소/노쇼 현황"
echo "$RESPONSE" | jq '.data.cancellationStats | {
  "취소 건수": .cancelledCount,
  "노쇼 건수": .noShowCount,
  "취소율": "\(.cancellationRate)%",
  "노쇼율": "\(.noShowRate)%",
  "매출 손실액": "\(.lostRevenue)원"
}'

# 액션 알림
echo ""
echo "⚠️ 처리 필요 알림"
TOTAL_ALERTS=$(echo "$RESPONSE" | jq '.data.actionAlerts | (.pendingReservations + .upcomingReservations + .birthdayCustomers + .inactiveCustomers)')
echo "전체 알림: $TOTAL_ALERTS건"
echo "$RESPONSE" | jq '.data.actionAlerts | {
  "확정 대기": .pendingReservations,
  "1시간 내 시작": .upcomingReservations,
  "생일 고객": .birthdayCustomers,
  "재방문 유도": .inactiveCustomers
}'

# 고객 세그먼트
echo ""
echo "👥 고객 세그먼트"
echo "$RESPONSE" | jq '.data.customerSegments | {
  "VIP": .vipCount,
  "단골": .regularCount,
  "신규": .newCount,
  "이탈": .inactiveCount,
  "전체": .totalCustomers,
  "재방문율": "\(.returningRate)%"
}'

# 직원 성과 TOP 3
echo ""
echo "👤 직원 성과 TOP 3"
echo "$RESPONSE" | jq '.data.topStaffPerformances[] | {
  "이름": .staffName,
  "예약 수": .reservationCount,
  "매출": "\(.totalRevenue)원",
  "평균 시간": "\(.averageDuration)분"
}'

# 인기 서비스 TOP 5
echo ""
echo "📋 인기 서비스 TOP 5"
echo "$RESPONSE" | jq '.data.popularServices[] | {
  "서비스명": .serviceName,
  "예약 수": .reservationCount,
  "매출": "\(.totalRevenue)원",
  "매출 비중": "\(.revenuePercentage)%"
}'

# 평균 지표
echo ""
echo "📊 평균 지표"
echo "$RESPONSE" | jq '.data.averageMetrics | {
  "평균 예약 금액": "\(.averageReservationAmount)원",
  "평균 서비스 시간": "\(.averageServiceDuration)분",
  "평균 방문 횟수": .averageVisitCount,
  "평균 LTV": "\(.averageCustomerLifetimeValue)원",
  "완료 전환율": "\(.completionRate)%"
}'

echo ""
echo "=========================================="
echo "✅ 테스트 완료"
echo "=========================================="
