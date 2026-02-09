-- 데이터베이스 직접 조회로 통계 확인
-- PostgreSQL 명령어: docker exec -it moer-postgresql psql -U postgres -d moer_booking -f check-db-stats.sql

\echo '=========================================='
\echo '🔍 예약 및 고객 통계 확인'
\echo '=========================================='
\echo ''

-- 1. 최근 예약 조회 (상태별)
\echo '📌 1. 최근 예약 목록 (최근 10건)'
SELECT
    r.id,
    r.reservation_number,
    r.status::text,
    c.name AS customer_name,
    r.total_price,
    r.reservation_date,
    r.created_at
FROM reservations r
LEFT JOIN customers c ON r.customer_id = c.id
ORDER BY r.created_at DESC
LIMIT 10;

\echo ''
\echo '📌 2. 예약 상태별 집계'
SELECT
    status::text,
    COUNT(*) AS count
FROM reservations
GROUP BY status
ORDER BY count DESC;

\echo ''
\echo '📌 3. 고객 통계 (방문 횟수 있는 고객만)'
SELECT
    id,
    name,
    phone,
    visit_count,
    total_spent,
    last_visit_date,
    tags,
    created_at
FROM customers
WHERE visit_count > 0
ORDER BY visit_count DESC, total_spent DESC
LIMIT 10;

\echo ''
\echo '📌 4. 통계가 0인 고객 (문제 의심)'
SELECT
    id,
    name,
    phone,
    visit_count,
    total_spent,
    last_visit_date,
    created_at
FROM customers
WHERE (visit_count = 0 OR visit_count IS NULL)
    AND id IN (
        SELECT DISTINCT customer_id
        FROM reservations
        WHERE status = 'COMPLETED'
    )
LIMIT 10;

\echo ''
\echo '📌 5. 최근 완료된 예약 & 고객 통계 비교'
SELECT
    r.id AS reservation_id,
    r.reservation_number,
    r.status::text,
    r.total_price AS reservation_price,
    r.reservation_date,
    c.id AS customer_id,
    c.name AS customer_name,
    c.visit_count,
    c.total_spent,
    c.last_visit_date
FROM reservations r
LEFT JOIN customers c ON r.customer_id = c.id
WHERE r.status = 'COMPLETED'
ORDER BY r.updated_at DESC
LIMIT 5;

\echo ''
\echo '=========================================='
