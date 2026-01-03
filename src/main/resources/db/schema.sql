-- 사용자 테이블
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(100) NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       name VARCHAR(50) NOT NULL,
                       phone VARCHAR(20),
                       role VARCHAR(20) NOT NULL DEFAULT 'OWNER',  -- ADMIN, OWNER, STAFF
                       status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, INACTIVE, SUSPENDED
                       staff_id BIGINT,
                       business_id BIGINT,
                       email_verified CHAR(1) DEFAULT 'N',
                       last_login_at TIMESTAMP,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE users IS '사용자 (시스템 관리자, 매장 사장님, 직원)';
COMMENT ON COLUMN users.id IS '사용자 ID';
COMMENT ON COLUMN users.email IS '이메일 (로그인 ID)';
COMMENT ON COLUMN users.password IS '비밀번호 (BCrypt 암호화)';
COMMENT ON COLUMN users.name IS '이름';
COMMENT ON COLUMN users.phone IS '전화번호';
COMMENT ON COLUMN users.role IS '역할 (ADMIN: 시스템관리자, OWNER: 사장님, STAFF: 직원)';
COMMENT ON COLUMN users.status IS '상태 (ACTIVE: 활성, INACTIVE: 휴면, SUSPENDED: 정지)';
COMMENT ON COLUMN users.staff_id IS '연결된 직원 ID (STAFF 역할인 경우)';
COMMENT ON COLUMN users.business_id IS '소속 매장 ID (OWNER/STAFF)';
COMMENT ON COLUMN users.email_verified IS '이메일 인증 여부 (Y/N)';
COMMENT ON COLUMN users.last_login_at IS '마지막 로그인 시각';
COMMENT ON COLUMN users.created_at IS '생성일시';
COMMENT ON COLUMN users.updated_at IS '수정일시';

-- 필수 인덱스만
CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_business_id ON users(business_id);

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

