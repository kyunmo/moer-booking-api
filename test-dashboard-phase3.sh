#!/bin/bash

# Phase 3 대시보드 기능 테스트 스크립트

BUSINESS_ID=${1:-1}
DATE=${2:-$(date +%Y-%m-%d)}

echo "=========================================="
echo "📊 Phase 3 대시보드 테스트"
echo "=========================================="
echo "Business ID: $BUSINESS_ID"
echo "Date: $DATE"
echo ""

# 대시보드 API 호출
RESPONSE=$(curl -s "http://localhost:8080/api/businesses/$BUSINESS_ID/dashboard?date=$DATE")

echo "=========================================="
echo "📈 매출 트렌드 분석"
echo "=========================================="
echo "$RESPONSE" | jq '.data.revenueTrend | {
  "오늘 매출": "\(.todayRevenue)원",
  "전일 대비": "\(.dailyGrowthRate)%",
  "이번 주 매출": "\(.thisWeekRevenue)원",
  "전주 대비": "\(.weeklyGrowthRate)%",
  "이번 달 매출": "\(.thisMonthRevenue)원",
  "전월 대비": "\(.monthlyGrowthRate)%",
  "전년 대비": "\(.yearlyGrowthRate)%"
}'

echo ""
echo "📊 최근 6개월 월별 매출"
echo "$RESPONSE" | jq '.data.revenueTrend.monthlyRevenues[] | {
  "년월": .yearMonth,
  "매출": "\(.revenue)원",
  "예약 수": "\(.reservationCount)건"
}'

echo ""
echo "=========================================="
echo "⏰ 시간대별 분석"
echo "=========================================="
echo "피크 타임: $(echo "$RESPONSE" | jq -r '.data.timeSlotAnalysis.peakHour') ($(echo "$RESPONSE" | jq -r '.data.timeSlotAnalysis.peakHourCount')건)"
echo "한산한 시간: $(echo "$RESPONSE" | jq -r '.data.timeSlotAnalysis.offPeakHour') ($(echo "$RESPONSE" | jq -r '.data.timeSlotAnalysis.offPeakHourCount')건)"

echo ""
echo "시간대별 예약 분포 (TOP 5)"
echo "$RESPONSE" | jq '.data.timeSlotAnalysis.hourlyDistribution | sort_by(-.count) | .[0:5] | .[] | {
  "시간": .hour,
  "예약 수": "\(.count)건"
}'

echo ""
echo "요일별 예약 분포"
echo "$RESPONSE" | jq '.data.timeSlotAnalysis.weekdayDistribution[] | {
  "요일": .dayName,
  "예약 수": "\(.count)건"
}'

echo ""
echo "=========================================="
echo "🎯 목표 달성률"
echo "=========================================="

DAILY_GOAL=$(echo "$RESPONSE" | jq -r '.data.goalProgress.dailyRevenueGoal')
DAILY_ACHIEVEMENT=$(echo "$RESPONSE" | jq -r '.data.goalProgress.dailyRevenueAchievement')

echo "📌 일일 매출 목표"
if [ "$DAILY_GOAL" != "null" ]; then
  echo "  목표: ${DAILY_GOAL}원"
  echo "  현재: $(echo "$RESPONSE" | jq -r '.data.goalProgress.todayRevenue')원"
  echo "  달성률: ${DAILY_ACHIEVEMENT}%"

  if (( $(echo "$DAILY_ACHIEVEMENT >= 100" | bc -l) )); then
    echo "  상태: ✅ 목표 달성!"
  elif (( $(echo "$DAILY_ACHIEVEMENT >= 80" | bc -l) )); then
    echo "  상태: 🟡 목표 근접 (80% 이상)"
  else
    echo "  상태: 🔴 목표 미달"
  fi
else
  echo "  ⚠️ 목표가 설정되지 않았습니다"
fi

echo ""
echo "📌 월간 매출 목표"
MONTHLY_GOAL=$(echo "$RESPONSE" | jq -r '.data.goalProgress.monthlyRevenueGoal')
MONTHLY_ACHIEVEMENT=$(echo "$RESPONSE" | jq -r '.data.goalProgress.monthlyRevenueAchievement')

if [ "$MONTHLY_GOAL" != "null" ]; then
  echo "  목표: ${MONTHLY_GOAL}원"
  echo "  현재: $(echo "$RESPONSE" | jq -r '.data.goalProgress.thisMonthRevenue')원"
  echo "  달성률: ${MONTHLY_ACHIEVEMENT}%"

  if (( $(echo "$MONTHLY_ACHIEVEMENT >= 100" | bc -l) )); then
    echo "  상태: ✅ 목표 달성!"
  elif (( $(echo "$MONTHLY_ACHIEVEMENT >= 80" | bc -l) )); then
    echo "  상태: 🟡 목표 근접 (80% 이상)"
  else
    echo "  상태: 🔴 목표 미달"
  fi
else
  echo "  ⚠️ 목표가 설정되지 않았습니다"
fi

echo ""
echo "📌 월간 신규 고객 목표"
CUSTOMER_GOAL=$(echo "$RESPONSE" | jq -r '.data.goalProgress.monthlyNewCustomerGoal')
CUSTOMER_ACHIEVEMENT=$(echo "$RESPONSE" | jq -r '.data.goalProgress.monthlyNewCustomerAchievement')

if [ "$CUSTOMER_GOAL" != "null" ]; then
  echo "  목표: ${CUSTOMER_GOAL}명"
  echo "  현재: $(echo "$RESPONSE" | jq -r '.data.goalProgress.thisMonthNewCustomers')명"
  echo "  달성률: ${CUSTOMER_ACHIEVEMENT}%"

  if (( $(echo "$CUSTOMER_ACHIEVEMENT >= 100" | bc -l) )); then
    echo "  상태: ✅ 목표 달성!"
  elif (( $(echo "$CUSTOMER_ACHIEVEMENT >= 80" | bc -l) )); then
    echo "  상태: 🟡 목표 근접 (80% 이상)"
  else
    echo "  상태: 🔴 목표 미달"
  fi
else
  echo "  ⚠️ 목표가 설정되지 않았습니다"
fi

echo ""
echo "=========================================="
echo "✅ 테스트 완료"
echo "=========================================="
