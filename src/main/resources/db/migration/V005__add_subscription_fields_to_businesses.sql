-- ============================================
-- Migration: V005 - businesses 테이블 확장
-- 작성일: 2026-02-12
-- 목적: 구독 관리를 위한 사용량 카운터 추가
-- ============================================

-- 1. 사용량 카운터 컬럼 추가
ALTER TABLE businesses
ADD COLUMN current_staff_count INTEGER DEFAULT 0 NOT NULL,
ADD COLUMN current_month_reservation_count INTEGER DEFAULT 0 NOT NULL;

-- 2. 인덱스 추가 (구독 관리 성능 향상)
CREATE INDEX idx_businesses_subscription_status ON businesses(subscription_status);
CREATE INDEX idx_businesses_trial_ends_at ON businesses(trial_ends_at);

-- 3. 기존 데이터 동기화 - 활성 직원 수 계산
UPDATE businesses b
SET current_staff_count = (
    SELECT COUNT(*)
    FROM staff
    WHERE business_id = b.id
    AND status = 'ACTIVE'
);

-- 4. 기존 데이터 동기화 - 이번 달 예약 수 계산
UPDATE businesses b
SET current_month_reservation_count = (
    SELECT COUNT(*)
    FROM reservations
    WHERE business_id = b.id
    AND reservation_date >= DATE_TRUNC('month', CURRENT_DATE)
    AND status != 'CANCELED'
);

-- 5. 코멘트 추가
COMMENT ON COLUMN businesses.current_staff_count IS '현재 활성 직원 수 (플랜 제한 체크용)';
COMMENT ON COLUMN businesses.current_month_reservation_count IS '이번 달 예약 수 (플랜 제한 체크용)';
