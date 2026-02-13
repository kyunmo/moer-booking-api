-- =============================================================================
-- moer-booking 초기 데이터 + 샘플 데이터
-- 생성일: 2026-02-14
-- 주의: schema.sql 실행 후 사용
-- =============================================================================

-- =============================================================================
-- 1. 시스템 관리자
-- =============================================================================

-- 슈퍼 관리자 (비밀번호: password123)
-- $2a$10$zOjLVHiSvRhfzFVpbn4IB.4pr4LSZMKx5JdBjf5USqdwoGG6NcquC
-- $2a$10$zOjLVHiSvRhfzFVpbn4IB.4pr4LSZMKx5JdBjf5USqdwoGG6NcquC
INSERT INTO users (id, email, password, name, role, status, email_verified, created_at)
VALUES (
    1,
    'superadmin@moer.io',
    '$2a$10$zOjLVHiSvRhfzFVpbn4IB.4pr4LSZMKx5JdBjf5USqdwoGG6NcquC',
    '시스템 관리자',
    'SUPER_ADMIN',
    'ACTIVE',
    'Y',
    CURRENT_TIMESTAMP
);

-- =============================================================================
-- 2. 샘플 매장 사장님
-- =============================================================================

-- 사장님 (비밀번호: password123)
INSERT INTO users (id, email, password, name, phone, role, status, business_id, email_verified, is_premium, trial_started_at, trial_expires_at, created_at)
VALUES (
    2,
    'owner@salon.com',
    '$2a$10$zOjLVHiSvRhfzFVpbn4IB.4pr4LSZMKx5JdBjf5USqdwoGG6NcquC',
    '김미영',
    '010-1234-5678',
    'OWNER',
    'ACTIVE',
    1,
    'Y',
    'N',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    CURRENT_TIMESTAMP
);

-- 직원 계정 (비밀번호: Staff123!)
INSERT INTO users (id, email, password, name, phone, role, status, staff_id, business_id, email_verified, created_at)
VALUES (
    3,
    'staff1@salon.com',
    '$2a$10$zOjLVHiSvRhfzFVpbn4IB.4pr4LSZMKx5JdBjf5USqdwoGG6NcquC',
    '이수진',
    '010-2345-6789',
    'STAFF',
    'ACTIVE',
    1,
    1,
    'Y',
    CURRENT_TIMESTAMP
);

-- =============================================================================
-- 3. 샘플 매장
-- =============================================================================

INSERT INTO businesses (id, owner_id, name, business_type, phone, address, description, business_hours, status,
                        subscription_plan, subscription_status, trial_started_at, trial_ends_at, current_staff_count)
VALUES (
    1,
    2,
    '뷰티살롱 모어',
    'BEAUTY_SHOP',
    '02-1234-5678',
    '서울시 강남구 역삼동 123-45 뷰티빌딩 2층',
    '트렌디한 헤어 스타일을 제안하는 강남 프리미엄 뷰티살롱입니다.',
    '{
        "mon": {"open": "10:00", "close": "20:00"},
        "tue": {"open": "10:00", "close": "20:00"},
        "wed": {"open": "10:00", "close": "20:00"},
        "thu": {"open": "10:00", "close": "21:00"},
        "fri": {"open": "10:00", "close": "21:00"},
        "sat": {"open": "10:00", "close": "18:00"},
        "sun": null
    }'::jsonb,
    'ACTIVE',
    'FREE',
    'TRIAL',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP + INTERVAL '30 days',
    3
);

-- 매장 설정 (기본값)
INSERT INTO business_settings (id, business_id)
VALUES (1, 1);

-- =============================================================================
-- 4. 샘플 직원
-- =============================================================================

INSERT INTO staffs (id, business_id, name, position, phone, email, specialty, career_years, introduction, is_active) VALUES
(1, 1, '이수진', '원장', '010-2345-6789', 'staff1@salon.com', '펌, 염색, 클리닉', 15, '15년 경력의 헤어 전문가입니다. 고객님의 얼굴형과 라이프스타일에 맞는 최적의 스타일을 제안합니다.', 'Y'),
(2, 1, '박준혁', '실장', '010-3456-7890', 'staff2@salon.com', '남성컷, 펌', 8, '남성 헤어 전문 디자이너입니다. 깔끔하고 세련된 스타일을 추구합니다.', 'Y'),
(3, 1, '최하늘', '디자이너', '010-4567-8901', 'staff3@salon.com', '여성컷, 염색, 업스타일', 5, '트렌디한 컬러와 스타일링을 전문으로 합니다.', 'Y');

