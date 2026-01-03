-- ============================================
-- moer 예약 시스템 샘플 데이터
-- 개발/테스트용
-- ============================================

-- ============================================
-- 1. 사용자 (Users)
-- 비밀번호: password123 (BCrypt 암호화)
-- ============================================

INSERT INTO users (id, email, password, name, phone, role, status, staff_id, business_id, email_verified) VALUES
(1, 'admin@moer.io', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '시스템관리자', '010-0000-0000', 'ADMIN', 'ACTIVE', NULL, NULL, 'Y'),
(2, 'owner1@moer.io', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '김사장', '010-1111-1111', 'OWNER', 'ACTIVE', NULL, 1, 'Y'),
(3, 'owner2@moer.io', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '박사장', '010-1111-2222', 'OWNER', 'ACTIVE', NULL, 2, 'Y'),
(4, 'staff1@moer.io', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '이디자이너', '010-2222-1111', 'STAFF', 'ACTIVE', 1, 1, 'Y'),
(5, 'staff2@moer.io', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '최디자이너', '010-2222-2222', 'STAFF', 'ACTIVE', 2, 1, 'Y'),
(6, 'staff3@moer.io', '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG', '강강사', '010-2222-3333', 'STAFF', 'ACTIVE', 3, 2, 'Y');

-- ID 시퀀스 재설정
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));


-- ============================================
-- 2. 매장 (Businesses)
-- ============================================

INSERT INTO businesses (id, owner_id, name, business_type, phone, address, description, business_hours, settings, status) VALUES
(1, 2, '강남 헤어살롱', 'SALON', '02-1111-1111', '서울특별시 강남구 테헤란로 123', '강남역 5분거리 프리미엄 헤어살롱입니다.',
'{
"mon": {"open": "10:00", "close": "20:00"},
"tue": {"open": "10:00", "close": "20:00"},
"wed": {"open": "10:00", "close": "20:00"},
"thu": {"open": "10:00", "close": "20:00"},
"fri": {"open": "10:00", "close": "21:00"},
"sat": {"open": "10:00", "close": "21:00"},
"sun": {"open": "11:00", "close": "19:00"}
}'::jsonb,
'ACTIVE'),

(2, 3, '홍대 필라테스', 'PILATES', '02-2222-2222', '서울특별시 마포구 홍익로 456', '1:1 맞춤 필라테스 전문센터입니다.',
'{
"mon": {"open": "07:00", "close": "22:00"},
"tue": {"open": "07:00", "close": "22:00"},
"wed": {"open": "07:00", "close": "22:00"},
"thu": {"open": "07:00", "close": "22:00"},
"fri": {"open": "07:00", "close": "22:00"},
"sat": {"open": "09:00", "close": "18:00"},
"sun": null
}'::jsonb,
'{
"bookingInterval": 60,
"autoConfirm": true,
"allowOnlineBooking": true,
"maxAdvanceBookingDays": 14,
"minAdvanceBookingHours": 12
}'::jsonb,
'ACTIVE');

SELECT setval('businesses_id_seq', (SELECT MAX(id) FROM businesses));

-- business_settings 샘플 데이터
INSERT INTO business_settings (business_id, booking_interval, auto_confirm, allow_online_booking,
                               max_advance_booking_days, min_advance_booking_hours,
                               send_confirmation_sms, send_reminder_sms, reminder_hours_before,
                               payment_methods, allow_cancellation, cancel_deadline_hours) VALUES
(1, 30, 'N', 'Y', 30, 2, 'Y', 'Y', 24, 'CARD,CASH,TRANSFER', 'Y', 24),
(2, 60, 'Y', 'Y', 14, 12, 'Y', 'Y', 24, 'CARD,TRANSFER', 'Y', 12);

-- ============================================
-- 3. 직원 (Staffs)
-- ============================================

INSERT INTO staffs (id, business_id, name, position, phone, email, specialty, career_years, introduction, is_active) VALUES
(1, 1, '이디자이너', '실장', '010-2222-1111', 'designer1@salon.com', '펌, 컬러', 8, '8년차 펌/컬러 전문 디자이너입니다. 고객님의 스타일을 찾아드립니다.', 'Y'),
(2, 1, '최디자이너', '디자이너', '010-2222-2222', 'designer2@salon.com', '남성컷, 클리닉', 5, '5년차 남성 헤어 전문가입니다.', 'Y'),
(3, 2, '강강사', '원장', '010-2222-3333', 'instructor1@pilates.com', '재활 필라테스', 10, '10년차 재활 필라테스 전문 강사입니다.', 'Y'),
(4, 1, '정디자이너', '디자이너', '010-2222-4444', 'designer3@salon.com', '여성컷, 파마', 3, '트렌디한 여성 컷 전문 디자이너입니다.', 'N');

