#!/bin/bash

# 예약 완료 API 테스트 스크립트
# 사용법: ./test-reservation-complete.sh {businessId} {reservationId}

BUSINESS_ID=${1:-1}
RESERVATION_ID=${2:-1}

echo "=========================================="
echo "예약 완료 API 테스트"
echo "=========================================="
echo "Business ID: $BUSINESS_ID"
echo "Reservation ID: $RESERVATION_ID"
echo ""

# 1. 예약 완료 API 호출
echo "📌 Step 1: 예약 완료 (PATCH /api/businesses/$BUSINESS_ID/reservations/$RESERVATION_ID/complete)"
COMPLETE_RESPONSE=$(curl -s -X PATCH "http://localhost:8080/api/businesses/$BUSINESS_ID/reservations/$RESERVATION_ID/complete" \
  -H "Content-Type: application/json")

echo "Response:"
echo "$COMPLETE_RESPONSE" | jq '.'
echo ""

# 2. 예약 정보 확인
echo "📌 Step 2: 예약 정보 조회 (GET /api/businesses/$BUSINESS_ID/reservations/$RESERVATION_ID)"
RESERVATION=$(curl -s "http://localhost:8080/api/businesses/$BUSINESS_ID/reservations/$RESERVATION_ID")
echo "$RESERVATION" | jq '.'

CUSTOMER_ID=$(echo "$RESERVATION" | jq -r '.data.customerId')
echo ""
echo "Customer ID: $CUSTOMER_ID"
echo ""

# 3. 고객 정보 확인 (통계 업데이트 확인)
echo "📌 Step 3: 고객 정보 조회 (GET /api/businesses/$BUSINESS_ID/customers/$CUSTOMER_ID)"
CUSTOMER=$(curl -s "http://localhost:8080/api/businesses/$BUSINESS_ID/customers/$CUSTOMER_ID")
echo "$CUSTOMER" | jq '.'

# 4. 통계 확인
VISIT_COUNT=$(echo "$CUSTOMER" | jq -r '.data.visitCount')
TOTAL_SPENT=$(echo "$CUSTOMER" | jq -r '.data.totalSpent')
LAST_VISIT=$(echo "$CUSTOMER" | jq -r '.data.lastVisitDate')
TAGS=$(echo "$CUSTOMER" | jq -r '.data.tags')

echo ""
echo "=========================================="
echo "✅ 고객 통계 확인"
echo "=========================================="
echo "방문 횟수: $VISIT_COUNT"
echo "총 결제액: $TOTAL_SPENT"
echo "최근 방문: $LAST_VISIT"
echo "태그: $TAGS"
echo "=========================================="
