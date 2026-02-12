-- ============================================
-- Migration: V007 - coupons 및 coupon_usages 테이블 생성
-- 작성일: 2026-02-12
-- 목적: 쿠폰 관리 및 사용 이력 추적
-- ============================================

-- 1. 할인 타입 Enum 생성
CREATE TYPE discount_type AS ENUM (
    'PERCENTAGE',    -- 퍼센트 할인 (예: 20%)
    'FIXED_AMOUNT'   -- 고정 금액 할인 (예: 10,000원)
);

-- 2. 쿠폰 상태 Enum 생성
CREATE TYPE coupon_status AS ENUM (
    'ACTIVE',       -- 활성 (사용 가능)
    'INACTIVE',     -- 비활성 (일시 중지)
    'EXPIRED'       -- 만료됨
);

-- 3. coupons 테이블 생성
CREATE TABLE coupons (
    -- 기본키
    id BIGSERIAL PRIMARY KEY,

    -- 쿠폰 기본 정보
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,

    -- 할인 정보
    discount_type discount_type NOT NULL,
    discount_value INTEGER NOT NULL,
    max_discount_amount INTEGER,

    -- 적용 조건
    applicable_plans TEXT,
    min_purchase_amount INTEGER DEFAULT 0,

    -- 유효 기간
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,

    -- 사용 제한
    max_total_uses INTEGER,
    current_total_uses INTEGER DEFAULT 0,
    max_uses_per_business INTEGER DEFAULT 1,

    -- 상태
    status coupon_status DEFAULT 'ACTIVE',

    -- 메타데이터
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. coupon_usages 테이블 생성 (쿠폰 사용 내역)
CREATE TABLE coupon_usages (
    -- 기본키
    id BIGSERIAL PRIMARY KEY,

    -- 외래키
    coupon_id BIGINT NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    business_id BIGINT NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    payment_id BIGINT REFERENCES payments(id),

    -- 사용 정보
    discount_amount INTEGER NOT NULL,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- 유니크 제약 (같은 매장이 같은 쿠폰을 중복 사용 방지)
    UNIQUE(coupon_id, business_id)
);

-- 5. coupons 테이블 인덱스 생성
CREATE INDEX idx_coupons_code ON coupons(code);
CREATE INDEX idx_coupons_status ON coupons(status);
CREATE INDEX idx_coupons_valid_period ON coupons(valid_from, valid_until);
CREATE INDEX idx_coupons_created_at ON coupons(created_at DESC);

-- 6. coupon_usages 테이블 인덱스 생성
CREATE INDEX idx_coupon_usages_coupon_id ON coupon_usages(coupon_id);
CREATE INDEX idx_coupon_usages_business_id ON coupon_usages(business_id);
CREATE INDEX idx_coupon_usages_payment_id ON coupon_usages(payment_id);
CREATE INDEX idx_coupon_usages_used_at ON coupon_usages(used_at DESC);

-- 7. 체크 제약조건 추가
ALTER TABLE coupons ADD CONSTRAINT check_discount_value
    CHECK (discount_value > 0);

ALTER TABLE coupons ADD CONSTRAINT check_max_discount_amount
    CHECK (max_discount_amount IS NULL OR max_discount_amount > 0);

ALTER TABLE coupons ADD CONSTRAINT check_min_purchase_amount
    CHECK (min_purchase_amount >= 0);

ALTER TABLE coupons ADD CONSTRAINT check_valid_period
    CHECK (valid_from < valid_until);

ALTER TABLE coupons ADD CONSTRAINT check_max_uses
    CHECK (max_total_uses IS NULL OR max_total_uses > 0);

ALTER TABLE coupons ADD CONSTRAINT check_current_uses
    CHECK (current_total_uses >= 0);

ALTER TABLE coupons ADD CONSTRAINT check_uses_per_business
    CHECK (max_uses_per_business > 0);

ALTER TABLE coupon_usages ADD CONSTRAINT check_discount_amount
    CHECK (discount_amount >= 0);

-- 8. 코멘트 추가
COMMENT ON TABLE coupons IS '쿠폰 관리 테이블';
COMMENT ON COLUMN coupons.code IS '쿠폰 코드 (대소문자 구분, 유니크)';
COMMENT ON COLUMN coupons.name IS '쿠폰 이름';
COMMENT ON COLUMN coupons.description IS '쿠폰 설명';
COMMENT ON COLUMN coupons.discount_type IS '할인 타입: PERCENTAGE(퍼센트), FIXED_AMOUNT(고정금액)';
COMMENT ON COLUMN coupons.discount_value IS '할인 값 (PERCENTAGE일 경우 1~100, FIXED_AMOUNT일 경우 원 단위)';
COMMENT ON COLUMN coupons.max_discount_amount IS '최대 할인 금액 (PERCENTAGE 타입일 때만 사용)';
COMMENT ON COLUMN coupons.applicable_plans IS '적용 가능 플랜 (쉼표 구분): BASIC,PRO 또는 null(모든 플랜)';
COMMENT ON COLUMN coupons.min_purchase_amount IS '최소 구매 금액';
COMMENT ON COLUMN coupons.valid_from IS '쿠폰 유효 시작일';
COMMENT ON COLUMN coupons.valid_until IS '쿠폰 유효 종료일';
COMMENT ON COLUMN coupons.max_total_uses IS '전체 사용 가능 횟수 (null이면 무제한)';
COMMENT ON COLUMN coupons.current_total_uses IS '현재 사용된 횟수';
COMMENT ON COLUMN coupons.max_uses_per_business IS '매장당 사용 가능 횟수';
COMMENT ON COLUMN coupons.status IS '쿠폰 상태: ACTIVE, INACTIVE, EXPIRED';
COMMENT ON COLUMN coupons.created_by IS '쿠폰 생성자 (SUPER_ADMIN)';

COMMENT ON TABLE coupon_usages IS '쿠폰 사용 내역';
COMMENT ON COLUMN coupon_usages.discount_amount IS '실제 할인된 금액';
COMMENT ON COLUMN coupon_usages.used_at IS '쿠폰 사용 시각';