-- =============================================================================
-- 5. 서비스 카테고리
-- =============================================================================

INSERT INTO service_categories (id, business_id, name, description, sort_order) VALUES
(1, 1, '커트',   '헤어 커트 서비스',           1),
(2, 1, '펌',     '다양한 펌 서비스',           2),
(3, 1, '염색',   '컬러링 및 염색 서비스',       3),
(4, 1, '클리닉', '모발 케어 및 트리트먼트',     4);

-- =============================================================================
-- 6. 서비스
-- =============================================================================

INSERT INTO services (id, business_id, category_id, name, description, duration, price, staff_ids, sort_order, is_active) VALUES
-- 커트
(1,  1, 1, '남성 커트',         '샴푸 포함',                     30, 15000, '1,2',   1, 'Y'),
(2,  1, 1, '여성 커트',         '샴푸 + 드라이 포함',             40, 25000, '1,2,3', 2, 'Y'),
(3,  1, 1, '아동 커트',         '초등학생 이하',                  20, 10000, '2,3',   3, 'Y'),
-- 펌
(4,  1, 2, '셋팅펌',           '자연스러운 볼륨감',               90, 80000, '1,2',   1, 'Y'),
(5,  1, 2, '디지털펌',         '웨이브 + 볼륨',                  120, 120000, '1',    2, 'Y'),
(6,  1, 2, '매직스트레이트',    '축모교정',                       150, 150000, '1,3',  3, 'Y'),
-- 염색
(7,  1, 3, '전체 염색',        '뿌리~모발 끝 전체 컬러링',        90, 70000, '1,3',   1, 'Y'),
(8,  1, 3, '뿌리 염색',        '새치 / 뿌리 부분 염색',           60, 40000, '1,2,3', 2, 'Y'),
(9,  1, 3, '탈색 + 염색',      '블리치 후 컬러링',               150, 130000, '1,3',  3, 'Y'),
-- 클리닉
(10, 1, 4, '두피 클리닉',      '두피 스케일링 + 영양 공급',        60, 50000, '1',     1, 'Y'),
(11, 1, 4, '모발 트리트먼트',   '손상 모발 집중 케어',             40, 30000, '1,3',   2, 'Y');

-- =============================================================================
-- 7. 샘플 고객
-- =============================================================================

INSERT INTO customers (id, business_id, name, phone, email, birth_date, gender, visit_count, total_spent, last_visit_date, tags, memo) VALUES
(1, 1, '정다은', '010-5678-1234', 'daeun@gmail.com',   '1990-05-15', 'FEMALE', 12, 450000, '2026-02-10', 'VIP,단골',     '펌 시 두피 민감, 약한 약제 사용 필요'),
(2, 1, '홍길동', '010-6789-2345', NULL,                 '1985-11-20', 'MALE',   5,  120000, '2026-02-08', '단골',         '짧은 스타일 선호'),
(3, 1, '박서연', '010-7890-3456', 'sy.park@naver.com',  '1995-03-22', 'FEMALE', 3,  250000, '2026-01-25', NULL,           '발레아쥬 관심'),
(4, 1, '김도현', '010-8901-4567', NULL,                 '2000-08-10', 'MALE',   1,  15000,  '2026-02-12', '신규',         NULL),
(5, 1, '이하영', '010-9012-5678', 'hayoung@gmail.com',  '1988-12-01', 'FEMALE', 8,  380000, '2026-02-05', 'VIP',          '매달 뿌리 염색 정기 방문');

-- =============================================================================
-- 8. 샘플 예약
-- =============================================================================

INSERT INTO reservations (id, business_id, customer_id, staff_id, reservation_date, start_time, end_time,
                          services, total_duration, total_price, status, reservation_number, customer_memo) VALUES
-- 오늘 예약
(1, 1, 1, 1, CURRENT_DATE, '14:00', '15:30',
 '[{"id": 2, "name": "여성 커트", "price": 25000, "duration": 40}, {"id": 7, "name": "전체 염색", "price": 70000, "duration": 90}]'::jsonb,
 130, 95000, 'CONFIRMED', 'RSV-20260214-001', '밝은 톤으로 부탁드려요'),

(2, 1, 2, 2, CURRENT_DATE, '16:00', '16:30',
 '[{"id": 1, "name": "남성 커트", "price": 15000, "duration": 30}]'::jsonb,
 30, 15000, 'PENDING', 'RSV-20260214-002', NULL),

