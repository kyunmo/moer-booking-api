-- Phase 1: 30일 체험판 시스템 추가
-- Users 테이블에 체험판 관련 컬럼 추가

ALTER TABLE users ADD COLUMN trial_started_at TIMESTAMP;
ALTER TABLE users ADD COLUMN trial_expires_at TIMESTAMP;
ALTER TABLE users ADD COLUMN is_premium CHAR(1) DEFAULT 'N';

-- 인덱스 추가 (체험판 만료 확인 쿼리 최적화)
CREATE INDEX idx_users_trial_expires ON users(trial_expires_at)
  WHERE is_premium = 'N';

-- 기존 사용자들에 대해 체험판 자동 설정 (선택사항)
-- UPDATE users
-- SET trial_started_at = created_at,
--     trial_expires_at = created_at + INTERVAL '30 days',
--     is_premium = 'N'
-- WHERE trial_started_at IS NULL;