SELECT setval('staffs_id_seq', (SELECT MAX(id) FROM staffs));


-- ============================================
-- 4. 포트폴리오 (Portfolios)
-- ============================================

INSERT INTO portfolios (id, staff_id, title, description, image_url, tags, is_visible) VALUES
(1, 1, '볼륨 펌 스타일', 'C컬 볼륨펌으로 자연스러운 웨이브 연출', 'https://via.placeholder.com/400x400/FF6B6B/FFFFFF?text=Perm1', '펌,볼륨펌,C컬', 'Y'),
(2, 1, '애쉬 브라운 컬러', '트렌디한 애쉬 브라운 염색', 'https://via.placeholder.com/400x400/4ECDC4/FFFFFF?text=Color1', '염색,애쉬브라운,컬러', 'Y'),
(3, 2, '남성 투블럭 컷', '깔끔한 남성 투블럭 스타일', 'https://via.placeholder.com/400x400/95E1D3/FFFFFF?text=MenCut1', '남성컷,투블럭', 'Y'),
(4, 1, '옴브레 컬러', '그라데이션 옴브레 염색', 'https://via.placeholder.com/400x400/F38181/FFFFFF?text=Ombre1', '염색,옴브레,그라데이션', 'Y'),
(5, 2, '클리닉 케어', '두피 딥 클렌징 케어', 'https://via.placeholder.com/400x400/AA96DA/FFFFFF?text=Clinic1', '클리닉,두피케어', 'N');

SELECT setval('portfolios_id_seq', (SELECT MAX(id) FROM portfolios));


-- ============================================
-- 5. 서비스 (Services)
-- ============================================

-- 강남 헤어살롱 서비스
INSERT INTO services (id, business_id, category, name, description, duration, price, staff_ids, is_active) VALUES
(1, 1, '컷', '여성 컷', '기본 여성 헤어컷', 60, 35000, '1,2,4', 'Y'),
(2, 1, '컷', '남성 컷', '기본 남성 헤어컷', 40, 25000, '2', 'Y'),
(3, 1, '펌', '볼륨 펌', 'C컬 볼륨펌', 120, 80000, '1,4', 'Y'),
(4, 1, '펌', '매직 펌', '매직 스트레이트 펌', 150, 100000, '1', 'Y'),
(5, 1, '염색', '전체 염색', '전체 염색 (숏)', 90, 70000, '1,4', 'Y'),
(6, 1, '염색', '부분 염색', '뿌리 염색', 60, 50000, '1,4', 'Y'),
(7, 1, '클리닉', '두피 클리닉', '두피 딥 클렌징', 45, 40000, '2', 'Y'),
(8, 1, '클리닉', '헤어 클리닉', '헤어 트리트먼트', 30, 30000, '1,2,4', 'Y'),

-- 홍대 필라테스 서비스
(9, 2, '개인 레슨', '1:1 개인 레슨 (50분)', '맞춤형 1:1 필라테스 레슨', 50, 80000, '3', 'Y'),
(10, 2, '개인 레슨', '1:1 개인 레슨 (80분)', '집중 1:1 필라테스 레슨', 80, 120000, '3', 'Y'),
(11, 2, '그룹 레슨', '그룹 레슨 (2인)', '2인 그룹 레슨', 50, 50000, '3', 'Y'),
(12, 2, '재활', '재활 필라테스', '재활 운동 프로그램', 60, 100000, '3', 'Y');

SELECT setval('services_id_seq', (SELECT MAX(id) FROM services));


-- ============================================
-- 6. 특별 휴무일 (Special Holidays)
-- ============================================

INSERT INTO special_holidays (id, business_id, holiday_date, reason) VALUES
(1, 1, '2026-01-01', '신정'),
(2, 1, '2026-02-10', '설날 연휴'),
(3, 1, '2026-02-11', '설날'),
(4, 1, '2026-02-12', '설날 연휴'),
(5, 1, '2026-03-01', '삼일절'),
(6, 2, '2026-01-01', '신정'),
(7, 2, '2026-02-11', '설날'),
(8, 2, '2026-12-25', '크리스마스');

SELECT setval('special_holidays_id_seq', (SELECT MAX(id) FROM special_holidays));


