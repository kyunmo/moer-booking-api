-- ============================================
-- Migration: V006 - payments 테이블 생성
-- 작성일: 2026-02-12
-- 목적: 구독 결제 내역 관리
-- ============================================

-- 1. 결제 상태 Enum 타입 생성
CREATE TYPE payment_status AS ENUM (
    'PENDING',      -- 결제 대기
    'COMPLETED',    -- 결제 완료
    'FAILED',       -- 결제 실패
    'REFUNDED'      -- 환불 완료
);

-- 2. 결제 수단 Enum 타입 생성
CREATE TYPE payment_method AS ENUM (
    'CARD',         -- 신용/체크카드
    'BANK_TRANSFER', -- 계좌이체
    'VIRTUAL_ACCOUNT', -- 가상계좌
    'MOBILE'        -- 모바일 결제
);

-- 3. payments 테이블 생성
CREATE TABLE payments (
    -- 기본키
    id BIGSERIAL PRIMARY KEY,

    -- 외래키
    business_id BIGINT NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    coupon_id BIGINT REFERENCES coupons(id),

    -- 구독 정보
    subscription_plan VARCHAR(20) NOT NULL,
    billing_period_start DATE NOT NULL,
    billing_period_end DATE NOT NULL,

    -- 금액 정보
    amount INTEGER NOT NULL,
    discount_amount INTEGER DEFAULT 0,
    final_amount INTEGER NOT NULL,

    -- 쿠폰 정보
    coupon_code VARCHAR(50),

    -- 결제 정보
    payment_method payment_method DEFAULT 'CARD',
    payment_status payment_status NOT NULL DEFAULT 'PENDING',
    pg_provider VARCHAR(50),
    pg_transaction_id VARCHAR(200),

    -- 웹훅 정보
    webhook_received_at TIMESTAMP,
    webhook_data JSONB,

    -- 메타데이터
    paid_at TIMESTAMP,
    failed_reason TEXT,
    refunded_at TIMESTAMP,
    refund_amount INTEGER,

    -- 타임스탬프
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. 인덱스 생성
CREATE INDEX idx_payments_business_id ON payments(business_id);
CREATE INDEX idx_payments_status ON payments(payment_status);
CREATE INDEX idx_payments_billing_period ON payments(billing_period_start, billing_period_end);
CREATE INDEX idx_payments_pg_transaction_id ON payments(pg_transaction_id);
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);

-- 5. 체크 제약조건 추가
ALTER TABLE payments ADD CONSTRAINT check_payment_amount
    CHECK (amount >= 0 AND discount_amount >= 0 AND final_amount >= 0);

ALTER TABLE payments ADD CONSTRAINT check_billing_period
    CHECK (billing_period_start <= billing_period_end);

-- 6. 코멘트 추가
COMMENT ON TABLE payments IS '구독 결제 내역';
COMMENT ON COLUMN payments.subscription_plan IS '구독 플랜: FREE, BASIC, PRO, ENTERPRISE';
COMMENT ON COLUMN payments.billing_period_start IS '청구 기간 시작일';
COMMENT ON COLUMN payments.billing_period_end IS '청구 기간 종료일';
COMMENT ON COLUMN payments.amount IS '원래 금액 (할인 전)';
COMMENT ON COLUMN payments.discount_amount IS '할인 금액';
COMMENT ON COLUMN payments.final_amount IS '최종 결제 금액';
COMMENT ON COLUMN payments.payment_status IS '결제 상태: PENDING, COMPLETED, FAILED, REFUNDED';
COMMENT ON COLUMN payments.payment_method IS '결제 수단: CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT, MOBILE';
COMMENT ON COLUMN payments.pg_provider IS 'PG사 이름 (예: toss, iamport)';
COMMENT ON COLUMN payments.pg_transaction_id IS 'PG사 거래 ID';
COMMENT ON COLUMN payments.webhook_data IS '웹훅으로 받은 원본 데이터 (JSON)';
