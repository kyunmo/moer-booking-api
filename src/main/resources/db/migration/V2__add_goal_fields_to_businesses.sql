-- 대시보드 목표 달성률 기능을 위한 필드 추가
-- Business 테이블에 목표 관련 컬럼 추가

ALTER TABLE businesses
ADD COLUMN IF NOT EXISTS daily_revenue_goal INTEGER DEFAULT NULL,
ADD COLUMN IF NOT EXISTS monthly_revenue_goal INTEGER DEFAULT NULL,
ADD COLUMN IF NOT EXISTS monthly_new_customer_goal INTEGER DEFAULT NULL;

-- 코멘트 추가
COMMENT ON COLUMN businesses.daily_revenue_goal IS '일일 매출 목표 (원)';
COMMENT ON COLUMN businesses.monthly_revenue_goal IS '월간 매출 목표 (원)';
COMMENT ON COLUMN businesses.monthly_new_customer_goal IS '월간 신규 고객 목표 (명)';

-- 예시 데이터 (테스트용)
UPDATE businesses
SET daily_revenue_goal = 1000000,
    monthly_revenue_goal = 30000000,
    monthly_new_customer_goal = 50
WHERE id = 1;