-- ============================================
-- 7. 고객 (Customers)
-- ============================================

INSERT INTO customers (id, business_id, name, phone, email, birth_date, gender, visit_count, total_spent, last_visit_date, tags, memo) VALUES
(1, 1, '홍길동', '010-3333-1111', 'hong@example.com', '1990-05-15', 'MALE', 12, 850000, '2025-12-20', 'VIP,단골', '컬러 알레르기 주의'),
(2, 1, '김영희', '010-3333-2222', 'kim@example.com', '1995-08-22', 'FEMALE', 8, 620000, '2025-12-28', '단골', ''),
(3, 1, '박철수', '010-3333-3333', 'park@example.com', '1988-03-10', 'MALE', 3, 180000, '2025-11-15', '신규', ''),
(4, 1, '이미영', '010-3333-4444', 'lee@example.com', '1992-11-30', 'FEMALE', 15, 1200000, '2026-01-02', 'VIP,단골', '파마만 선호'),
(5, 1, '최민수', '010-3333-5555', 'choi@example.com', '1985-07-18', 'MALE', 1, 25000, '2025-12-01', '신규', ''),
(6, 2, '강지은', '010-4444-1111', 'kang@example.com', '1993-04-25', 'FEMALE', 20, 1600000, '2026-01-03', 'VIP,단골', '허리 디스크 주의'),
(7, 2, '윤서준', '010-4444-2222', 'yoon@example.com', '1991-09-12', 'MALE', 5, 400000, '2025-12-30', '신규', '');

SELECT setval('customers_id_seq', (SELECT MAX(id) FROM customers));


-- ============================================
-- 8. 예약 (Reservations)
-- ============================================

INSERT INTO reservations (id, business_id, customer_id, staff_id, reservation_date, start_time, end_time, services, total_duration, total_price, status, reservation_number, customer_memo, staff_memo) VALUES
-- 완료된 예약들
(1, 1, 1, 1, '2025-12-20', '14:00', '16:30',
'[{"id": 3, "name": "볼륨 펌", "price": 80000, "duration": 120}, {"id": 1, "name": "여성 컷", "price": 35000, "duration": 60}]'::jsonb,
150, 115000, 'COMPLETED', 'RES-20251220-0001', '볼륨감 많이 살려주세요', '고객님 만족하심', '2025-12-20 16:30:00'),

(2, 1, 2, 1, '2025-12-28', '10:00', '11:30',
'[{"id": 5, "name": "전체 염색", "price": 70000, "duration": 90}]'::jsonb,
90, 70000, 'COMPLETED', 'RES-20251228-0001', '', '애쉬 브라운으로 시술', '2025-12-28 11:30:00'),

(3, 1, 4, 1, '2026-01-02', '15:00', '16:00',
'[{"id": 6, "name": "부분 염색", "price": 50000, "duration": 60}]'::jsonb,
60, 50000, 'COMPLETED', 'RES-20260102-0001', '뿌리만 해주세요', '', '2026-01-02 16:00:00'),

-- 확정된 예약들 (미래)
(4, 1, 1, 1, '2026-01-10', '14:00', '15:00',
'[{"id": 1, "name": "여성 컷", "price": 35000, "duration": 60}]'::jsonb,
60, 35000, 'CONFIRMED', 'RES-20260110-0001', '', ''),

(5, 1, 2, 2, '2026-01-10', '11:00', '12:30',
'[{"id": 1, "name": "여성 컷", "price": 35000, "duration": 60}, {"id": 8, "name": "헤어 클리닉", "price": 30000, "duration": 30}]'::jsonb,
90, 65000, 'CONFIRMED', 'RES-20260110-0002', '컷은 레이어드로', ''),

(6, 1, 3, 2, '2026-01-11', '16:00', '16:40',
'[{"id": 2, "name": "남성 컷", "price": 25000, "duration": 40}]'::jsonb,
40, 25000, 'CONFIRMED', 'RES-20260111-0001', '', ''),

-- 대기 중인 예약
(7, 1, 4, 1, '2026-01-12', '10:00', '12:00',
'[{"id": 3, "name": "볼륨 펌", "price": 80000, "duration": 120}]'::jsonb,
120, 80000, 'PENDING', 'RES-20260112-0001', 'C컬로 해주세요', ''),

