-- V20260220: 고객 등급 임계값 설정 컬럼 추가
-- business_settings 테이블에 고객 등급 관련 설정 컬럼 추가

ALTER TABLE business_settings ADD COLUMN IF NOT EXISTS regular_threshold INTEGER DEFAULT 3;
ALTER TABLE business_settings ADD COLUMN IF NOT EXISTS vip_threshold INTEGER DEFAULT 10;
ALTER TABLE business_settings ADD COLUMN IF NOT EXISTS vip_benefit_description TEXT;

COMMENT ON COLUMN business_settings.regular_threshold IS '단골 고객 임계값 (방문 횟수, 기본값: 3)';
COMMENT ON COLUMN business_settings.vip_threshold IS 'VIP 고객 임계값 (방문 횟수, 기본값: 10)';
COMMENT ON COLUMN business_settings.vip_benefit_description IS 'VIP 혜택 설명';
