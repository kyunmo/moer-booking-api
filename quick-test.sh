#!/bin/bash

# 빠른 테스트: 예약 생성 → 확정 → 완료 → 고객 통계 확인

BUSINESS_ID=1

echo "=========================================="
echo "🧪 예약 완료 통계 업데이트 테스트"
echo "=========================================="
echo ""

# 1. 예약 생성
echo "1️⃣ 예약 생성..."
CREATE_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/businesses/$BUSINESS_ID/reservations" \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "테스트고객",
    "customerPhone": "010-9999-9999",
    "serviceIds": [1],
    "reservationDate": "2026-02-15",
    "startTime": "14:00",
    "customerMemo": "통계 업데이트 테스트"
  }')

RESERVATION_ID=$(echo "$CREATE_RESPONSE" | jq -r '.data.id')
CUSTOMER_ID=$(echo "$CREATE_RESPONSE" | jq -r '.data.customerId')

echo "✅ 예약 생성 완료"
echo "   - 예약 ID: $RESERVATION_ID"
echo "   - 고객 ID: $CUSTOMER_ID"
echo ""

# 2. 고객 초기 상태 확인
echo "2️⃣ 고객 초기 상태 확인..."
CUSTOMER_BEFORE=$(curl -s "http://localhost:8080/api/businesses/$BUSINESS_ID/customers/$CUSTOMER_ID")
VISIT_BEFORE=$(echo "$CUSTOMER_BEFORE" | jq -r '.data.visitCount')
SPENT_BEFORE=$(echo "$CUSTOMER_BEFORE" | jq -r '.data.totalSpent')
echo "   - 방문 횟수: $VISIT_BEFORE"
echo "   - 총 결제액: $SPENT_BEFORE"
echo ""

# 3. 예약 확정
echo "3️⃣ 예약 확정 (PENDING → CONFIRMED)..."
curl -s -X PATCH "http://localhost:8080/api/businesses/$BUSINESS_ID/reservations/$RESERVATION_ID/confirm" > /dev/null
echo "✅ 확정 완료"
echo ""

# 4. 예약 완료 (통계 업데이트 발생)
echo "4️⃣ 예약 완료 (CONFIRMED → COMPLETED)..."
COMPLETE_RESPONSE=$(curl -s -X PATCH "http://localhost:8080/api/businesses/$BUSINESS_ID/reservations/$RESERVATION_ID/complete")
echo "✅ 완료 처리"
echo ""

# 5. 고객 통계 확인
echo "5️⃣ 고객 통계 확인 (업데이트 확인)..."
sleep 1  # DB 반영 대기
CUSTOMER_AFTER=$(curl -s "http://localhost:8080/api/businesses/$BUSINESS_ID/customers/$CUSTOMER_ID")
VISIT_AFTER=$(echo "$CUSTOMER_AFTER" | jq -r '.data.visitCount')
SPENT_AFTER=$(echo "$CUSTOMER_AFTER" | jq -r '.data.totalSpent')
LAST_VISIT=$(echo "$CUSTOMER_AFTER" | jq -r '.data.lastVisitDate')
TAGS=$(echo "$CUSTOMER_AFTER" | jq -r '.data.tags')

echo ""
echo "=========================================="
echo "📊 결과 비교"
echo "=========================================="
echo "방문 횟수: $VISIT_BEFORE → $VISIT_AFTER"
echo "총 결제액: $SPENT_BEFORE → $SPENT_AFTER"
echo "최근 방문: $LAST_VISIT"
echo "자동 태그: $TAGS"
echo ""

if [ "$VISIT_AFTER" -gt "$VISIT_BEFORE" ]; then
  echo "✅ 통계 업데이트 성공!"
else
  echo "❌ 통계 업데이트 실패!"
fi
echo "=========================================="
