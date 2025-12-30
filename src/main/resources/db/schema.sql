-- users 테이블
CREATE TABLE users (
id BIGSERIAL PRIMARY KEY,
email VARCHAR(100) UNIQUE NOT NULL,
password VARCHAR(255) NOT NULL,
name VARCHAR(50) NOT NULL,
phone VARCHAR(20),
role VARCHAR(20) DEFAULT 'OWNER' NOT NULL,
status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
email_verified BOOLEAN DEFAULT FALSE,
last_login_at TIMESTAMP,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);

-- 테스트 데이터
INSERT INTO users (email, password, name, phone, role) VALUES
('admin@moer.io', 'password123', '관리자', '010-1234-5678', 'ADMIN'),
('owner1@moer.io', 'password123', '김미용', '010-2345-6789', 'OWNER'),
('owner2@moer.io', 'password123', '박헤어', '010-3456-7890', 'OWNER');


-- businesses 테이블
CREATE TABLE businesses (
id BIGSERIAL PRIMARY KEY,
owner_id BIGINT NOT NULL,
business_type VARCHAR(50) NOT NULL,
name VARCHAR(100) NOT NULL,
description TEXT,
phone VARCHAR(20),
email VARCHAR(100),

-- 주소
address VARCHAR(255),
address_detail VARCHAR(100),
zip_code VARCHAR(10),
latitude DECIMAL(10, 8),
longitude DECIMAL(11, 8),

-- 영업 정보 (JSONB)
opening_hours JSONB,
regular_holidays JSONB,

-- 이미지
logo_url VARCHAR(255),
cover_image_url VARCHAR(255),
images JSONB,

-- 소셜/링크
website VARCHAR(255),
instagram VARCHAR(100),
facebook VARCHAR(100),

-- 상태
status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,

created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 인덱스
CREATE INDEX idx_businesses_owner ON businesses(owner_id);
CREATE INDEX idx_businesses_type ON businesses(business_type);
CREATE INDEX idx_businesses_status ON businesses(status);
CREATE INDEX idx_businesses_location ON businesses(latitude, longitude);


-- businesses 테이블
CREATE TABLE businesses (
id BIGSERIAL PRIMARY KEY,
owner_id BIGINT NOT NULL,
business_type VARCHAR(50) NOT NULL,
name VARCHAR(100) NOT NULL,
description TEXT,
phone VARCHAR(20),
email VARCHAR(100),
address VARCHAR(255),
address_detail VARCHAR(100),
zip_code VARCHAR(10),
latitude DECIMAL(10, 8),
longitude DECIMAL(11, 8),
opening_hours JSONB,
regular_holidays JSONB,
logo_url VARCHAR(255),
cover_image_url VARCHAR(255),
images JSONB,
website VARCHAR(255),
instagram VARCHAR(100),
facebook VARCHAR(100),
status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 인덱스
CREATE INDEX idx_businesses_owner ON businesses(owner_id);
CREATE INDEX idx_businesses_type ON businesses(business_type);
CREATE INDEX idx_businesses_status ON businesses(status);
CREATE INDEX idx_businesses_location ON businesses(latitude, longitude);

-- business_settings 테이블
CREATE TABLE business_settings (
id BIGSERIAL PRIMARY KEY,
business_id BIGINT UNIQUE NOT NULL,
settings JSONB NOT NULL DEFAULT '{}',
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
);

CREATE INDEX idx_business_settings_business ON business_settings(business_id);
CREATE INDEX idx_business_settings_type ON business_settings USING GIN ((settings->'type'));

-- 테스트 데이터
INSERT INTO businesses (owner_id, business_type, name, description, phone, address, opening_hours, regular_holidays) VALUES
(2, 'BEAUTY_SHOP', '김미용 헤어샵', '강남역 10년 경력 미용실', '02-1234-5678', '서울시 강남구 테헤란로',
'{"mon": {"open": "09:00", "close": "20:00", "is_closed": false}, "tue": {"open": "09:00", "close": "20:00", "is_closed": false}, "wed": {"open": "09:00", "close": "20:00", "is_closed": false}, "thu": {"open": "09:00", "close": "20:00", "is_closed": false}, "fri": {"open": "09:00", "close": "20:00", "is_closed": false}, "sat": {"open": "10:00", "close": "19:00", "is_closed": false}, "sun": {"is_closed": true}}'::jsonb,
'["mon"]'::jsonb),
(2, 'BEAUTY_SHOP', '김미용 2호점', '역삼점', '02-2345-6789', '서울시 강남구 역삼동',
'{}'::jsonb, '["mon"]'::jsonb),
(3, 'BEAUTY_SHOP', '박헤어 살롱', '신논현역 근처', '02-3456-7890', '서울시 강남구 논현동',
'{}'::jsonb, '[]'::jsonb);

-- business_settings 테스트 데이터
INSERT INTO business_settings (business_id, settings) VALUES
(1, '{"type": "beauty_shop", "portfolio_enabled": true, "service_categories": ["컷", "펌", "염색"], "booking_interval": 30}'::jsonb),
(2, '{"type": "beauty_shop", "portfolio_enabled": true, "service_categories": ["컷", "펌"], "booking_interval": 30}'::jsonb),
(3, '{"type": "beauty_shop", "portfolio_enabled": false, "service_categories": ["컷", "펌", "염색", "클리닉"], "booking_interval": 60}'::jsonb);



-- staffs 테이블
CREATE TABLE staffs (
id BIGSERIAL PRIMARY KEY,
business_id BIGINT NOT NULL,
user_id BIGINT,  -- nullable (계정 연동 선택)
name VARCHAR(50) NOT NULL,
nickname VARCHAR(50),
phone VARCHAR(20),
email VARCHAR(100),

-- 프로필
profile_image_url VARCHAR(255),
introduction TEXT,
career_years INT,
specialties JSONB,  -- ["컷", "펌"] or ["필라테스 기초", "재활"]

-- 근무 정보
work_schedule JSONB,  -- {"mon": {"start": "09:00", "end": "18:00", "is_off": false}, ...}
is_active BOOLEAN DEFAULT TRUE,
display_order INT DEFAULT 0,

created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- 인덱스
CREATE INDEX idx_staffs_business ON staffs(business_id);
CREATE INDEX idx_staffs_user ON staffs(user_id);
CREATE INDEX idx_staffs_active ON staffs(business_id, is_active);

-- portfolios 테이블 (미용실 특화)
CREATE TABLE portfolios (
id BIGSERIAL PRIMARY KEY,
staff_id BIGINT NOT NULL,
business_id BIGINT NOT NULL,

title VARCHAR(100),
description TEXT,
image_url VARCHAR(255) NOT NULL,
tags JSONB,  -- ["단발컷", "볼륨펌", "자연스러운"]

display_order INT DEFAULT 0,
is_visible BOOLEAN DEFAULT TRUE,

created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

FOREIGN KEY (staff_id) REFERENCES staffs(id) ON DELETE CASCADE,
FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
);

CREATE INDEX idx_portfolios_staff ON portfolios(staff_id);
CREATE INDEX idx_portfolios_business ON portfolios(business_id);
CREATE INDEX idx_portfolios_tags ON portfolios USING GIN (tags);

-- 테스트 데이터
INSERT INTO staffs (business_id, name, nickname, phone, introduction, career_years, specialties, work_schedule, is_active) VALUES
(1, '김미용', '미용쌤', '010-1111-1111', '10년 경력 원장', 10,
'["컷", "펌", "염색"]'::jsonb,
'{"mon": {"start": "09:00", "end": "20:00", "is_off": false}, "tue": {"start": "09:00", "end": "20:00", "is_off": false}, "wed": {"start": "09:00", "end": "20:00", "is_off": false}, "thu": {"start": "09:00", "end": "20:00", "is_off": false}, "fri": {"start": "09:00", "end": "20:00", "is_off": false}, "sat": {"start": "10:00", "end": "19:00", "is_off": false}, "sun": {"is_off": true}}'::jsonb,
true),
(1, '박디자이너', '박쌤', '010-2222-2222', '컷트 전문', 5,
'["컷"]'::jsonb,
'{"mon": {"start": "10:00", "end": "19:00", "is_off": false}, "tue": {"start": "10:00", "end": "19:00", "is_off": false}, "wed": {"start": "10:00", "end": "19:00", "is_off": false}, "thu": {"start": "10:00", "end": "19:00", "is_off": false}, "fri": {"start": "10:00", "end": "19:00", "is_off": false}, "sat": {"is_off": true}, "sun": {"is_off": true}}'::jsonb,
true),
(2, '이디자이너', '이쌤', '010-3333-3333', '염색 전문가', 7,
'["염색", "클리닉"]'::jsonb,
'{}'::jsonb,
true);

-- 포트폴리오 테스트 데이터
INSERT INTO portfolios (staff_id, business_id, title, description, image_url, tags, display_order) VALUES
(1, 1, '단발 레이어드컷', '고객님 취향 저격 단발컷', 'https://example.com/portfolio1.jpg', '["단발", "레이어드", "여성컷"]'::jsonb, 1),
(1, 1, '볼륨 펌', '자연스러운 볼륨감', 'https://example.com/portfolio2.jpg', '["볼륨펌", "웨이브"]'::jsonb, 2),
(2, 1, '남성 페이드컷', '깔끔한 페이드 스타일', 'https://example.com/portfolio3.jpg', '["남성컷", "페이드"]'::jsonb, 1);


-- services 테이블
CREATE TABLE services (
id BIGSERIAL PRIMARY KEY,
business_id BIGINT NOT NULL,

category VARCHAR(50),  -- "컷", "펌", "염색", "1:1수업", "그룹수업", "좌석"
name VARCHAR(100) NOT NULL,
description TEXT,

-- 가격/시간
price INT DEFAULT 0,
duration INT NOT NULL,  -- 소요 시간 (분)

-- 이미지
image_url VARCHAR(255),

-- 옵션 (업종별로 다름)
options JSONB,  -- 업종별 추가 옵션

-- 가능한 직원 (NULL이면 모든 직원 가능)
available_staff_ids JSONB,  -- [1, 2, 3] or null

-- 표시
is_active BOOLEAN DEFAULT TRUE,
display_order INT DEFAULT 0,

created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
);

-- 인덱스
CREATE INDEX idx_services_business ON services(business_id);
CREATE INDEX idx_services_category ON services(business_id, category);
CREATE INDEX idx_services_active ON services(business_id, is_active);

-- 테스트 데이터 (미용실)
INSERT INTO services (business_id, category, name, description, price, duration, options, available_staff_ids, is_active) VALUES
(1, '컷', '여성컷', '여성 기본 커트', 30000, 30,
'{"can_combine": true, "requires_consultation": false, "material_cost_included": true}'::jsonb,
'[1, 2]'::jsonb,
true),
(1, '컷', '남성컷', '남성 기본 커트', 20000, 20,
'{"can_combine": true, "requires_consultation": false, "material_cost_included": true}'::jsonb,
'[1, 2]'::jsonb,
true),
(1, '펌', '볼륨펌', '자연스러운 볼륨감', 80000, 120,
'{"can_combine": true, "requires_consultation": true, "material_cost_included": true}'::jsonb,
'[1]'::jsonb,
true),
(1, '펌', '디지털펌', '웨이브 디지털펌', 100000, 150,
'{"can_combine": true, "requires_consultation": true, "material_cost_included": true}'::jsonb,
'[1]'::jsonb,
true),
(1, '염색', '전체염색', '전체 염색', 70000, 90,
'{"can_combine": true, "requires_consultation": true, "material_cost_included": false}'::jsonb,
'[1, 3]'::jsonb,
true),
(1, '클리닉', '두피 클리닉', '두피 케어 프로그램', 50000, 60,
'{"can_combine": false, "requires_consultation": false, "material_cost_included": true}'::jsonb,
'[1]'::jsonb,
true);

-- 테스트 데이터 (필라테스 - business_id=2라고 가정)
-- INSERT INTO services (business_id, category, name, description, price, duration, options, is_active) VALUES
-- (2, '1:1수업', '개인 필라테스', '1:1 개인 수업', 80000, 50,
--  '{"class_type": "private", "max_capacity": 1, "equipment_required": ["리포머"], "level": "all"}'::jsonb,
--  true),
-- (2, '그룹수업', '그룹 필라테스', '소그룹 수업 (최대 4명)', 40000, 50,
--  '{"class_type": "group", "max_capacity": 4, "equipment_required": ["리포머"], "level": "beginner"}'::jsonb,
--  true),
-- (2, '그룹수업', '매트 필라테스', '매트 운동 (최대 10명)', 30000, 50,
--  '{"class_type": "large", "max_capacity": 10, "equipment_required": ["매트"], "level": "all"}'::jsonb,
--  true);



-- special_holidays 테이블
CREATE TABLE special_holidays (
id BIGSERIAL PRIMARY KEY,
business_id BIGINT NOT NULL,

holiday_date DATE NOT NULL,
title VARCHAR(100),
is_closed BOOLEAN DEFAULT TRUE,

created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
UNIQUE(business_id, holiday_date)
);

-- 인덱스
CREATE INDEX idx_special_holidays_business ON special_holidays(business_id);
CREATE INDEX idx_special_holidays_date ON special_holidays(business_id, holiday_date);

-- 테스트 데이터
INSERT INTO special_holidays (business_id, holiday_date, title, is_closed) VALUES
(1, '2026-01-01', '신정', true),
(1, '2026-02-16', '설날 연휴', true),
(1, '2026-02-17', '설날', true),
(1, '2026-02-18', '설날 연휴', true),
(1, '2026-03-01', '삼일절', true),
(1, '2026-05-05', '어린이날', true),
(1, '2026-06-06', '현충일', true),
(1, '2026-08-15', '광복절', true),
(1, '2026-09-28', '추석 연휴', true),
(1, '2026-09-29', '추석', true),
(1, '2026-09-30', '추석 연휴', true),
(1, '2026-10-03', '개천절', true),
(1, '2026-10-09', '한글날', true),
(1, '2026-12-25', '크리스마스', true),
(1, '2026-07-15', '리모델링 휴업', true),
(1, '2026-07-16', '리모델링 휴업', true);


-- customers 테이블
CREATE TABLE customers (
id BIGSERIAL PRIMARY KEY,
business_id BIGINT NOT NULL,

-- 기본 정보
name VARCHAR(50) NOT NULL,
phone VARCHAR(20) NOT NULL,
email VARCHAR(100),

-- 통계
visit_count INT DEFAULT 0,
total_spent INT DEFAULT 0,
last_visit_date DATE,

-- 태그
tags JSONB,  -- ["VIP", "단골", "신규"]

-- 관리자 메모 (관리자만 볼 수 있음)
admin_memo TEXT,

-- 카카오톡
kakao_user_key VARCHAR(100),  -- 카카오 알림용

created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
);

-- 인덱스
CREATE INDEX idx_customers_business ON customers(business_id);
CREATE INDEX idx_customers_phone ON customers(business_id, phone);
CREATE INDEX idx_customers_visit ON customers(business_id, visit_count DESC);
CREATE INDEX idx_customers_tags ON customers USING GIN (tags);

-- 테스트 데이터
INSERT INTO customers (business_id, name, phone, email, visit_count, total_spent, last_visit_date, tags, admin_memo) VALUES
(1, '김고객', '010-1111-1111', 'kim@example.com', 15, 450000, '2026-01-05',
'["VIP", "단골"]'::jsonb,
'항상 밝은 갈색 염색 선호. 다음엔 더 밝게 해달라고 하심'),
(1, '이고객', '010-2222-2222', 'lee@example.com', 8, 240000, '2025-12-28',
'["단골"]'::jsonb,
'컷만 주로 하심. 짧게 자르는 스타일 선호'),
(1, '박고객', '010-3333-3333', null, 1, 30000, '2026-01-03',
'["신규"]'::jsonb,
''),
(1, '최고객', '010-4444-4444', 'choi@example.com', 3, 90000, '2025-12-20',
'[]'::jsonb,
'알레르기 있음 - 염색약 주의'),
(2, '강고객', '010-5555-5555', 'kang@example.com', 20, 800000, '2026-01-04',
'["VIP"]'::jsonb,
'1:1 수업 선호. 요가 매트 개인 소지');


-- reservations 테이블
CREATE TABLE reservations (
id BIGSERIAL PRIMARY KEY,
business_id BIGINT NOT NULL,
customer_id BIGINT NOT NULL,
staff_id BIGINT,  -- NULL 가능 (상관없음 선택 시)

-- 예약 정보
reservation_number VARCHAR(20) UNIQUE NOT NULL,  -- RES-20260106-0001
reservation_date DATE NOT NULL,
start_time TIME NOT NULL,
end_time TIME NOT NULL,

-- 서비스 (여러 개 선택 가능)
service_ids JSONB NOT NULL,  -- [1, 2, 3]
service_names JSONB NOT NULL,  -- ["여성컷", "볼륨펌"]
total_duration INT NOT NULL,  -- 총 소요 시간 (분)
total_price INT DEFAULT 0,

-- 상태
status VARCHAR(20) DEFAULT 'PENDING',
-- PENDING(대기), CONFIRMED(확정), COMPLETED(완료), CANCELLED(취소), NOSHOW(노쇼)

-- 메모
customer_request TEXT,  -- 고객 요청사항
admin_memo TEXT,  -- 관리자 메모

-- 알림
notification_sent JSONB,  -- {"confirmed": true, "reminder": true, "completed": false}

-- 취소 정보
cancelled_at TIMESTAMP,
cancel_reason TEXT,

created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
FOREIGN KEY (staff_id) REFERENCES staffs(id) ON DELETE SET NULL
);

-- 인덱스
CREATE INDEX idx_reservations_business ON reservations(business_id);
CREATE INDEX idx_reservations_customer ON reservations(customer_id);
CREATE INDEX idx_reservations_staff ON reservations(staff_id);
CREATE INDEX idx_reservations_date ON reservations(business_id, reservation_date);
CREATE INDEX idx_reservations_status ON reservations(business_id, status);
CREATE INDEX idx_reservations_number ON reservations(reservation_number);

-- 시간 겹침 방지용 복합 인덱스
CREATE INDEX idx_reservations_time_check
ON reservations(business_id, staff_id, reservation_date, start_time, end_time)
WHERE status IN ('PENDING', 'CONFIRMED');

-- 예약 번호 시퀀스
CREATE SEQUENCE reservation_number_seq START 1;

-- 테스트 데이터
INSERT INTO reservations (
business_id, customer_id, staff_id,
reservation_number, reservation_date, start_time, end_time,
service_ids, service_names, total_duration, total_price,
status, customer_request, notification_sent
) VALUES
-- 오늘 예약들
(1, 1, 1, 'RES-20260106-0001', '2026-01-06', '10:00', '11:00',
'[1, 3]'::jsonb, '["여성컷", "볼륨펌"]'::jsonb, 150, 110000,
'CONFIRMED', '펌 밝게 해주세요',
'{"confirmed": true, "reminder": false}'::jsonb),

(1, 2, 2, 'RES-20260106-0002', '2026-01-06', '14:00', '14:30',
'[2]'::jsonb, '["남성컷"]'::jsonb, 30, 20000,
'CONFIRMED', '',
'{"confirmed": true, "reminder": false}'::jsonb),

(1, 3, 1, 'RES-20260106-0003', '2026-01-06', '16:00', '16:30',
'[1]'::jsonb, '["여성컷"]'::jsonb, 30, 30000,
'PENDING', '짧게 잘라주세요',
'{}'::jsonb),

-- 내일 예약들
(1, 1, 1, 'RES-20260107-0001', '2026-01-07', '10:00', '12:30',
'[1, 3]'::jsonb, '["여성컷", "볼륨펌"]'::jsonb, 150, 110000,
'CONFIRMED', '',
'{"confirmed": true, "reminder": false}'::jsonb),

(1, 4, 2, 'RES-20260107-0002', '2026-01-07', '15:00', '16:00',
'[5]'::jsonb, '["전체염색"]'::jsonb, 90, 70000,
'CONFIRMED', '밝은 갈색으로',
'{"confirmed": true, "reminder": false}'::jsonb),

-- 과거 완료된 예약
(1, 1, 1, 'RES-20260105-0001', '2026-01-05', '10:00', '10:30',
'[1]'::jsonb, '["여성컷"]'::jsonb, 30, 30000,
'COMPLETED', '',
'{"confirmed": true, "reminder": true, "completed": true}'::jsonb),

-- 취소된 예약
(1, 2, 2, 'RES-20260108-0001', '2026-01-08', '14:00', '14:30',
'[2]'::jsonb, '["남성컷"]'::jsonb, 30, 20000,
'CANCELLED', '일정 변경으로 취소',
'{"confirmed": true, "reminder": false}'::jsonb);


-- customer_histories 테이블
CREATE TABLE customer_histories (
id BIGSERIAL PRIMARY KEY,
customer_id BIGINT NOT NULL,
business_id BIGINT NOT NULL,
reservation_id BIGINT,
staff_id BIGINT,

visit_date DATE NOT NULL,
services JSONB NOT NULL,  -- [{"id": 1, "name": "여성컷", "price": 30000}, ...]
total_price INT DEFAULT 0,

-- 상세 메모 (미용실 특화)
details JSONB,  -- {"color": "밝은 갈색", "length": "어깨선", "style": "레이어드"}
before_image_url VARCHAR(255),
after_image_url VARCHAR(255),

admin_memo TEXT,

created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE,
FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
FOREIGN KEY (reservation_id) REFERENCES reservations(id) ON DELETE SET NULL,
FOREIGN KEY (staff_id) REFERENCES staffs(id) ON DELETE SET NULL
);

-- 인덱스
CREATE INDEX idx_histories_customer ON customer_histories(customer_id);
CREATE INDEX idx_histories_business ON customer_histories(business_id);
CREATE INDEX idx_histories_date ON customer_histories(visit_date DESC);
CREATE INDEX idx_histories_reservation ON customer_histories(reservation_id);

-- 테스트 데이터
INSERT INTO customer_histories (
customer_id, business_id, reservation_id, staff_id,
visit_date, services, total_price, details, admin_memo
) VALUES
-- 김고객 이력 (VIP, 15회 방문)
(1, 1, 6, 1, '2025-12-05',
'[{"id": 1, "name": "여성컷", "price": 30000}]'::jsonb, 30000,
'{"length": "어깨선", "style": "레이어드"}'::jsonb,
'레이어드 컷으로 볼륨감 살림'),

(1, 1, NULL, 1, '2025-11-10',
'[{"id": 3, "name": "볼륨펌", "price": 80000}]'::jsonb, 80000,
'{"perm_type": "볼륨펌", "curl_strength": "중간"}'::jsonb,
'볼륨펌. 다음에 더 밝게 해달라고 하심'),

(1, 1, NULL, 1, '2025-10-15',
'[{"id": 5, "name": "전체염색", "price": 70000}]'::jsonb, 70000,
'{"color": "밝은 갈색", "brand": "웰라"}'::jsonb,
'밝은 갈색 염색. 만족도 높음'),

-- 이고객 이력 (단골, 8회 방문)
(2, 1, NULL, 2, '2025-12-20',
'[{"id": 2, "name": "남성컷", "price": 20000}]'::jsonb, 20000,
'{"length": "짧게", "style": "투블럭"}'::jsonb,
'투블럭 스타일 선호'),

(2, 1, NULL, 2, '2025-11-25',
'[{"id": 2, "name": "남성컷", "price": 20000}]'::jsonb, 20000,
'{"length": "짧게"}'::jsonb,
'짧게 선호'),

-- 박고객 이력 (신규, 1회)
(3, 1, NULL, 1, '2025-12-28',
'[{"id": 1, "name": "여성컷", "price": 30000}]'::jsonb, 30000,
'{"length": "짧게", "style": "단발"}'::jsonb,
'첫 방문. 짧게 잘라달라고 하심'),

-- 최고객 이력 (단골, 3회)
(4, 1, NULL, 1, '2025-12-15',
'[{"id": 1, "name": "여성컷", "price": 30000}, {"id": 4, "name": "트리트먼트", "price": 20000}]'::jsonb, 50000,
'{"length": "중간", "treatment": "케라틴"}'::jsonb,
'컷 + 트리트먼트'),

-- 강고객 이력 (2회)
(5, 1, NULL, 2, '2025-12-10',
'[{"id": 2, "name": "남성컷", "price": 20000}]'::jsonb, 20000,
NULL,
'일반 컷');


-- users 테이블에 staffId, businessId 추가
ALTER TABLE users
    ADD COLUMN staff_id BIGINT,
ADD COLUMN business_id BIGINT;

-- 외래키 추가
ALTER TABLE users
    ADD CONSTRAINT fk_users_staff
        FOREIGN KEY (staff_id) REFERENCES staffs(id) ON DELETE SET NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_business
        FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE SET NULL;

-- 인덱스 추가
CREATE INDEX idx_users_staff ON users(staff_id);
CREATE INDEX idx_users_business ON users(business_id);

COMMENT ON COLUMN users.staff_id IS 'STAFF 역할인 경우 연결된 Staff ID';
COMMENT ON COLUMN users.business_id IS 'OWNER/STAFF가 소속된 Business ID';


-- refresh_tokens 테이블 생성
CREATE TABLE refresh_tokens (
id BIGSERIAL PRIMARY KEY,
user_id BIGINT NOT NULL,
token VARCHAR(500) NOT NULL,
expires_at TIMESTAMP NOT NULL,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
REFERENCES users(id) ON DELETE CASCADE
);

-- 인덱스
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at);

COMMENT ON TABLE refresh_tokens IS 'Refresh Token 저장';
COMMENT ON COLUMN refresh_tokens.user_id IS '사용자 ID';
COMMENT ON COLUMN refresh_tokens.token IS 'Refresh Token';
COMMENT ON COLUMN refresh_tokens.expires_at IS '만료 시간';