-- ============================================
-- Migration: V008 - businesses 테이블 구독 컬럼 추가
-- 작성일: 2026-02-12
-- 목적: SaaS 구독 관리를 위한 컬럼 추가
-- ============================================

-- 1. 구독 관련 컬럼 추가
ALTER TABLE businesses
ADD COLUMN subscription_plan VARCHAR(20) DEFAULT 'FREE' NOT NULL,
ADD COLUMN subscription_status VARCHAR(20) DEFAULT 'TRIAL' NOT NULL,
ADD COLUMN trial_started_at TIMESTAMP,
ADD COLUMN trial_ends_at TIMESTAMP,
ADD COLUMN subscription_started_at TIMESTAMP,
ADD COLUMN next_billing_date TIMESTAMP;

-- 2. 기존 데이터 마이그레이션 (기존 매장은 FREE 플랜, 30일 체험 설정)
UPDATE businesses
SET trial_started_at = created_at,
    trial_ends_at = created_at + INTERVAL '30 days'
WHERE trial_started_at IS NULL;

-- 3. 코멘트 추가
COMMENT ON COLUMN businesses.subscription_plan IS '구독 플랜: FREE, BASIC, PRO, ENTERPRISE';
COMMENT ON COLUMN businesses.subscription_status IS '구독 상태: TRIAL, ACTIVE, EXPIRED, CANCELED, SUSPENDED';
COMMENT ON COLUMN businesses.trial_started_at IS '무료 체험 시작일';
COMMENT ON COLUMN businesses.trial_ends_at IS '무료 체험 종료일 (30일)';
COMMENT ON COLUMN businesses.subscription_started_at IS '유료 구독 시작일';
COMMENT ON COLUMN businesses.next_billing_date IS '다음 결제 예정일';
