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