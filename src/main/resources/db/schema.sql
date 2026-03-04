-- =============================================================================
-- moer-booking 최종 스키마 DDL
-- 생성일: 2026-02-14
-- PostgreSQL 16
-- 총 20개 테이블 (PG 커스텀 enum 타입 미사용, 모두 VARCHAR 처리)
-- =============================================================================

-- =============================================================================
-- 1. 사용자 / 인증
-- =============================================================================

-- 사용자 테이블
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    profile_image_url TEXT,
    role VARCHAR(20) NOT NULL DEFAULT 'OWNER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    staff_id BIGINT,
    business_id BIGINT,
    email_verified CHAR(1) DEFAULT 'N',
    trial_started_at TIMESTAMP,
    trial_expires_at TIMESTAMP,
    is_premium CHAR(1) DEFAULT 'N',
    marketing_agree CHAR(1) DEFAULT 'N',
    last_login_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE users IS '사용자 (시스템 관리자, 매장 사장님, 직원, 고객)';
COMMENT ON COLUMN users.id IS '사용자 ID';
COMMENT ON COLUMN users.email IS '이메일 (로그인 ID)';
COMMENT ON COLUMN users.password IS '비밀번호 (BCrypt 암호화)';
COMMENT ON COLUMN users.name IS '이름';
COMMENT ON COLUMN users.phone IS '전화번호';
COMMENT ON COLUMN users.profile_image_url IS '프로필 이미지 URL';
COMMENT ON COLUMN users.role IS '역할 (SUPER_ADMIN, ADMIN, OWNER, STAFF, CUSTOMER)';
COMMENT ON COLUMN users.status IS '상태 (ACTIVE, INACTIVE, SUSPENDED, DELETED)';
COMMENT ON COLUMN users.staff_id IS '연결된 직원 ID (STAFF 역할인 경우)';
COMMENT ON COLUMN users.business_id IS '소속 매장 ID (OWNER/STAFF)';
COMMENT ON COLUMN users.email_verified IS '이메일 인증 여부 (Y/N)';
COMMENT ON COLUMN users.trial_started_at IS '무료 체험 시작일';
COMMENT ON COLUMN users.trial_expires_at IS '무료 체험 만료일';
COMMENT ON COLUMN users.is_premium IS '프리미엄 여부 (Y/N)';
COMMENT ON COLUMN users.marketing_agree IS '마케팅 수신 동의 여부 (Y/N)';
COMMENT ON COLUMN users.last_login_at IS '마지막 로그인 시각';
COMMENT ON COLUMN users.created_at IS '생성일시';
COMMENT ON COLUMN users.updated_at IS '수정일시';

CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_business_id ON users(business_id);
CREATE INDEX idx_users_trial_expires ON users(trial_expires_at) WHERE (is_premium = 'N');

-- 리프레시 토큰 테이블
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE refresh_tokens IS 'JWT 리프레시 토큰';
COMMENT ON COLUMN refresh_tokens.id IS '토큰 ID';
COMMENT ON COLUMN refresh_tokens.user_id IS '사용자 ID';
COMMENT ON COLUMN refresh_tokens.token IS '리프레시 토큰 문자열';
COMMENT ON COLUMN refresh_tokens.expires_at IS '만료 시각';
COMMENT ON COLUMN refresh_tokens.created_at IS '생성일시';

CREATE UNIQUE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- 비밀번호 재설정 토큰 테이블
CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used CHAR(1) DEFAULT 'N',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

COMMENT ON TABLE password_reset_tokens IS '비밀번호 재설정 토큰';
COMMENT ON COLUMN password_reset_tokens.token IS 'UUID 형태의 재설정 토큰';
COMMENT ON COLUMN password_reset_tokens.expires_at IS '토큰 만료 시간 (30분)';
COMMENT ON COLUMN password_reset_tokens.used IS '사용 여부 (Y/N)';

CREATE UNIQUE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);
CREATE INDEX idx_password_reset_tokens_user_id ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_expires ON password_reset_tokens(expires_at) WHERE (used = 'N');

-- SNS 계정 연동 테이블
CREATE TABLE sns_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    name VARCHAR(100),
    profile_image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sns_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

COMMENT ON TABLE sns_accounts IS 'SNS 계정 연동 정보';
COMMENT ON COLUMN sns_accounts.provider IS 'SNS 제공자 (GOOGLE, NAVER, KAKAO)';
COMMENT ON COLUMN sns_accounts.provider_user_id IS 'SNS 제공자의 사용자 고유 ID';
COMMENT ON COLUMN sns_accounts.email IS 'SNS에서 제공한 이메일';
COMMENT ON COLUMN sns_accounts.name IS 'SNS에서 제공한 이름';
COMMENT ON COLUMN sns_accounts.profile_image_url IS 'SNS 프로필 이미지 URL';

CREATE UNIQUE INDEX idx_sns_accounts_provider_user ON sns_accounts(provider, provider_user_id);
CREATE INDEX idx_sns_accounts_user_id ON sns_accounts(user_id);
CREATE INDEX idx_sns_accounts_email ON sns_accounts(email);

-- =============================================================================
-- 2. 매장
-- =============================================================================