-- 내일 예약
(3, 1, 5, 1, CURRENT_DATE + 1, '10:00', '11:00',
 '[{"id": 8, "name": "뿌리 염색", "price": 40000, "duration": 60}]'::jsonb,
 60, 40000, 'CONFIRMED', 'RSV-20260215-001', '지난번과 같은 컬러로'),

-- 완료된 예약
(4, 1, 3, 3, CURRENT_DATE - 3, '13:00', '15:30',
 '[{"id": 9, "name": "탈색 + 염색", "price": 130000, "duration": 150}]'::jsonb,
 150, 130000, 'COMPLETED', 'RSV-20260211-001', '발레아쥬 시술'),

-- 취소된 예약
(5, 1, 4, 2, CURRENT_DATE - 1, '11:00', '11:30',
 '[{"id": 1, "name": "남성 커트", "price": 15000, "duration": 30}]'::jsonb,
 30, 15000, 'CANCELLED', 'RSV-20260213-001', NULL);

-- 취소된 예약 상세 정보 업데이트
UPDATE reservations SET cancelled_at = CURRENT_TIMESTAMP - INTERVAL '1 day', cancel_reason = '개인 사정' WHERE id = 5;

-- =============================================================================
-- 9. 샘플 고객 시술 이력 (완료된 예약에 대한 이력)
-- =============================================================================

INSERT INTO customer_histories (id, business_id, customer_id, staff_id, reservation_id, visit_date, services, total_price, payment_method, details) VALUES
(1, 1, 3, 3, 4, CURRENT_DATE - 3,
 '[{"id": 9, "name": "탈색 + 염색", "price": 130000, "duration": 150}]'::jsonb,
 130000, 'CARD',
 '{"technique": "발레아쥬", "colorUsed": "애쉬베이지", "bleachLevel": 2, "notes": "다음 방문 시 리터치 권장"}'::jsonb);

-- =============================================================================
-- 10. 샘플 특별 휴무일
-- =============================================================================

INSERT INTO special_holidays (id, business_id, name, "date", type, reason) VALUES
(1, 1, '설날 연휴', '2026-02-17', 'NATIONAL', '설날 당일'),
(2, 1, '설날 연휴', '2026-02-18', 'NATIONAL', '설날 대체 공휴일'),
(3, 1, '매장 인테리어', '2026-03-01', 'TEMPORARY', '매장 리뉴얼 공사');

-- =============================================================================
-- 11. 샘플 포트폴리오
-- =============================================================================

INSERT INTO portfolios (id, staff_id, title, description, image_url, tags, is_visible) VALUES
(1, 1, '내추럴 웨이브 펌', '자연스러운 S컬 디지털펌', 'https://example.com/portfolio/1.jpg', '펌,웨이브,여성', 'Y'),
(2, 1, '애쉬 발레아쥬', '그라데이션 컬러링', 'https://example.com/portfolio/2.jpg', '염색,발레아쥬,트렌드', 'Y'),
(3, 2, '투블럭 남성컷', '깔끔한 비즈니스 스타일', 'https://example.com/portfolio/3.jpg', '남성,커트,비즈니스', 'Y');

-- =============================================================================
-- 12. 시퀀스 리셋 (명시적 ID 삽입 후 시퀀스 동기화)
-- =============================================================================

SELECT setval('users_id_seq', (SELECT COALESCE(MAX(id), 0) FROM users));
SELECT setval('businesses_id_seq', (SELECT COALESCE(MAX(id), 0) FROM businesses));
SELECT setval('business_settings_id_seq', (SELECT COALESCE(MAX(id), 0) FROM business_settings));
SELECT setval('staffs_id_seq', (SELECT COALESCE(MAX(id), 0) FROM staffs));
SELECT setval('portfolios_id_seq', (SELECT COALESCE(MAX(id), 0) FROM portfolios));
SELECT setval('service_categories_id_seq', (SELECT COALESCE(MAX(id), 0) FROM service_categories));
SELECT setval('services_id_seq', (SELECT COALESCE(MAX(id), 0) FROM services));
SELECT setval('customers_id_seq', (SELECT COALESCE(MAX(id), 0) FROM customers));
SELECT setval('reservations_id_seq', (SELECT COALESCE(MAX(id), 0) FROM reservations));
SELECT setval('customer_histories_id_seq', (SELECT COALESCE(MAX(id), 0) FROM customer_histories));
SELECT setval('special_holidays_id_seq', (SELECT COALESCE(MAX(id), 0) FROM special_holidays));