-- 필라테스 예약
(8, 2, 6, 3, '2026-01-03', '10:00', '10:50',
'[{"id": 9, "name": "1:1 개인 레슨 (50분)", "price": 80000, "duration": 50}]'::jsonb,
50, 80000, 'COMPLETED', 'RES-20260103-0001', '', '척추 중립 자세 교정', '2026-01-03 10:50:00'),

(9, 2, 6, 3, '2026-01-10', '10:00', '10:50',
'[{"id": 9, "name": "1:1 개인 레슨 (50분)", "price": 80000, "duration": 50}]'::jsonb,
50, 80000, 'CONFIRMED', 'RES-20260110-0003', '', ''),

(10, 2, 7, 3, '2026-01-10', '14:00', '14:50',
'[{"id": 9, "name": "1:1 개인 레슨 (50분)", "price": 80000, "duration": 50}]'::jsonb,
50, 80000, 'CONFIRMED', 'RES-20260110-0004', '처음이라 걱정됩니다', '');

SELECT setval('reservations_id_seq', (SELECT MAX(id) FROM reservations));


-- ============================================
-- 9. 고객 시술 이력 (Customer Histories)
-- ============================================

INSERT INTO customer_histories (id, business_id, customer_id, staff_id, reservation_id, visit_date, services, total_price, payment_method, details) VALUES
(1, 1, 1, 1, 1, '2025-12-20',
 '[{"id": 3, "name": "볼륨 펌", "price": 80000}, {"id": 1, "name": "여성 컷", "price": 35000}]'::jsonb,
 115000, 'CARD',
 '{"products": ["로레알 펌제", "케라스타즈 트리트먼트"], "notes": "C컬 볼륨펌, 고객 만족도 높음"}'::jsonb),

(2, 1, 2, 1, 2, '2025-12-28',
 '[{"id": 5, "name": "전체 염색", "price": 70000}]'::jsonb,
 70000, 'CARD',
 '{"products": ["웰라 염색약 7A"], "notes": "애쉬 브라운, 20분 방치"}'::jsonb),

(3, 1, 4, 1, 3, '2026-01-02',
 '[{"id": 6, "name": "부분 염색", "price": 50000}]'::jsonb,
 50000, 'CASH',
 '{"products": ["웰라 염색약 6N"], "notes": "뿌리 2cm 염색"}'::jsonb),

(4, 1, 1, 1, NULL, '2025-11-15',
 '[{"id": 5, "name": "전체 염색", "price": 70000}]'::jsonb,
 70000, 'CARD',
 '{"products": ["웰라 염색약 8A"], "notes": "전체 염색, 애쉬 계열"}'::jsonb),

(5, 1, 2, 2, NULL, '2025-10-20',
 '[{"id": 1, "name": "여성 컷", "price": 35000}]'::jsonb,
 35000, 'CARD',
 '{"notes": "레이어드 컷"}'::jsonb),

(6, 2, 6, 3, 8, '2026-01-03',
 '[{"id": 9, "name": "1:1 개인 레슨 (50분)", "price": 80000}]'::jsonb,
 80000, 'CARD',
 '{"focus": "척추 중립", "notes": "허리 통증 개선 중"}'::jsonb),

(7, 2, 6, 3, NULL, '2025-12-27',
 '[{"id": 9, "name": "1:1 개인 레슨 (50분)", "price": 80000}]'::jsonb,
 80000, 'CARD',
 '{"focus": "골반 안정화", "notes": "자세 많이 개선됨"}'::jsonb);

SELECT setval('customer_histories_id_seq', (SELECT MAX(id) FROM customer_histories));


-- ============================================
-- 완료 메시지
-- ============================================

DO $$
BEGIN
RAISE NOTICE '====================================';
RAISE NOTICE '샘플 데이터 생성 완료!';
RAISE NOTICE '====================================';
RAISE NOTICE '사용자: 6명 (admin 1, owner 2, staff 3)';
RAISE NOTICE '매장: 2개 (헤어살롱 1, 필라테스 1)';
RAISE NOTICE '직원: 4명';
RAISE NOTICE '포트폴리오: 5개';
RAISE NOTICE '서비스: 12개';
RAISE NOTICE '고객: 7명';
RAISE NOTICE '예약: 10건';
RAISE NOTICE '이력: 7건';
RAISE NOTICE '====================================';
RAISE NOTICE '테스트 계정:';
RAISE NOTICE 'admin@moer.io / password123';
RAISE NOTICE 'owner1@moer.io / password123';
RAISE NOTICE 'staff1@moer.io / password123';
RAISE NOTICE '====================================';
END $$;