-- 매장 정보 테이블
CREATE TABLE businesses (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    slug VARCHAR(50) UNIQUE,
    business_type VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    address TEXT,
    address_detail VARCHAR(200),
    zip_code VARCHAR(10),
    description TEXT,
    profile_image_url TEXT,
    gallery_images JSONB,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    tags TEXT,
    average_rating DOUBLE PRECISION,
    review_count INTEGER DEFAULT 0,
    business_hours JSONB,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    daily_revenue_goal INTEGER,
    monthly_revenue_goal INTEGER,
    monthly_new_customer_goal INTEGER,
    subscription_plan VARCHAR(20) DEFAULT 'FREE',
    billing_cycle VARCHAR(10) DEFAULT 'MONTHLY',
    subscription_status VARCHAR(20) DEFAULT 'TRIAL',
    trial_started_at TIMESTAMP,
    trial_ends_at TIMESTAMP,
    subscription_started_at TIMESTAMP,
    next_billing_date TIMESTAMP,
    current_staff_count INTEGER NOT NULL DEFAULT 0,
    current_month_reservation_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE businesses IS '매장 정보';
COMMENT ON COLUMN businesses.id IS '매장 ID';
COMMENT ON COLUMN businesses.owner_id IS '사장님 사용자 ID';
COMMENT ON COLUMN businesses.name IS '매장명';
COMMENT ON COLUMN businesses.slug IS '매장 슬러그 (고객용 URL: /booking/{slug})';
COMMENT ON COLUMN businesses.business_type IS '업종 (BEAUTY_SHOP, PILATES, YOGA, CAFE, STUDY_CAFE, WORKSHOP, ACADEMY, PET_SALON, OTHER)';
COMMENT ON COLUMN businesses.phone IS '매장 전화번호';
COMMENT ON COLUMN businesses.address IS '매장 주소';
COMMENT ON COLUMN businesses.address_detail IS '상세주소 (예: 2층 201호)';
COMMENT ON COLUMN businesses.zip_code IS '우편번호 (예: 06234)';
COMMENT ON COLUMN businesses.description IS '매장 소개';
COMMENT ON COLUMN businesses.profile_image_url IS '매장 프로필 이미지 URL';
COMMENT ON COLUMN businesses.gallery_images IS '갤러리 이미지 URL 목록 (JSONB)';
COMMENT ON COLUMN businesses.latitude IS '위도 (위치 기반 검색용)';
COMMENT ON COLUMN businesses.longitude IS '경도 (위치 기반 검색용)';
COMMENT ON COLUMN businesses.tags IS '태그 (콤마 구분: 예약가능,주차가능,카드결제)';
COMMENT ON COLUMN businesses.average_rating IS '평균 평점 (리뷰 비정규화)';
COMMENT ON COLUMN businesses.review_count IS '리뷰 수 (비정규화)';
COMMENT ON COLUMN businesses.business_hours IS '영업시간 (JSONB: {"mon":{"open":"10:00","close":"20:00"}, ...})';
COMMENT ON COLUMN businesses.status IS '상태 (ACTIVE, INACTIVE, SUSPENDED)';
COMMENT ON COLUMN businesses.daily_revenue_goal IS '일일 매출 목표 (원)';
COMMENT ON COLUMN businesses.monthly_revenue_goal IS '월간 매출 목표 (원)';
COMMENT ON COLUMN businesses.monthly_new_customer_goal IS '월간 신규 고객 목표 (명)';
COMMENT ON COLUMN businesses.subscription_plan IS '구독 플랜 (FREE, BASIC)';
COMMENT ON COLUMN businesses.billing_cycle IS '결제 주기 (MONTHLY, YEARLY)';
COMMENT ON COLUMN businesses.subscription_status IS '구독 상태 (TRIAL, ACTIVE, EXPIRED, CANCELED, SUSPENDED)';
COMMENT ON COLUMN businesses.trial_started_at IS '무료 체험 시작일';
COMMENT ON COLUMN businesses.trial_ends_at IS '무료 체험 종료일';
COMMENT ON COLUMN businesses.subscription_started_at IS '유료 구독 시작일';
COMMENT ON COLUMN businesses.next_billing_date IS '다음 결제 예정일';
COMMENT ON COLUMN businesses.current_staff_count IS '현재 활성 직원 수 (플랜 제한 체크용)';
COMMENT ON COLUMN businesses.current_month_reservation_count IS '이번 달 예약 수 (플랜 제한 체크용)';
COMMENT ON COLUMN businesses.created_at IS '생성일시';
COMMENT ON COLUMN businesses.updated_at IS '수정일시';

CREATE INDEX idx_businesses_owner_id ON businesses(owner_id);
CREATE INDEX idx_businesses_slug ON businesses(slug);
CREATE INDEX idx_businesses_status ON businesses(status);
CREATE INDEX idx_businesses_subscription_status ON businesses(subscription_status);
CREATE INDEX idx_businesses_trial_ends_at ON businesses(trial_ends_at);

-- 매장 설정 테이블
CREATE TABLE business_settings (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    booking_interval INTEGER DEFAULT 30,
    auto_confirm CHAR(1) DEFAULT 'N',
    allow_online_booking CHAR(1) DEFAULT 'Y',
    max_advance_booking_days INTEGER DEFAULT 30,
    min_advance_booking_hours INTEGER DEFAULT 2,
    send_confirmation_sms CHAR(1) DEFAULT 'Y',
    send_reminder_sms CHAR(1) DEFAULT 'Y',
    reminder_hours_before INTEGER DEFAULT 24,
    send_cancel_sms CHAR(1) DEFAULT 'Y',
    kakao_channel_id VARCHAR(100),
    kakao_api_key VARCHAR(200),
    kakao_enabled CHAR(1) DEFAULT 'N',
    payment_methods TEXT DEFAULT 'CARD,CASH',
    require_deposit CHAR(1) DEFAULT 'N',
    deposit_amount INTEGER DEFAULT 0,
    allow_cancellation CHAR(1) DEFAULT 'Y',
    cancel_deadline_hours INTEGER DEFAULT 24,
    no_show_penalty_enabled CHAR(1) DEFAULT 'N',
    timezone VARCHAR(50) DEFAULT 'Asia/Seoul',
    language VARCHAR(10) DEFAULT 'ko',
    regular_threshold INTEGER DEFAULT 3,
    vip_threshold INTEGER DEFAULT 10,
    vip_spend_threshold DECIMAL(10,2) DEFAULT 500000,
    vip_benefit_description TEXT,
    kakao_sender_id VARCHAR(20),
    kakao_alimtalk_triggers JSONB DEFAULT '{"onReservationCreated":true,"onReservationConfirmed":true,"onReservationCancelled":true,"onReservationReminder":true,"reminderHoursBefore":24}',
    kakao_verified_at TIMESTAMP,
    onboarding_completed CHAR(1) DEFAULT 'N',
    onboarding_skipped CHAR(1) DEFAULT 'N',
    onboarding_step_service CHAR(1) DEFAULT 'N',
    onboarding_step_staff CHAR(1) DEFAULT 'N',
    onboarding_step_reservation CHAR(1) DEFAULT 'N',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE business_settings IS '매장 예약 시스템 설정';
COMMENT ON COLUMN business_settings.booking_interval IS '예약 시간 간격 (분)';
COMMENT ON COLUMN business_settings.auto_confirm IS '예약 자동 확정 (Y/N)';
COMMENT ON COLUMN business_settings.allow_online_booking IS '온라인 예약 허용 (Y/N)';
COMMENT ON COLUMN business_settings.max_advance_booking_days IS '최대 사전 예약 가능 일수';
COMMENT ON COLUMN business_settings.min_advance_booking_hours IS '최소 사전 예약 시간';
COMMENT ON COLUMN business_settings.kakao_channel_id IS '카카오 채널 ID';
COMMENT ON COLUMN business_settings.kakao_api_key IS '카카오 API KEY';
COMMENT ON COLUMN business_settings.kakao_enabled IS '카카오 알림톡 사용 여부 (Y/N)';
COMMENT ON COLUMN business_settings.kakao_sender_id IS '카카오 발신 프로필 키';
COMMENT ON COLUMN business_settings.kakao_alimtalk_triggers IS '카카오 알림톡 트리거 설정 (JSONB)';
COMMENT ON COLUMN business_settings.kakao_verified_at IS '카카오 채널 인증 일시';
COMMENT ON COLUMN business_settings.payment_methods IS '결제 수단 (콤마 구분)';
COMMENT ON COLUMN business_settings.timezone IS '시간대';
COMMENT ON COLUMN business_settings.language IS '언어';
COMMENT ON COLUMN business_settings.onboarding_completed IS '온보딩 완료 여부 (Y/N)';
COMMENT ON COLUMN business_settings.onboarding_skipped IS '온보딩 건너뛰기 여부 (Y/N)';
COMMENT ON COLUMN business_settings.onboarding_step_service IS '서비스 등록 스텝 완료 (Y/N)';
COMMENT ON COLUMN business_settings.onboarding_step_staff IS '스태프 등록 스텝 완료 (Y/N)';
COMMENT ON COLUMN business_settings.onboarding_step_reservation IS '예약 생성 스텝 완료 (Y/N)';

CREATE UNIQUE INDEX idx_business_settings_business_id ON business_settings(business_id);

-- =============================================================================
-- 3. 직원
-- =============================================================================

-- 직원 테이블
CREATE TABLE staffs (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    position VARCHAR(50),
    position_id BIGINT,
    phone VARCHAR(20),
    email VARCHAR(100),
    specialty TEXT,
    career_years INTEGER DEFAULT 0,
    profile_image_url TEXT,
    introduction TEXT,
    is_active CHAR(1) DEFAULT 'Y',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE staffs IS '직원 (디자이너/강사)';
COMMENT ON COLUMN staffs.id IS '직원 ID';
COMMENT ON COLUMN staffs.business_id IS '소속 매장 ID';
COMMENT ON COLUMN staffs.name IS '이름';
COMMENT ON COLUMN staffs.position IS '직급 텍스트 (원장, 실장, 디자이너 등)';
COMMENT ON COLUMN staffs.position_id IS '직급 ID (staff_positions 참조)';
COMMENT ON COLUMN staffs.phone IS '전화번호';
COMMENT ON COLUMN staffs.email IS '이메일';
COMMENT ON COLUMN staffs.specialty IS '전문분야 (예: 펌, 컬러, 남성컷)';
COMMENT ON COLUMN staffs.career_years IS '경력 (년)';
COMMENT ON COLUMN staffs.profile_image_url IS '프로필 이미지 URL';
COMMENT ON COLUMN staffs.introduction IS '소개글';
COMMENT ON COLUMN staffs.is_active IS '활성 여부 (Y/N)';
COMMENT ON COLUMN staffs.created_at IS '생성일시';
COMMENT ON COLUMN staffs.updated_at IS '수정일시';

CREATE INDEX idx_staffs_business_id ON staffs(business_id);
CREATE INDEX idx_staffs_position_id ON staffs(position_id);

-- 직급 테이블
CREATE TABLE staff_positions (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE staff_positions IS '직급 (원장, 실장, 디자이너 등)';
COMMENT ON COLUMN staff_positions.id IS '직급 ID';
COMMENT ON COLUMN staff_positions.business_id IS '매장 ID';
COMMENT ON COLUMN staff_positions.name IS '직급명';
COMMENT ON COLUMN staff_positions.description IS '설명';
COMMENT ON COLUMN staff_positions.sort_order IS '정렬 순서';
COMMENT ON COLUMN staff_positions.created_at IS '생성일시';
COMMENT ON COLUMN staff_positions.updated_at IS '수정일시';

CREATE INDEX idx_staff_positions_business_id ON staff_positions(business_id);
CREATE UNIQUE INDEX idx_staff_positions_business_name ON staff_positions(business_id, name);

-- 포트폴리오 테이블
CREATE TABLE portfolios (
    id BIGSERIAL PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    title VARCHAR(100),
    description TEXT,
    image_url TEXT NOT NULL,
    tags TEXT,
    service_category VARCHAR(100),
    sort_order INTEGER DEFAULT 0,
    is_visible CHAR(1) DEFAULT 'Y',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE portfolios IS '직원 포트폴리오';
COMMENT ON COLUMN portfolios.id IS '포트폴리오 ID';
COMMENT ON COLUMN portfolios.staff_id IS '직원 ID';
COMMENT ON COLUMN portfolios.title IS '제목';
COMMENT ON COLUMN portfolios.description IS '설명';
COMMENT ON COLUMN portfolios.image_url IS '이미지 URL';
COMMENT ON COLUMN portfolios.tags IS '태그 (콤마 구분)';
COMMENT ON COLUMN portfolios.service_category IS '서비스 카테고리 (예: 커트, 펌, 컬러)';
COMMENT ON COLUMN portfolios.sort_order IS '정렬 순서';
COMMENT ON COLUMN portfolios.is_visible IS '공개 여부 (Y/N)';
COMMENT ON COLUMN portfolios.created_at IS '생성일시';

CREATE INDEX idx_portfolios_staff_id ON portfolios(staff_id);
CREATE INDEX idx_portfolios_sort_order ON portfolios(staff_id, sort_order);

-- =============================================================================
-- 4. 서비스
-- =============================================================================

-- 서비스 카테고리 테이블
CREATE TABLE service_categories (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE service_categories IS '서비스 카테고리';
COMMENT ON COLUMN service_categories.id IS '카테고리 ID';
COMMENT ON COLUMN service_categories.business_id IS '매장 ID';
COMMENT ON COLUMN service_categories.name IS '카테고리명';
COMMENT ON COLUMN service_categories.description IS '설명';
COMMENT ON COLUMN service_categories.sort_order IS '정렬 순서';
COMMENT ON COLUMN service_categories.created_at IS '생성일시';
COMMENT ON COLUMN service_categories.updated_at IS '수정일시';

CREATE INDEX idx_service_categories_business_id ON service_categories(business_id);
CREATE UNIQUE INDEX idx_service_categories_business_name ON service_categories(business_id, name);

-- 서비스 테이블
CREATE TABLE services (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    category_id BIGINT,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    duration INTEGER NOT NULL,
    price INTEGER NOT NULL,
    staff_ids TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active CHAR(1) DEFAULT 'Y',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE services IS '서비스 메뉴 (시술/수업)';
COMMENT ON COLUMN services.id IS '서비스 ID';
COMMENT ON COLUMN services.business_id IS '매장 ID';
COMMENT ON COLUMN services.category_id IS '카테고리 ID (service_categories.id, NULL=미분류)';
COMMENT ON COLUMN services.name IS '서비스명';
COMMENT ON COLUMN services.description IS '설명';
COMMENT ON COLUMN services.duration IS '소요시간 (분)';
COMMENT ON COLUMN services.price IS '가격 (원)';
COMMENT ON COLUMN services.staff_ids IS '담당 가능 직원 ID 목록 (콤마 구분)';
COMMENT ON COLUMN services.sort_order IS '정렬 순서';
COMMENT ON COLUMN services.is_active IS '활성 여부 (Y/N)';
COMMENT ON COLUMN services.created_at IS '생성일시';
COMMENT ON COLUMN services.updated_at IS '수정일시';

CREATE INDEX idx_services_business_id ON services(business_id);
CREATE INDEX idx_services_category_id ON services(category_id);

-- =============================================================================
-- 5. 고객
-- =============================================================================

-- 고객 테이블
CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    user_id BIGINT,
    name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    birth_date DATE,
    gender VARCHAR(10),
    visit_count INTEGER DEFAULT 0,
    total_spent INTEGER DEFAULT 0,
    last_visit_date DATE,
    tags TEXT,
    memo TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE customers IS '고객';
COMMENT ON COLUMN customers.id IS '고객 ID';
COMMENT ON COLUMN customers.business_id IS '매장 ID';
COMMENT ON COLUMN customers.user_id IS '로그인 고객 사용자 ID (users 테이블)';
COMMENT ON COLUMN customers.name IS '이름';
COMMENT ON COLUMN customers.phone IS '전화번호';
COMMENT ON COLUMN customers.email IS '이메일';
COMMENT ON COLUMN customers.birth_date IS '생년월일';
COMMENT ON COLUMN customers.gender IS '성별 (MALE, FEMALE, OTHER)';
COMMENT ON COLUMN customers.visit_count IS '방문 횟수';
COMMENT ON COLUMN customers.total_spent IS '총 결제 금액 (원)';
COMMENT ON COLUMN customers.last_visit_date IS '마지막 방문일';
COMMENT ON COLUMN customers.tags IS '태그 (VIP, 단골, 신규 등 - 콤마 구분)';
COMMENT ON COLUMN customers.memo IS '메모';
COMMENT ON COLUMN customers.created_at IS '생성일시';
COMMENT ON COLUMN customers.updated_at IS '수정일시';

CREATE INDEX idx_customers_business_id ON customers(business_id);
CREATE INDEX idx_customers_phone ON customers(business_id, phone);

-- 고객 시술 이력 테이블
CREATE TABLE customer_histories (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    reservation_id BIGINT,
    visit_date DATE NOT NULL,
    services JSONB NOT NULL,
    total_price INTEGER NOT NULL,
    payment_method VARCHAR(20),
    details JSONB,
    before_image_url TEXT,
    after_image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE customer_histories IS '고객 시술 이력';
COMMENT ON COLUMN customer_histories.id IS '이력 ID';
COMMENT ON COLUMN customer_histories.business_id IS '매장 ID';
COMMENT ON COLUMN customer_histories.customer_id IS '고객 ID';
COMMENT ON COLUMN customer_histories.staff_id IS '담당 직원 ID';
COMMENT ON COLUMN customer_histories.reservation_id IS '연결된 예약 ID (자동 생성 시)';
COMMENT ON COLUMN customer_histories.visit_date IS '방문일';
COMMENT ON COLUMN customer_histories.services IS '서비스 목록 (JSONB)';
COMMENT ON COLUMN customer_histories.total_price IS '총 결제 금액 (원)';
COMMENT ON COLUMN customer_histories.payment_method IS '결제 수단 (CARD, CASH, TRANSFER 등)';
COMMENT ON COLUMN customer_histories.details IS '상세 정보 (JSONB - 사용약품, 시술내용 등)';
COMMENT ON COLUMN customer_histories.before_image_url IS '시술 전 이미지 URL';
COMMENT ON COLUMN customer_histories.after_image_url IS '시술 후 이미지 URL';
COMMENT ON COLUMN customer_histories.created_at IS '생성일시';

CREATE INDEX idx_customer_histories_customer_id ON customer_histories(customer_id);
CREATE INDEX idx_customer_histories_business_date ON customer_histories(business_id, visit_date);

-- =============================================================================
-- 6. 예약 / 휴무
-- =============================================================================

-- 예약 테이블
CREATE TABLE reservations (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    user_id BIGINT,
    staff_id BIGINT,
    reservation_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    services JSONB NOT NULL,
    total_duration INTEGER NOT NULL,
    total_price INTEGER NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    reservation_number VARCHAR(50) NOT NULL,
    customer_memo TEXT,
    staff_memo TEXT,
    cancelled_at TIMESTAMP,
    cancel_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE reservations IS '예약';
COMMENT ON COLUMN reservations.id IS '예약 ID';
COMMENT ON COLUMN reservations.business_id IS '매장 ID';
COMMENT ON COLUMN reservations.customer_id IS '고객 ID';
COMMENT ON COLUMN reservations.user_id IS '로그인 고객 사용자 ID (users 테이블)';
COMMENT ON COLUMN reservations.staff_id IS '담당 직원 ID';
COMMENT ON COLUMN reservations.reservation_date IS '예약 날짜';
COMMENT ON COLUMN reservations.start_time IS '시작 시각';
COMMENT ON COLUMN reservations.end_time IS '종료 시각';
COMMENT ON COLUMN reservations.services IS '서비스 목록 (JSONB: [{id, name, price, duration}])';
COMMENT ON COLUMN reservations.total_duration IS '총 소요시간 (분)';
COMMENT ON COLUMN reservations.total_price IS '총 가격 (원)';
COMMENT ON COLUMN reservations.status IS '상태 (PENDING, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW)';
COMMENT ON COLUMN reservations.reservation_number IS '예약 번호';
COMMENT ON COLUMN reservations.customer_memo IS '고객 요청사항';
COMMENT ON COLUMN reservations.staff_memo IS '직원 메모';
COMMENT ON COLUMN reservations.cancelled_at IS '취소 시각';
COMMENT ON COLUMN reservations.cancel_reason IS '취소 사유';
COMMENT ON COLUMN reservations.created_at IS '생성일시';
COMMENT ON COLUMN reservations.updated_at IS '수정일시';

CREATE UNIQUE INDEX idx_reservations_number ON reservations(reservation_number);
CREATE INDEX idx_reservations_business_date ON reservations(business_id, reservation_date);
CREATE INDEX idx_reservations_customer_id ON reservations(customer_id);
CREATE INDEX idx_reservations_staff_id ON reservations(staff_id);
CREATE INDEX idx_reservations_user_id ON reservations(user_id);

-- 특별 휴무일 테이블
CREATE TABLE special_holidays (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    name VARCHAR(100),
    "date" DATE NOT NULL,
    type VARCHAR(20) DEFAULT 'REGULAR',
    reason VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE special_holidays IS '특별 휴무일';
COMMENT ON COLUMN special_holidays.id IS '휴무일 ID';
COMMENT ON COLUMN special_holidays.business_id IS '매장 ID';
COMMENT ON COLUMN special_holidays.name IS '휴무일명 (설날, 추석 등)';
COMMENT ON COLUMN special_holidays."date" IS '휴무 날짜';
COMMENT ON COLUMN special_holidays.type IS '유형 (REGULAR, TEMPORARY, NATIONAL)';
COMMENT ON COLUMN special_holidays.reason IS '휴무 사유';
COMMENT ON COLUMN special_holidays.created_at IS '생성일시';

CREATE INDEX idx_special_holidays_business_date ON special_holidays(business_id, "date");

-- =============================================================================
-- 7. 감사 로그
-- =============================================================================

-- 감사 로그 테이블
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    user_email VARCHAR(100),
    user_role VARCHAR(20),
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    description TEXT,
    metadata JSONB,
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE audit_logs IS '감사 로그 (시스템 중요 액션 기록)';
COMMENT ON COLUMN audit_logs.id IS '로그 ID';
COMMENT ON COLUMN audit_logs.user_id IS '액션 수행 사용자 ID';
COMMENT ON COLUMN audit_logs.user_email IS '액션 수행 사용자 이메일';
COMMENT ON COLUMN audit_logs.user_role IS '액션 수행 사용자 역할';
COMMENT ON COLUMN audit_logs.action IS '액션 타입 (BUSINESS_CREATED, USER_ROLE_CHANGED 등)';
COMMENT ON COLUMN audit_logs.entity_type IS '대상 엔티티 타입 (Business, User, Reservation 등)';
COMMENT ON COLUMN audit_logs.entity_id IS '대상 엔티티 ID';
COMMENT ON COLUMN audit_logs.description IS '액션 설명';
COMMENT ON COLUMN audit_logs.metadata IS '추가 정보 (JSONB - 변경 전/후 값 등)';
COMMENT ON COLUMN audit_logs.ip_address IS 'IP 주소';
COMMENT ON COLUMN audit_logs.user_agent IS 'User-Agent';
COMMENT ON COLUMN audit_logs.created_at IS '생성일시';

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

-- =============================================================================
-- 8. 쿠폰 & 결제 (SaaS 구독)
-- =============================================================================

-- 매장 쿠폰 테이블 (매장별 고객 쿠폰)
CREATE TABLE business_coupons (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    coupon_type VARCHAR(20) NOT NULL,
    discount_amount INTEGER,
    discount_percentage INTEGER,
    max_discount_amount INTEGER,
    min_order_amount INTEGER DEFAULT 0,
    max_usage_count INTEGER,
    current_usage_count INTEGER DEFAULT 0,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_business_coupons_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
);

COMMENT ON TABLE business_coupons IS '매장별 쿠폰 관리';
COMMENT ON COLUMN business_coupons.coupon_type IS '쿠폰 타입 (PERCENTAGE: 정률, FIXED_AMOUNT: 정액)';
COMMENT ON COLUMN business_coupons.status IS '쿠폰 상태 (ACTIVE, EXPIRED, DISABLED)';

CREATE INDEX idx_business_coupons_business_id ON business_coupons(business_id);
CREATE INDEX idx_business_coupons_code ON business_coupons(code);
CREATE INDEX idx_business_coupons_status ON business_coupons(status);
CREATE INDEX idx_business_coupons_valid_until ON business_coupons(valid_until);

-- 시스템 쿠폰 테이블 (슈퍼관리자 발행, SaaS 구독 할인)
CREATE TABLE coupons (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    discount_type VARCHAR(20) NOT NULL,
    discount_value INTEGER NOT NULL,
    max_discount_amount INTEGER,
    applicable_plans TEXT,
    min_purchase_amount INTEGER DEFAULT 0,
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    max_total_uses INTEGER,
    current_total_uses INTEGER DEFAULT 0,
    max_uses_per_business INTEGER DEFAULT 1,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_discount_value CHECK (discount_value > 0),
    CONSTRAINT check_max_discount_amount CHECK (max_discount_amount IS NULL OR max_discount_amount > 0),
    CONSTRAINT check_min_purchase_amount CHECK (min_purchase_amount >= 0),
    CONSTRAINT check_max_uses CHECK (max_total_uses IS NULL OR max_total_uses > 0),
    CONSTRAINT check_uses_per_business CHECK (max_uses_per_business > 0),
    CONSTRAINT check_current_uses CHECK (current_total_uses >= 0),
    CONSTRAINT check_valid_period CHECK (valid_from < valid_until),
    CONSTRAINT fk_coupons_created_by FOREIGN KEY (created_by) REFERENCES users(id)
);

COMMENT ON TABLE coupons IS '시스템 쿠폰 (SaaS 구독 할인용)';
COMMENT ON COLUMN coupons.code IS '쿠폰 코드 (유니크)';
COMMENT ON COLUMN coupons.discount_type IS '할인 타입 (PERCENTAGE, FIXED_AMOUNT)';
COMMENT ON COLUMN coupons.discount_value IS '할인 값 (PERCENTAGE: 1~100, FIXED_AMOUNT: 원)';
COMMENT ON COLUMN coupons.max_discount_amount IS '최대 할인 금액 (PERCENTAGE 타입)';
COMMENT ON COLUMN coupons.applicable_plans IS '적용 가능 플랜 (콤마 구분: BASIC, null=전체)';
COMMENT ON COLUMN coupons.min_purchase_amount IS '최소 구매 금액';
COMMENT ON COLUMN coupons.valid_from IS '쿠폰 유효 시작일';
COMMENT ON COLUMN coupons.valid_until IS '쿠폰 유효 종료일';
COMMENT ON COLUMN coupons.max_total_uses IS '전체 사용 가능 횟수 (null=무제한)';
COMMENT ON COLUMN coupons.current_total_uses IS '현재 사용된 횟수';
COMMENT ON COLUMN coupons.max_uses_per_business IS '매장당 사용 가능 횟수';
COMMENT ON COLUMN coupons.status IS '쿠폰 상태 (ACTIVE, INACTIVE, EXPIRED)';
COMMENT ON COLUMN coupons.created_by IS '쿠폰 생성자 (SUPER_ADMIN)';

CREATE INDEX idx_coupons_code ON coupons(code);
CREATE INDEX idx_coupons_status ON coupons(status);
CREATE INDEX idx_coupons_valid_period ON coupons(valid_from, valid_until);
CREATE INDEX idx_coupons_created_at ON coupons(created_at DESC);

-- 구독 결제 내역 테이블
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    coupon_id BIGINT,
    subscription_plan VARCHAR(20) NOT NULL,
    billing_cycle VARCHAR(10),
    billing_period_start DATE NOT NULL,
    billing_period_end DATE NOT NULL,
    amount INTEGER NOT NULL,
    discount_amount INTEGER DEFAULT 0,
    final_amount INTEGER NOT NULL,
    coupon_code VARCHAR(50),
    payment_method VARCHAR(20) DEFAULT 'CARD',
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    pg_provider VARCHAR(50),
    pg_transaction_id VARCHAR(200),
    webhook_received_at TIMESTAMP,
    webhook_data JSONB,
    paid_at TIMESTAMP,
    failed_reason TEXT,
    refunded_at TIMESTAMP,
    refund_amount INTEGER,
    cancel_reason TEXT,
    cancelled_at TIMESTAMP,
    is_extension BOOLEAN DEFAULT FALSE,
    previous_billing_end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_billing_period CHECK (billing_period_start <= billing_period_end),
    CONSTRAINT check_payment_amount CHECK (amount >= 0 AND discount_amount >= 0 AND final_amount >= 0),
    CONSTRAINT fk_payments_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_payments_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id)
);

COMMENT ON TABLE payments IS '구독 결제 내역';
COMMENT ON COLUMN payments.subscription_plan IS '구독 플랜 (FREE, BASIC)';
COMMENT ON COLUMN payments.billing_cycle IS '결제 주기 (MONTHLY, YEARLY)';
COMMENT ON COLUMN payments.billing_period_start IS '청구 기간 시작일';
COMMENT ON COLUMN payments.billing_period_end IS '청구 기간 종료일';
COMMENT ON COLUMN payments.amount IS '원래 금액 (할인 전)';
COMMENT ON COLUMN payments.discount_amount IS '할인 금액';
COMMENT ON COLUMN payments.final_amount IS '최종 결제 금액';
COMMENT ON COLUMN payments.payment_method IS '결제 수단 (CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT, MOBILE)';
COMMENT ON COLUMN payments.payment_status IS '결제 상태 (PENDING, COMPLETED, FAILED, REFUNDED, CANCELLED)';
COMMENT ON COLUMN payments.pg_provider IS 'PG사 (toss, iamport 등)';
COMMENT ON COLUMN payments.pg_transaction_id IS 'PG사 거래 ID';
COMMENT ON COLUMN payments.webhook_data IS '웹훅 원본 데이터 (JSONB)';
COMMENT ON COLUMN payments.cancel_reason IS '결제 취소 사유';
COMMENT ON COLUMN payments.cancelled_at IS '결제 취소 시각';
COMMENT ON COLUMN payments.is_extension IS '기간 연장 결제 여부';
COMMENT ON COLUMN payments.previous_billing_end_date IS '연장 전 기존 종료일';

CREATE INDEX idx_payments_business_id ON payments(business_id);
CREATE INDEX idx_payments_status ON payments(payment_status);
CREATE INDEX idx_payments_billing_period ON payments(billing_period_start, billing_period_end);
CREATE INDEX idx_payments_pg_transaction_id ON payments(pg_transaction_id);
CREATE INDEX idx_payments_created_at ON payments(created_at DESC);

-- 시스템 쿠폰 사용 내역 테이블
CREATE TABLE coupon_usages (
    id BIGSERIAL PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    business_id BIGINT NOT NULL,
    payment_id BIGINT,
    discount_amount INTEGER NOT NULL,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_coupon_usage_discount CHECK (discount_amount >= 0),
    CONSTRAINT uq_coupon_usage_per_business UNIQUE (coupon_id, business_id),
    CONSTRAINT fk_coupon_usages_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE,
    CONSTRAINT fk_coupon_usages_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_coupon_usages_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
);

COMMENT ON TABLE coupon_usages IS '시스템 쿠폰 사용 내역';
COMMENT ON COLUMN coupon_usages.discount_amount IS '실제 할인된 금액';
COMMENT ON COLUMN coupon_usages.used_at IS '쿠폰 사용 시각';

CREATE INDEX idx_coupon_usages_coupon_id ON coupon_usages(coupon_id);
CREATE INDEX idx_coupon_usages_business_id ON coupon_usages(business_id);
CREATE INDEX idx_coupon_usages_payment_id ON coupon_usages(payment_id);
CREATE INDEX idx_coupon_usages_used_at ON coupon_usages(used_at DESC);

-- 매장 쿠폰 사용 내역 테이블
CREATE TABLE business_coupon_usages (
    id BIGSERIAL PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    payment_id BIGINT,
    discount_amount INTEGER NOT NULL,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    canceled CHAR(1) DEFAULT 'N',
    canceled_at TIMESTAMP,
    CONSTRAINT fk_bcu_coupon FOREIGN KEY (coupon_id) REFERENCES business_coupons(id) ON DELETE CASCADE,
    CONSTRAINT fk_bcu_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_bcu_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL
);

COMMENT ON TABLE business_coupon_usages IS '매장 쿠폰 사용 내역';

CREATE INDEX idx_business_coupon_usages_coupon_id ON business_coupon_usages(coupon_id);
CREATE INDEX idx_business_coupon_usages_user_id ON business_coupon_usages(user_id);
CREATE INDEX idx_business_coupon_usages_payment_id ON business_coupon_usages(payment_id);

-- =============================================================================
-- 9. 알림
-- =============================================================================
CREATE TABLE notifications (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               business_id BIGINT,
                               type VARCHAR(30) NOT NULL,
                               title VARCHAR(200) NOT NULL,
                               message TEXT NOT NULL,
                               link VARCHAR(500),
                               reference_type VARCHAR(50),
                               reference_id BIGINT,
                               is_read CHAR(1) DEFAULT 'N',
                               read_at TIMESTAMP,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

COMMENT ON TABLE notifications IS '사용자 알림';
COMMENT ON COLUMN notifications.user_id IS '수신 사용자 ID';
COMMENT ON COLUMN notifications.business_id IS '관련 매장 ID';
COMMENT ON COLUMN notifications.type IS '알림 유형 (RESERVATION_NEW, RESERVATION_CONFIRMED, RESERVATION_CANCELLED, RESERVATION_COMPLETED, SYSTEM)';
COMMENT ON COLUMN notifications.title IS '알림 제목';
COMMENT ON COLUMN notifications.message IS '알림 내용';
COMMENT ON COLUMN notifications.link IS '클릭 시 이동 경로';
COMMENT ON COLUMN notifications.reference_type IS '참조 엔티티 타입';
COMMENT ON COLUMN notifications.reference_id IS '참조 엔티티 ID';
COMMENT ON COLUMN notifications.is_read IS '읽음 여부 (Y/N)';
COMMENT ON COLUMN notifications.read_at IS '읽은 시각';

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = 'N';
CREATE INDEX idx_notifications_business_id ON notifications(business_id);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);

-- =============================================================================
-- 10. 직원 근무 스케줄
-- =============================================================================

CREATE TABLE staff_schedules (
    id BIGSERIAL PRIMARY KEY,
    staff_id BIGINT NOT NULL,
    business_id BIGINT NOT NULL,
    day_of_week INTEGER NOT NULL,
    start_time TIME,
    end_time TIME,
    break_start_time TIME,
    break_end_time TIME,
    is_working CHAR(1) DEFAULT 'Y',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_schedules_staff FOREIGN KEY (staff_id) REFERENCES staffs(id) ON DELETE CASCADE,
    CONSTRAINT fk_staff_schedules_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT uq_staff_schedule_day UNIQUE (staff_id, day_of_week),
    CONSTRAINT check_day_of_week CHECK (day_of_week BETWEEN 1 AND 7)
);

COMMENT ON TABLE staff_schedules IS '직원 근무 스케줄 (요일별)';
COMMENT ON COLUMN staff_schedules.staff_id IS '직원 ID';
COMMENT ON COLUMN staff_schedules.business_id IS '매장 ID';
COMMENT ON COLUMN staff_schedules.day_of_week IS '요일 (1=월, 2=화, ..., 7=일)';
COMMENT ON COLUMN staff_schedules.start_time IS '근무 시작 시간';
COMMENT ON COLUMN staff_schedules.end_time IS '근무 종료 시간';
COMMENT ON COLUMN staff_schedules.break_start_time IS '휴식 시작 시간';
COMMENT ON COLUMN staff_schedules.break_end_time IS '휴식 종료 시간';
COMMENT ON COLUMN staff_schedules.is_working IS '근무 여부 (Y/N)';

CREATE INDEX idx_staff_schedules_staff_id ON staff_schedules(staff_id);
CREATE INDEX idx_staff_schedules_business_id ON staff_schedules(business_id);

-- =============================================================================
-- 11. 리뷰
=============================================================================

CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    reservation_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    staff_id BIGINT,
    customer_name VARCHAR(50) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    rating INTEGER NOT NULL,
    content TEXT,
    images JSONB,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    reply_content TEXT,
    reply_created_at TIMESTAMP,
    delete_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reviews_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(id),
    CONSTRAINT fk_reviews_customer FOREIGN KEY (customer_id) REFERENCES customers(id),
    CONSTRAINT check_rating CHECK (rating BETWEEN 1 AND 5)
);

COMMENT ON TABLE reviews IS '고객 리뷰';
COMMENT ON COLUMN reviews.business_id IS '매장 ID';
COMMENT ON COLUMN reviews.reservation_id IS '예약 ID (1예약 1리뷰)';
COMMENT ON COLUMN reviews.customer_id IS '고객 ID';
COMMENT ON COLUMN reviews.staff_id IS '담당 스태프 ID';
COMMENT ON COLUMN reviews.customer_name IS '고객 이름 (비정규화)';
COMMENT ON COLUMN reviews.customer_phone IS '고객 전화번호 (본인 확인용)';
COMMENT ON COLUMN reviews.rating IS '별점 (1~5)';
COMMENT ON COLUMN reviews.content IS '리뷰 내용';
COMMENT ON COLUMN reviews.images IS '리뷰 이미지 URL 목록 (JSONB)';
COMMENT ON COLUMN reviews.status IS '리뷰 상태 (ACTIVE, HIDDEN, DELETED)';
COMMENT ON COLUMN reviews.reply_content IS '관리자 답변';
COMMENT ON COLUMN reviews.reply_created_at IS '답변 작성 시각';
COMMENT ON COLUMN reviews.delete_reason IS '삭제 사유';

CREATE UNIQUE INDEX idx_reviews_reservation_id ON reviews(reservation_id);
CREATE INDEX idx_reviews_business_id ON reviews(business_id);
CREATE INDEX idx_reviews_business_status ON reviews(business_id, status);
CREATE INDEX idx_reviews_business_rating ON reviews(business_id, rating);
CREATE INDEX idx_reviews_staff_id ON reviews(staff_id);
CREATE INDEX idx_reviews_created_at ON reviews(created_at DESC);

-- =============================================================================
-- 12. 알림 발송 로그
-- =============================================================================

CREATE TABLE notification_logs (
    id BIGSERIAL PRIMARY KEY,
    business_id BIGINT NOT NULL,
    reservation_id BIGINT,
    channel VARCHAR(20) NOT NULL,
    template_type VARCHAR(50) NOT NULL,
    recipient_phone VARCHAR(20),
    recipient_name VARCHAR(50),
    title VARCHAR(200),
    content TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    sent_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_logs_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
);

COMMENT ON TABLE notification_logs IS '알림 발송 기록 (카카오/SMS/이메일)';
COMMENT ON COLUMN notification_logs.channel IS '발송 채널 (KAKAO, SMS, EMAIL, SYSTEM)';
COMMENT ON COLUMN notification_logs.template_type IS '템플릿 타입 (RESERVATION_CREATED, RESERVATION_CONFIRMED 등)';
COMMENT ON COLUMN notification_logs.status IS '발송 상태 (PENDING, SENT, FAILED)';

CREATE INDEX idx_notification_logs_business_id ON notification_logs(business_id);
CREATE INDEX idx_notification_logs_reservation_id ON notification_logs(reservation_id);
CREATE INDEX idx_notification_logs_status ON notification_logs(status);
CREATE INDEX idx_notification_logs_created_at ON notification_logs(created_at DESC);

-- =============================================================================
-- 가격정책 변경 마이그레이션 (4티어 → 2티어)
-- =============================================================================

-- businesses 테이블에 billing_cycle 컬럼 추가
ALTER TABLE businesses ADD COLUMN IF NOT EXISTS billing_cycle VARCHAR(10) DEFAULT 'MONTHLY';

-- payments 테이블에 billing_cycle 컬럼 추가
ALTER TABLE payments ADD COLUMN IF NOT EXISTS billing_cycle VARCHAR(10);

-- PRO/ENTERPRISE → BASIC 전환
UPDATE businesses SET subscription_plan = 'BASIC', billing_cycle = 'MONTHLY'
WHERE subscription_plan IN ('PRO', 'ENTERPRISE');

-- 기존 BASIC → billing_cycle 기본값
UPDATE businesses SET billing_cycle = 'MONTHLY' WHERE subscription_plan = 'BASIC' AND billing_cycle IS NULL;

-- FREE → billing_cycle 기본값
UPDATE businesses SET billing_cycle = 'MONTHLY' WHERE billing_cycle IS NULL;

-- =============================================================================
-- 문의(Inquiry) 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS inquiries (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    type VARCHAR(30) NOT NULL DEFAULT 'GENERAL',
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_note TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE inquiries IS '문의하기';
COMMENT ON COLUMN inquiries.type IS '문의 유형 (GENERAL, FEATURE_REQUEST, BUG_REPORT, PARTNERSHIP)';
COMMENT ON COLUMN inquiries.status IS '처리 상태 (PENDING, IN_PROGRESS, RESOLVED, CLOSED)';
COMMENT ON COLUMN inquiries.admin_note IS '관리자 메모';
COMMENT ON COLUMN inquiries.ip_address IS '문의자 IP (스팸 방지용)';

CREATE INDEX IF NOT EXISTS idx_inquiries_status ON inquiries(status);
CREATE INDEX IF NOT EXISTS idx_inquiries_type ON inquiries(type);
CREATE INDEX IF NOT EXISTS idx_inquiries_created_at ON inquiries(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_inquiries_ip_address ON inquiries(ip_address);

-- =============================================================================
-- 13. 고객 즐겨찾기 (북마크)
-- =============================================================================

CREATE TABLE IF NOT EXISTS customer_bookmarks (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    business_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bookmarks_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_bookmarks_business FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
    CONSTRAINT uq_bookmark_user_business UNIQUE (user_id, business_id)
);

COMMENT ON TABLE customer_bookmarks IS '고객 매장 즐겨찾기';
COMMENT ON COLUMN customer_bookmarks.user_id IS '고객(사용자) ID';
COMMENT ON COLUMN customer_bookmarks.business_id IS '매장 ID';

CREATE INDEX IF NOT EXISTS idx_bookmarks_user_id ON customer_bookmarks(user_id);
CREATE INDEX IF NOT EXISTS idx_bookmarks_business_id ON customer_bookmarks(business_id);

-- =============================================================================
-- 14. 리뷰 이미지
-- =============================================================================

CREATE TABLE IF NOT EXISTS review_images (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL,
    image_url TEXT NOT NULL,
    thumbnail_url TEXT,
    original_filename VARCHAR(255),
    file_size INTEGER,
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_images_review FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE
);

COMMENT ON TABLE review_images IS '리뷰 이미지';
COMMENT ON COLUMN review_images.review_id IS '리뷰 ID';
COMMENT ON COLUMN review_images.image_url IS '이미지 URL';
COMMENT ON COLUMN review_images.thumbnail_url IS '썸네일 URL';
COMMENT ON COLUMN review_images.original_filename IS '원본 파일명';
COMMENT ON COLUMN review_images.file_size IS '파일 크기 (bytes)';
COMMENT ON COLUMN review_images.sort_order IS '정렬 순서';

CREATE INDEX IF NOT EXISTS idx_review_images_review_id ON review_images(review_id);

-- =============================================================================
-- 15. 공지 방송
-- =============================================================================

CREATE TABLE IF NOT EXISTS broadcasts (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    target_type VARCHAR(20) NOT NULL DEFAULT 'ALL',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    sent_by BIGINT NOT NULL,
    sent_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    recipient_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_broadcasts_sent_by FOREIGN KEY (sent_by) REFERENCES users(id)
);

COMMENT ON TABLE broadcasts IS '전체 공지 방송';
COMMENT ON COLUMN broadcasts.title IS '공지 제목';
COMMENT ON COLUMN broadcasts.content IS '공지 내용';
COMMENT ON COLUMN broadcasts.target_type IS '발송 대상 (ALL, PAID, TRIAL, FREE)';
COMMENT ON COLUMN broadcasts.priority IS '우선순위 (LOW, NORMAL, HIGH, URGENT)';
COMMENT ON COLUMN broadcasts.sent_by IS '발송자 (슈퍼 관리자) ID';
COMMENT ON COLUMN broadcasts.status IS '상태 (DRAFT, SENT)';
COMMENT ON COLUMN broadcasts.recipient_count IS '수신 매장 수';

CREATE INDEX IF NOT EXISTS idx_broadcasts_status ON broadcasts(status);
CREATE INDEX IF NOT EXISTS idx_broadcasts_target_type ON broadcasts(target_type);
CREATE INDEX IF NOT EXISTS idx_broadcasts_created_at ON broadcasts(created_at DESC);

-- =============================================================================
-- 서비스 이미지
-- =============================================================================
CREATE TABLE IF NOT EXISTS service_images (
    id BIGSERIAL PRIMARY KEY,
    service_id BIGINT NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    business_id BIGINT NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    image_url VARCHAR(500) NOT NULL,
    thumbnail_url VARCHAR(500),
    original_filename VARCHAR(255),
    file_size BIGINT DEFAULT 0,
    sort_order INTEGER DEFAULT 0,
    caption VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_service_images_service_id ON service_images(service_id);

-- =============================================================================
-- 고객 메모
-- =============================================================================
CREATE TABLE IF NOT EXISTS customer_notes (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    business_id BIGINT NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    is_private BOOLEAN DEFAULT false,
    author_id BIGINT,
    author_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_customer_notes_customer_id ON customer_notes(customer_id);

COMMENT ON TABLE customer_notes IS '고객 메모';
COMMENT ON COLUMN customer_notes.customer_id IS '고객 ID';
COMMENT ON COLUMN customer_notes.business_id IS '매장 ID';
COMMENT ON COLUMN customer_notes.content IS '메모 내용';
COMMENT ON COLUMN customer_notes.is_private IS '비공개 여부';
COMMENT ON COLUMN customer_notes.author_id IS '작성자 ID';
COMMENT ON COLUMN customer_notes.author_name IS '작성자 이름';

-- =============================================================================
-- 도움말 (인앱 도움말)
-- =============================================================================
CREATE TABLE IF NOT EXISTS help_articles (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    related_feature VARCHAR(100),
    sort_order INTEGER DEFAULT 0,
    lang VARCHAR(10) DEFAULT 'ko',
    is_published BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_help_articles_category ON help_articles(category);

COMMENT ON TABLE help_articles IS '인앱 도움말 콘텐츠';
COMMENT ON COLUMN help_articles.category IS '카테고리 (reservation, staff, service, payment, statistics)';
COMMENT ON COLUMN help_articles.title IS '제목';
COMMENT ON COLUMN help_articles.content IS '본문 (마크다운)';
COMMENT ON COLUMN help_articles.related_feature IS '관련 기능 식별자';
COMMENT ON COLUMN help_articles.sort_order IS '정렬 순서';
COMMENT ON COLUMN help_articles.lang IS '언어 코드 (ko, en)';
COMMENT ON COLUMN help_articles.is_published IS '공개 여부';

