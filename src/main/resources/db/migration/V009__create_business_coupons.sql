-- ============================================
-- Migration: V009 - 매장별 쿠폰 시스템
-- 작성일: 2026-02-12
-- 목적: 매장별로 쿠폰을 생성하고 관리하는 시스템 추가
-- ============================================

-- 1. 쿠폰 타입 Enum (이미 존재하면 스킵)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'coupon_type') THEN
        CREATE TYPE coupon_type AS ENUM ('PERCENTAGE', 'FIXED_AMOUNT');
    END IF;
END$$;

-- 2. 매장 쿠폰 테이블 (기존 coupons와 별도)
CREATE TABLE IF NOT EXISTS business_coupons (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,

    -- 쿠폰 정보
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,

    -- 할인 정보
    coupon_type coupon_type NOT NULL,
    discount_amount INTEGER,         -- 정액 할인 금액
    discount_percentage INTEGER,     -- 정률 할인 비율 (0~100)
    max_discount_amount INTEGER,     -- 정률 할인 시 최대 할인 금액

    -- 사용 조건
    min_order_amount INTEGER DEFAULT 0,  -- 최소 주문 금액
    max_usage_count INTEGER,             -- 최대 사용 횟수 (NULL이면 무제한)
    current_usage_count INTEGER DEFAULT 0,  -- 현재 사용 횟수

    -- 유효 기간
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,

    -- 상태
    status coupon_status NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. 매장 쿠폰 사용 내역 테이블
CREATE TABLE IF NOT EXISTS business_coupon_usages (
    id BIGSERIAL PRIMARY KEY,
    coupon_id BIGINT NOT NULL REFERENCES business_coupons(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id),
    payment_id BIGINT REFERENCES payments(id) ON DELETE SET NULL,

    -- 사용 정보
    discount_amount INTEGER NOT NULL,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 취소 정보
    canceled CHAR(1) DEFAULT 'N',
    canceled_at TIMESTAMP
);

-- 4. 인덱스 생성
CREATE INDEX idx_business_coupons_business_id ON business_coupons(business_id);
CREATE INDEX idx_business_coupons_code ON business_coupons(code);
CREATE INDEX idx_business_coupons_status ON business_coupons(status);
CREATE INDEX idx_business_coupons_valid_until ON business_coupons(valid_until);

CREATE INDEX idx_business_coupon_usages_coupon_id ON business_coupon_usages(coupon_id);
CREATE INDEX idx_business_coupon_usages_user_id ON business_coupon_usages(user_id);
CREATE INDEX idx_business_coupon_usages_payment_id ON business_coupon_usages(payment_id);

-- 5. 코멘트 추가
COMMENT ON TABLE business_coupons IS '매장별 쿠폰 관리';
COMMENT ON COLUMN business_coupons.coupon_type IS 'PERCENTAGE(정률), FIXED_AMOUNT(정액)';
COMMENT ON TABLE business_coupon_usages IS '매장 쿠폰 사용 내역';
