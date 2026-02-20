-- =============================================================================
-- businesses 테이블에 address_detail, zip_code 컬럼 추가
-- 생성일: 2026-02-21
-- =============================================================================

-- 상세주소 컬럼 추가
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS address_detail VARCHAR(200);
COMMENT ON COLUMN businesses.address_detail IS '상세주소 (예: 2층 201호)';

-- 우편번호 컬럼 추가
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS zip_code VARCHAR(10);
COMMENT ON COLUMN businesses.zip_code IS '우편번호 (예: 06234)';
