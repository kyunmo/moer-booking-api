-- =============================================================================
-- SECURITY (P3-1): VARCHAR enum 컬럼에 CHECK 제약 추가
-- 작성일: 2026-05-12
-- 목적: 애플리케이션 우회 또는 SQL 직접 입력 시 무효 enum 값 차단 (방어 계층)
-- =============================================================================

-- users: role, status, *_flag (Y/N) — schema.sql 에서 직접 정의됨 (신규 배포 기준)
-- 기존 환경(이미 데이터 있는 경우) 마이그레이션 시 아래 ALTER 사용:
--   ALTER TABLE users ADD CONSTRAINT chk_users_role
--     CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'OWNER', 'STAFF', 'CUSTOMER'));
--   (기타 컬럼도 동일 패턴)

-- businesses
ALTER TABLE businesses ADD CONSTRAINT chk_businesses_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'DELETED'));
ALTER TABLE businesses ADD CONSTRAINT chk_businesses_subscription_status
    CHECK (subscription_status IN ('TRIAL', 'ACTIVE', 'EXPIRED', 'CANCELLED'));

-- reservations
ALTER TABLE reservations ADD CONSTRAINT chk_reservations_status
    CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'));
ALTER TABLE reservations ADD CONSTRAINT chk_reservations_payment_method
    CHECK (payment_method IS NULL OR payment_method IN ('CASH', 'CARD', 'TRANSFER', 'OTHER'));

-- customers
ALTER TABLE customers ADD CONSTRAINT chk_customers_type
    CHECK (type IN ('REGULAR', 'VIP', 'NEW', 'INACTIVE'));

-- audit_logs
ALTER TABLE audit_logs ADD CONSTRAINT chk_audit_logs_user_role
    CHECK (user_role IS NULL OR user_role IN ('SUPER_ADMIN', 'ADMIN', 'OWNER', 'STAFF', 'CUSTOMER', 'SYSTEM'));

-- business_coupons
ALTER TABLE business_coupons ADD CONSTRAINT chk_business_coupons_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED'));

-- subscription_coupons (관리자 쿠폰)
ALTER TABLE subscription_coupons ADD CONSTRAINT chk_subscription_coupons_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED'));
ALTER TABLE subscription_coupons ADD CONSTRAINT chk_subscription_coupons_discount_type
    CHECK (discount_type IN ('PERCENT', 'FIXED'));

-- payments
ALTER TABLE payments ADD CONSTRAINT chk_payments_method
    CHECK (payment_method IN ('CARD', 'TRANSFER', 'OTHER'));
ALTER TABLE payments ADD CONSTRAINT chk_payments_status
    CHECK (payment_status IN ('PENDING', 'COMPLETED', 'FAILED', 'CANCELLED', 'REFUNDED'));

-- inquiries
ALTER TABLE inquiries ADD CONSTRAINT chk_inquiries_status
    CHECK (status IN ('PENDING', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'));

-- notification_logs
ALTER TABLE notification_logs ADD CONSTRAINT chk_notification_logs_status
    CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'BOUNCED'));

-- broadcasts
ALTER TABLE broadcasts ADD CONSTRAINT chk_broadcasts_target_type
    CHECK (target_type IN ('ALL', 'ROLE', 'BUSINESS', 'CUSTOM'));
ALTER TABLE broadcasts ADD CONSTRAINT chk_broadcasts_status
    CHECK (status IN ('DRAFT', 'SCHEDULED', 'SENT', 'CANCELLED'));

-- 참고: 실제 운영 마이그레이션 적용 전, 기존 데이터에 위 enum 값 외의 값이 있는지 검증 필요:
-- SELECT DISTINCT role FROM users WHERE role NOT IN ('SUPER_ADMIN', 'ADMIN', 'OWNER', 'STAFF', 'CUSTOMER');
-- (각 컬럼 별로 수행 후 정합성 OK 시 위 ALTER 실행)
