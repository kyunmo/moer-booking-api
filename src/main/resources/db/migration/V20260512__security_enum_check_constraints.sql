-- =============================================================================
-- SECURITY (P3-1): VARCHAR enum 컬럼에 CHECK 제약 추가
-- 작성일: 2026-05-12
-- 목적: 애플리케이션 우회 또는 SQL 직접 입력 시 무효 enum 값 차단 (방어 계층)
-- 적용: 이미 데이터가 존재하는 기존 환경. 신규 환경은 schema.sql 의 인라인 정의 사용.
--
-- 적용 전 정합성 검증 (각 컬럼마다 수행):
--   SELECT DISTINCT status FROM businesses WHERE status NOT IN ('ACTIVE', 'INACTIVE', 'SUSPENDED');
--   결과가 비어있어야 ALTER 가능.
-- =============================================================================

-- users: schema.sql 의 신규 정의에 인라인 CHECK 가 있음. 기존 환경에 적용 시:
ALTER TABLE users ADD CONSTRAINT chk_users_role
    CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'OWNER', 'STAFF', 'CUSTOMER'));
ALTER TABLE users ADD CONSTRAINT chk_users_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED'));
ALTER TABLE users ADD CONSTRAINT chk_users_email_verified
    CHECK (email_verified IN ('Y', 'N'));
ALTER TABLE users ADD CONSTRAINT chk_users_is_premium
    CHECK (is_premium IN ('Y', 'N'));
ALTER TABLE users ADD CONSTRAINT chk_users_marketing_agree
    CHECK (marketing_agree IN ('Y', 'N'));

-- SECURITY (P3-4): 첫 로그인 시 비밀번호 변경 강제 플래그
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_change_required CHAR(1) DEFAULT 'N';
ALTER TABLE users ADD CONSTRAINT chk_users_password_change_required
    CHECK (password_change_required IN ('Y', 'N'));

-- businesses
ALTER TABLE businesses ADD CONSTRAINT chk_businesses_business_type
    CHECK (business_type IN ('BEAUTY_SHOP', 'PILATES', 'YOGA', 'CAFE', 'STUDY_CAFE',
                              'WORKSHOP', 'ACADEMY', 'PET_SALON', 'OTHER'));
ALTER TABLE businesses ADD CONSTRAINT chk_businesses_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED'));
ALTER TABLE businesses ADD CONSTRAINT chk_businesses_subscription_plan
    CHECK (subscription_plan IN ('FREE', 'BASIC'));
ALTER TABLE businesses ADD CONSTRAINT chk_businesses_billing_cycle
    CHECK (billing_cycle IN ('MONTHLY', 'YEARLY'));
ALTER TABLE businesses ADD CONSTRAINT chk_businesses_subscription_status
    CHECK (subscription_status IN ('TRIAL', 'ACTIVE', 'EXPIRED', 'CANCELED', 'SUSPENDED'));

-- reservations
ALTER TABLE reservations ADD CONSTRAINT chk_reservations_status
    CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'));

-- audit_logs
ALTER TABLE audit_logs ADD CONSTRAINT chk_audit_logs_user_role
    CHECK (user_role IS NULL OR user_role IN ('SUPER_ADMIN', 'ADMIN', 'OWNER', 'STAFF', 'CUSTOMER'));

-- business_coupons
ALTER TABLE business_coupons ADD CONSTRAINT chk_business_coupons_coupon_type
    CHECK (coupon_type IN ('PERCENTAGE', 'FIXED_AMOUNT'));
ALTER TABLE business_coupons ADD CONSTRAINT chk_business_coupons_status
    CHECK (status IN ('ACTIVE', 'EXPIRED', 'DISABLED'));

-- coupons (시스템 쿠폰)
ALTER TABLE coupons ADD CONSTRAINT chk_coupons_discount_type
    CHECK (discount_type IN ('PERCENTAGE', 'FIXED_AMOUNT'));
ALTER TABLE coupons ADD CONSTRAINT chk_coupons_status
    CHECK (status IN ('ACTIVE', 'EXPIRED', 'DISABLED'));

-- payments
ALTER TABLE payments ADD CONSTRAINT chk_payments_subscription_plan
    CHECK (subscription_plan IN ('FREE', 'BASIC'));
ALTER TABLE payments ADD CONSTRAINT chk_payments_billing_cycle
    CHECK (billing_cycle IS NULL OR billing_cycle IN ('MONTHLY', 'YEARLY'));
ALTER TABLE payments ADD CONSTRAINT chk_payments_method
    CHECK (payment_method IN ('CARD', 'BANK_TRANSFER', 'VIRTUAL_ACCOUNT', 'MOBILE'));
ALTER TABLE payments ADD CONSTRAINT chk_payments_status
    CHECK (payment_status IN ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED', 'CANCELLED'));

-- reviews
ALTER TABLE reviews ADD CONSTRAINT chk_reviews_status
    CHECK (status IN ('ACTIVE', 'HIDDEN', 'DELETED'));

-- inquiries
ALTER TABLE inquiries ADD CONSTRAINT chk_inquiries_type
    CHECK (type IN ('GENERAL', 'FEATURE_REQUEST', 'BUG_REPORT', 'PARTNERSHIP'));
ALTER TABLE inquiries ADD CONSTRAINT chk_inquiries_status
    CHECK (status IN ('PENDING', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'));

-- notification_logs
ALTER TABLE notification_logs ADD CONSTRAINT chk_notification_logs_status
    CHECK (status IN ('PENDING', 'SENT', 'FAILED'));

-- broadcasts
ALTER TABLE broadcasts ADD CONSTRAINT chk_broadcasts_target_type
    CHECK (target_type IN ('ALL', 'PAID', 'TRIAL', 'FREE'));
ALTER TABLE broadcasts ADD CONSTRAINT chk_broadcasts_priority
    CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'));
ALTER TABLE broadcasts ADD CONSTRAINT chk_broadcasts_status
    CHECK (status IN ('DRAFT', 'SENT'));

-- =============================================================================
-- SECURITY (P1-1): refresh_tokens 평문 토큰 → BCrypt 해시 컬럼으로 변경
-- 주의: 기존 토큰은 무효화됨 (해시 형태가 아니므로 검증 실패) → 전체 사용자 재로그인 필요
-- =============================================================================
ALTER TABLE refresh_tokens RENAME COLUMN token TO token_hash;
ALTER TABLE refresh_tokens ALTER COLUMN token_hash TYPE VARCHAR(255);
-- 기존 평문 토큰 unique index 제거 (해시 저장 후 user_id 기준 조회로 전환)
DROP INDEX IF EXISTS idx_refresh_tokens_token;
-- 무효화된 기존 토큰 일괄 삭제 권장:
-- DELETE FROM refresh_tokens;
