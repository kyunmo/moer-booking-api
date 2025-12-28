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