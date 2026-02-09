-- Phase 3 대시보드 기능을 위한 DB 마이그레이션
-- 실행 방법: docker exec moer-postgresql psql -U moer -d moer_dev -f /path/to/this/file.sql

\echo '=========================================='
\echo '📊 대시보드 Phase 3 마이그레이션 시작'
\echo '=========================================='
\echo ''

-- Business 테이블에 목표 필드 추가
\echo '1️⃣ Business 테이블에 목표 필드 추가...'
ALTER TABLE businesses
ADD COLUMN IF NOT EXISTS daily_revenue_goal INTEGER DEFAULT NULL,
ADD COLUMN IF NOT EXISTS monthly_revenue_goal INTEGER DEFAULT NULL,
ADD COLUMN IF NOT EXISTS monthly_new_customer_goal INTEGER DEFAULT NULL;

-- 코멘트 추가
COMMENT ON COLUMN businesses.daily_revenue_goal IS '일일 매출 목표 (원)';
COMMENT ON COLUMN businesses.monthly_revenue_goal IS '월간 매출 목표 (원)';
COMMENT ON COLUMN businesses.monthly_new_customer_goal IS '월간 신규 고객 목표 (명)';

\echo '✅ 목표 필드 추가 완료'
\echo ''

-- 예시 데이터 추가 (테스트용)
\echo '2️⃣ 테스트 데이터 추가...'
UPDATE businesses
SET daily_revenue_goal = 1000000,
    monthly_revenue_goal = 30000000,
    monthly_new_customer_goal = 50
WHERE id = 1;

\echo '✅ 테스트 데이터 추가 완료'
\echo ''

-- 확인
\echo '3️⃣ 적용 결과 확인...'
SELECT
    id,
    name,
    daily_revenue_goal,
    monthly_revenue_goal,
    monthly_new_customer_goal
FROM businesses
WHERE id = 1;

\echo ''
\echo '=========================================='
\echo '✅ 마이그레이션 완료'
\echo '=========================================='