-- 필수 인덱스만
CREATE UNIQUE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- 매장 테이블
-- 3. businesses
CREATE TABLE businesses (
                            id BIGSERIAL PRIMARY KEY,
                            owner_id BIGINT NOT NULL,
                            name VARCHAR(100) NOT NULL,
                            business_type VARCHAR(50) NOT NULL,
                            phone VARCHAR(20),
                            address TEXT,
                            description TEXT,
                            business_hours JSONB,
                            status VARCHAR(20) DEFAULT 'ACTIVE',
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE businesses IS '매장 정보';
COMMENT ON COLUMN businesses.id IS '매장 ID';
COMMENT ON COLUMN businesses.owner_id IS '사장님 사용자 ID';
COMMENT ON COLUMN businesses.name IS '매장명';
COMMENT ON COLUMN businesses.business_type IS '업종 (SALON: 미용실, PILATES: 필라테스, STUDY_CAFE: 스터디카페)';
COMMENT ON COLUMN businesses.phone IS '매장 전화번호';
COMMENT ON COLUMN businesses.address IS '매장 주소';
COMMENT ON COLUMN businesses.description IS '매장 소개';
COMMENT ON COLUMN businesses.business_hours IS '영업시간 (JSON: {mon:{open,close}, tue:...})';
COMMENT ON COLUMN businesses.status IS '상태 (ACTIVE: 영업중, INACTIVE: 휴업, SUSPENDED: 정지)';
COMMENT ON COLUMN businesses.created_at IS '생성일시';
COMMENT ON COLUMN businesses.updated_at IS '수정일시';

-- 필수 인덱스만
CREATE INDEX idx_businesses_owner_id ON businesses(owner_id);
CREATE INDEX idx_businesses_status ON businesses(status);

-- 4. business_settings
CREATE TABLE business_settings (
                                   id BIGSERIAL PRIMARY KEY,
                                   business_id BIGINT NOT NULL,

    -- 예약 설정
                                   booking_interval INTEGER DEFAULT 30,
                                   auto_confirm CHAR(1) DEFAULT 'N',
                                   allow_online_booking CHAR(1) DEFAULT 'Y',
                                   max_advance_booking_days INTEGER DEFAULT 30,
                                   min_advance_booking_hours INTEGER DEFAULT 2,

    -- 알림 설정
                                   send_confirmation_sms CHAR(1) DEFAULT 'Y',
                                   send_reminder_sms CHAR(1) DEFAULT 'Y',
                                   reminder_hours_before INTEGER DEFAULT 24,
                                   send_cancel_sms CHAR(1) DEFAULT 'Y',

    -- 카카오톡 설정
                                   kakao_channel_id VARCHAR(100),
                                   kakao_api_key VARCHAR(200),
                                   kakao_enabled CHAR(1) DEFAULT 'N',

    -- 결제 설정
                                   payment_methods TEXT DEFAULT 'CARD,CASH',
                                   require_deposit CHAR(1) DEFAULT 'N',
                                   deposit_amount INTEGER DEFAULT 0,

    -- 취소 정책
                                   allow_cancellation CHAR(1) DEFAULT 'Y',
                                   cancel_deadline_hours INTEGER DEFAULT 24,
                                   no_show_penalty_enabled CHAR(1) DEFAULT 'N',

    -- 기타
                                   timezone VARCHAR(50) DEFAULT 'Asia/Seoul',
                                   language VARCHAR(10) DEFAULT 'ko',

                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE business_settings IS '매장 예약 시스템 설정';
COMMENT ON COLUMN business_settings.booking_interval IS '예약 시간 간격 (분)';
COMMENT ON COLUMN business_settings.auto_confirm IS '예약 자동 확정 (Y/N)';

CREATE UNIQUE INDEX idx_business_settings_business_id ON business_settings(business_id);



-- 직원 테이블
CREATE TABLE staffs (
                        id BIGSERIAL PRIMARY KEY,
                        business_id BIGINT NOT NULL,
                        name VARCHAR(50) NOT NULL,
                        position VARCHAR(50),
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
COMMENT ON COLUMN staffs.position IS '직급 (원장, 실장, 디자이너 등)';
COMMENT ON COLUMN staffs.phone IS '전화번호';
COMMENT ON COLUMN staffs.email IS '이메일';
COMMENT ON COLUMN staffs.specialty IS '전문분야 (예: 펌, 컬러, 남성컷)';
COMMENT ON COLUMN staffs.career_years IS '경력 (년)';
COMMENT ON COLUMN staffs.profile_image_url IS '프로필 이미지 URL';
COMMENT ON COLUMN staffs.introduction IS '소개글';
COMMENT ON COLUMN staffs.is_active IS '활성 여부 (Y/N)';
COMMENT ON COLUMN staffs.created_at IS '생성일시';
COMMENT ON COLUMN staffs.updated_at IS '수정일시';

-- 필수 인덱스만
CREATE INDEX idx_staffs_business_id ON staffs(business_id);

-- 포트폴리오 테이블
CREATE TABLE portfolios (
                            id BIGSERIAL PRIMARY KEY,
                            staff_id BIGINT NOT NULL,
                            title VARCHAR(100),
                            description TEXT,
                            image_url TEXT NOT NULL,
                            tags TEXT,
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
COMMENT ON COLUMN portfolios.is_visible IS '공개 여부 (Y/N)';
COMMENT ON COLUMN portfolios.created_at IS '생성일시';

-- 필수 인덱스만
CREATE INDEX idx_portfolios_staff_id ON portfolios(staff_id);

-- 서비스 테이블
CREATE TABLE services (
                          id BIGSERIAL PRIMARY KEY,
                          business_id BIGINT NOT NULL,
                          category VARCHAR(50),
                          name VARCHAR(100) NOT NULL,
                          description TEXT,
                          duration INTEGER NOT NULL,
                          price INTEGER NOT NULL,
                          staff_ids TEXT,
                          is_active CHAR(1) DEFAULT 'Y',
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE services IS '서비스 메뉴 (시술/수업)';
COMMENT ON COLUMN services.id IS '서비스 ID';
COMMENT ON COLUMN services.business_id IS '매장 ID';
COMMENT ON COLUMN services.category IS '카테고리 (컷, 펌, 염색 등)';
COMMENT ON COLUMN services.name IS '서비스명';
COMMENT ON COLUMN services.description IS '설명';
COMMENT ON COLUMN services.duration IS '소요시간 (분)';
COMMENT ON COLUMN services.price IS '가격 (원)';
COMMENT ON COLUMN services.staff_ids IS '담당 가능 직원 ID 목록 (콤마 구분)';
COMMENT ON COLUMN services.is_active IS '활성 여부 (Y/N)';
COMMENT ON COLUMN services.created_at IS '생성일시';
COMMENT ON COLUMN services.updated_at IS '수정일시';

-- 필수 인덱스만
CREATE INDEX idx_services_business_id ON services(business_id);

-- 특별 휴무일 테이블
CREATE TABLE special_holidays (
                                  id BIGSERIAL PRIMARY KEY,
                                  business_id BIGINT NOT NULL,
                                  holiday_date DATE NOT NULL,
                                  reason VARCHAR(100),
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE special_holidays IS '특별 휴무일';
COMMENT ON COLUMN special_holidays.id IS '휴무일 ID';
COMMENT ON COLUMN special_holidays.business_id IS '매장 ID';
COMMENT ON COLUMN special_holidays.holiday_date IS '휴무 날짜';
COMMENT ON COLUMN special_holidays.reason IS '휴무 사유';
COMMENT ON COLUMN special_holidays.created_at IS '생성일시';

-- 필수 인덱스만
CREATE INDEX idx_special_holidays_business_date ON special_holidays(business_id, holiday_date);

-- 고객 테이블
CREATE TABLE customers (
                           id BIGSERIAL PRIMARY KEY,
                           business_id BIGINT NOT NULL,
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
COMMENT ON COLUMN customers.name IS '이름';
COMMENT ON COLUMN customers.phone IS '전화번호';
COMMENT ON COLUMN customers.email IS '이메일';
COMMENT ON COLUMN customers.birth_date IS '생년월일';
COMMENT ON COLUMN customers.gender IS '성별 (MALE/FEMALE/OTHER)';
COMMENT ON COLUMN customers.visit_count IS '방문 횟수';
COMMENT ON COLUMN customers.total_spent IS '총 결제 금액 (원)';
COMMENT ON COLUMN customers.last_visit_date IS '마지막 방문일';
COMMENT ON COLUMN customers.tags IS '태그 (VIP, 단골, 신규 등 - 콤마 구분)';
COMMENT ON COLUMN customers.memo IS '메모';
COMMENT ON COLUMN customers.created_at IS '생성일시';
COMMENT ON COLUMN customers.updated_at IS '수정일시';

-- 필수 인덱스만
CREATE INDEX idx_customers_business_id ON customers(business_id);
CREATE INDEX idx_customers_phone ON customers(business_id, phone);

-- 예약 테이블
CREATE TABLE reservations (
                              id BIGSERIAL PRIMARY KEY,
                              business_id BIGINT NOT NULL,
                              customer_id BIGINT NOT NULL,
                              staff_id BIGINT NOT NULL,
                              reservation_date DATE NOT NULL,
                              start_time TIME NOT NULL,
                              end_time TIME NOT NULL,
                              services JSONB NOT NULL,
                              total_duration INTEGER NOT NULL,
                              total_price INTEGER NOT NULL,
                              status VARCHAR(20) DEFAULT 'PENDING',  -- PENDING, CONFIRMED, COMPLETED, CANCELLED, NO_SHOW
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
COMMENT ON COLUMN reservations.staff_id IS '담당 직원 ID';
COMMENT ON COLUMN reservations.reservation_date IS '예약 날짜';
COMMENT ON COLUMN reservations.start_time IS '시작 시각';
COMMENT ON COLUMN reservations.end_time IS '종료 시각';
COMMENT ON COLUMN reservations.services IS '서비스 목록 (JSON: [{id, name, price, duration}])';
COMMENT ON COLUMN reservations.total_duration IS '총 소요시간 (분)';
COMMENT ON COLUMN reservations.total_price IS '총 가격 (원)';
COMMENT ON COLUMN reservations.status IS '상태 (PENDING: 대기, CONFIRMED: 확정, COMPLETED: 완료, CANCELLED: 취소, NO_SHOW: 노쇼)';
COMMENT ON COLUMN reservations.reservation_number IS '예약 번호';
COMMENT ON COLUMN reservations.customer_memo IS '고객 요청사항';
COMMENT ON COLUMN reservations.staff_memo IS '직원 메모';
COMMENT ON COLUMN reservations.cancelled_at IS '취소 시각';
COMMENT ON COLUMN reservations.cancel_reason IS '취소 사유';
COMMENT ON COLUMN reservations.created_at IS '생성일시';
COMMENT ON COLUMN reservations.updated_at IS '수정일시';

-- 필수 인덱스만
CREATE UNIQUE INDEX idx_reservations_number ON reservations(reservation_number);
CREATE INDEX idx_reservations_business_date ON reservations(business_id, reservation_date);
CREATE INDEX idx_reservations_customer_id ON reservations(customer_id);
CREATE INDEX idx_reservations_staff_id ON reservations(staff_id);

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
COMMENT ON COLUMN customer_histories.services IS '서비스 목록 (JSON)';
COMMENT ON COLUMN customer_histories.total_price IS '총 결제 금액 (원)';
COMMENT ON COLUMN customer_histories.payment_method IS '결제 수단 (CARD/CASH/TRANSFER 등)';
COMMENT ON COLUMN customer_histories.details IS '상세 정보 (JSON - 미용실: 사용약품, 시술내용 등)';
COMMENT ON COLUMN customer_histories.before_image_url IS '시술 전 이미지 URL';
COMMENT ON COLUMN customer_histories.after_image_url IS '시술 후 이미지 URL';
COMMENT ON COLUMN customer_histories.created_at IS '생성일시';

-- 필수 인덱스만
CREATE INDEX idx_customer_histories_customer_id ON customer_histories(customer_id);
CREATE INDEX idx_customer_histories_business_date ON customer_histories(business_id, visit_date);