-- public.audit_logs definition

-- Drop table

-- DROP TABLE audit_logs;

CREATE TABLE audit_logs (
                            id bigserial NOT NULL, -- 로그 ID
                            user_id int8 NULL, -- 액션 수행 사용자 ID
                            user_email varchar(100) NULL, -- 액션 수행 사용자 이메일
                            "user_role" varchar(20) NULL, -- 액션 수행 사용자 역할
                            "action" varchar(50) NOT NULL, -- 액션 타입 (BUSINESS_CREATED, USER_ROLE_CHANGED 등)
                            entity_type varchar(50) NULL, -- 대상 엔티티 타입 (Business, User, Reservation 등)
                            entity_id int8 NULL, -- 대상 엔티티 ID
                            description text NULL, -- 액션 설명
                            metadata jsonb NULL, -- 추가 정보 (JSON - 변경 전/후 값 등)
                            ip_address varchar(50) NULL, -- IP 주소
                            user_agent text NULL, -- User-Agent
                            created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 생성일시
                            CONSTRAINT audit_logs_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_audit_logs_action ON public.audit_logs USING btree (action);
CREATE INDEX idx_audit_logs_created_at ON public.audit_logs USING btree (created_at);
CREATE INDEX idx_audit_logs_entity ON public.audit_logs USING btree (entity_type, entity_id);
CREATE INDEX idx_audit_logs_user_id ON public.audit_logs USING btree (user_id);
COMMENT ON TABLE public.audit_logs IS '감사 로그 (시스템 중요 액션 기록)';

-- Column comments

COMMENT ON COLUMN public.audit_logs.id IS '로그 ID';
COMMENT ON COLUMN public.audit_logs.user_id IS '액션 수행 사용자 ID';
COMMENT ON COLUMN public.audit_logs.user_email IS '액션 수행 사용자 이메일';
COMMENT ON COLUMN public.audit_logs."user_role" IS '액션 수행 사용자 역할';
COMMENT ON COLUMN public.audit_logs."action" IS '액션 타입 (BUSINESS_CREATED, USER_ROLE_CHANGED 등)';
COMMENT ON COLUMN public.audit_logs.entity_type IS '대상 엔티티 타입 (Business, User, Reservation 등)';
COMMENT ON COLUMN public.audit_logs.entity_id IS '대상 엔티티 ID';
COMMENT ON COLUMN public.audit_logs.description IS '액션 설명';
COMMENT ON COLUMN public.audit_logs.metadata IS '추가 정보 (JSON - 변경 전/후 값 등)';
COMMENT ON COLUMN public.audit_logs.ip_address IS 'IP 주소';
COMMENT ON COLUMN public.audit_logs.user_agent IS 'User-Agent';
COMMENT ON COLUMN public.audit_logs.created_at IS '생성일시';


-- public.business_settings definition

-- Drop table

-- DROP TABLE business_settings;

CREATE TABLE business_settings (
                                   id bigserial NOT NULL,
                                   business_id int8 NOT NULL,
                                   booking_interval int4 DEFAULT 30 NULL, -- 예약 시간 간격 (분)
                                   auto_confirm bpchar(1) DEFAULT 'N'::bpchar NULL, -- 예약 자동 확정 (Y/N)
                                   allow_online_booking bpchar(1) DEFAULT 'Y'::bpchar NULL,
                                   max_advance_booking_days int4 DEFAULT 30 NULL,
                                   min_advance_booking_hours int4 DEFAULT 2 NULL,
                                   send_confirmation_sms bpchar(1) DEFAULT 'Y'::bpchar NULL,
                                   send_reminder_sms bpchar(1) DEFAULT 'Y'::bpchar NULL,
                                   reminder_hours_before int4 DEFAULT 24 NULL,
                                   send_cancel_sms bpchar(1) DEFAULT 'Y'::bpchar NULL,
                                   kakao_channel_id varchar(100) NULL,
                                   kakao_api_key varchar(200) NULL,
                                   kakao_enabled bpchar(1) DEFAULT 'N'::bpchar NULL,
                                   payment_methods text DEFAULT 'CARD,CASH'::text NULL,
                                   require_deposit bpchar(1) DEFAULT 'N'::bpchar NULL,
                                   deposit_amount int4 DEFAULT 0 NULL,
                                   allow_cancellation bpchar(1) DEFAULT 'Y'::bpchar NULL,
                                   cancel_deadline_hours int4 DEFAULT 24 NULL,
                                   no_show_penalty_enabled bpchar(1) DEFAULT 'N'::bpchar NULL,
                                   timezone varchar(50) DEFAULT 'Asia/Seoul'::character varying NULL,
                                   "language" varchar(10) DEFAULT 'ko'::character varying NULL,
                                   created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                                   updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                                   CONSTRAINT business_settings_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX idx_business_settings_business_id ON public.business_settings USING btree (business_id);
COMMENT ON TABLE public.business_settings IS '매장 예약 시스템 설정';

-- Column comments

COMMENT ON COLUMN public.business_settings.booking_interval IS '예약 시간 간격 (분)';
COMMENT ON COLUMN public.business_settings.auto_confirm IS '예약 자동 확정 (Y/N)';


-- public.businesses definition

-- Drop table

-- DROP TABLE businesses;

CREATE TABLE businesses (
                            id bigserial NOT NULL, -- 매장 ID
                            owner_id int8 NOT NULL, -- 사장님 사용자 ID
                            "name" varchar(100) NOT NULL, -- 매장명
                            "business_type" varchar(50) NOT NULL, -- 업종 (SALON: 미용실, PILATES: 필라테스, STUDY_CAFE: 스터디카페)
                            phone varchar(20) NULL, -- 매장 전화번호
                            address text NULL, -- 매장 주소
                            description text NULL, -- 매장 소개
                            business_hours jsonb NULL, -- 영업시간 (JSON: {mon:{open,close}, tue:...})
                            status varchar(20) DEFAULT 'ACTIVE'::character varying NULL, -- 상태 (ACTIVE: 영업중, INACTIVE: 휴업, SUSPENDED: 정지)
                            created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 생성일시
                            updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 수정일시
                            daily_revenue_goal int4 NULL, -- 일일 매출 목표 (원)
                            monthly_revenue_goal int4 NULL, -- 월간 매출 목표 (원)
                            monthly_new_customer_goal int4 NULL, -- 월간 신규 고객 목표 (명)
                            subscription_plan varchar(20) DEFAULT 'FREE'::character varying NULL, -- 구독 플랜: FREE, BASIC, PRO, ENTERPRISE
                            subscription_status varchar(20) DEFAULT 'TRIAL'::character varying NULL, -- 구독 상태: TRIAL, ACTIVE, EXPIRED, CANCELED, SUSPENDED
                            trial_ends_at timestamp NULL, -- 무료 체험 종료일 (30일)
                            trial_started_at timestamp NULL, -- 무료 체험 시작일
                            subscription_started_at timestamp NULL, -- 유료 구독 시작일
                            next_billing_date timestamp NULL, -- 다음 결제 예정일
                            current_staff_count int4 DEFAULT 0 NOT NULL, -- 현재 활성 직원 수 (플랜 제한 체크용)
                            current_month_reservation_count int4 DEFAULT 0 NOT NULL, -- 이번 달 예약 수 (플랜 제한 체크용)
                            CONSTRAINT businesses_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_businesses_owner_id ON public.businesses USING btree (owner_id);
CREATE INDEX idx_businesses_status ON public.businesses USING btree (status);
CREATE INDEX idx_businesses_subscription_status ON public.businesses USING btree (subscription_status);
CREATE INDEX idx_businesses_trial_ends_at ON public.businesses USING btree (trial_ends_at);
COMMENT ON TABLE public.businesses IS '매장 정보';

-- Column comments

COMMENT ON COLUMN public.businesses.id IS '매장 ID';
COMMENT ON COLUMN public.businesses.owner_id IS '사장님 사용자 ID';
COMMENT ON COLUMN public.businesses."name" IS '매장명';
COMMENT ON COLUMN public.businesses."business_type" IS '업종 (SALON: 미용실, PILATES: 필라테스, STUDY_CAFE: 스터디카페)';
COMMENT ON COLUMN public.businesses.phone IS '매장 전화번호';
COMMENT ON COLUMN public.businesses.address IS '매장 주소';
COMMENT ON COLUMN public.businesses.description IS '매장 소개';
COMMENT ON COLUMN public.businesses.business_hours IS '영업시간 (JSON: {mon:{open,close}, tue:...})';
COMMENT ON COLUMN public.businesses.status IS '상태 (ACTIVE: 영업중, INACTIVE: 휴업, SUSPENDED: 정지)';
COMMENT ON COLUMN public.businesses.created_at IS '생성일시';
COMMENT ON COLUMN public.businesses.updated_at IS '수정일시';
COMMENT ON COLUMN public.businesses.daily_revenue_goal IS '일일 매출 목표 (원)';
COMMENT ON COLUMN public.businesses.monthly_revenue_goal IS '월간 매출 목표 (원)';
COMMENT ON COLUMN public.businesses.monthly_new_customer_goal IS '월간 신규 고객 목표 (명)';
COMMENT ON COLUMN public.businesses.subscription_plan IS '구독 플랜: FREE, BASIC, PRO, ENTERPRISE';
COMMENT ON COLUMN public.businesses.subscription_status IS '구독 상태: TRIAL, ACTIVE, EXPIRED, CANCELED, SUSPENDED';
COMMENT ON COLUMN public.businesses.trial_ends_at IS '무료 체험 종료일 (30일)';
COMMENT ON COLUMN public.businesses.trial_started_at IS '무료 체험 시작일';
COMMENT ON COLUMN public.businesses.subscription_started_at IS '유료 구독 시작일';
COMMENT ON COLUMN public.businesses.next_billing_date IS '다음 결제 예정일';
COMMENT ON COLUMN public.businesses.current_staff_count IS '현재 활성 직원 수 (플랜 제한 체크용)';
COMMENT ON COLUMN public.businesses.current_month_reservation_count IS '이번 달 예약 수 (플랜 제한 체크용)';


-- public.customer_histories definition

-- Drop table

-- DROP TABLE customer_histories;

CREATE TABLE customer_histories (
                                    id bigserial NOT NULL, -- 이력 ID
                                    business_id int8 NOT NULL, -- 매장 ID
                                    customer_id int8 NOT NULL, -- 고객 ID
                                    staff_id int8 NOT NULL, -- 담당 직원 ID
                                    reservation_id int8 NULL, -- 연결된 예약 ID (자동 생성 시)
                                    visit_date date NOT NULL, -- 방문일
                                    services jsonb NOT NULL, -- 서비스 목록 (JSON)
                                    total_price int4 NOT NULL, -- 총 결제 금액 (원)
                                    "payment_method" varchar(20) NULL, -- 결제 수단 (CARD/CASH/TRANSFER 등)
                                    details jsonb NULL, -- 상세 정보 (JSON - 미용실: 사용약품, 시술내용 등)
                                    before_image_url text NULL, -- 시술 전 이미지 URL
                                    after_image_url text NULL, -- 시술 후 이미지 URL
                                    created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 생성일시
                                    CONSTRAINT customer_histories_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_customer_histories_business_date ON public.customer_histories USING btree (business_id, visit_date);
CREATE INDEX idx_customer_histories_customer_id ON public.customer_histories USING btree (customer_id);
COMMENT ON TABLE public.customer_histories IS '고객 시술 이력';

-- Column comments

COMMENT ON COLUMN public.customer_histories.id IS '이력 ID';
COMMENT ON COLUMN public.customer_histories.business_id IS '매장 ID';
COMMENT ON COLUMN public.customer_histories.customer_id IS '고객 ID';
COMMENT ON COLUMN public.customer_histories.staff_id IS '담당 직원 ID';
COMMENT ON COLUMN public.customer_histories.reservation_id IS '연결된 예약 ID (자동 생성 시)';
COMMENT ON COLUMN public.customer_histories.visit_date IS '방문일';
COMMENT ON COLUMN public.customer_histories.services IS '서비스 목록 (JSON)';
COMMENT ON COLUMN public.customer_histories.total_price IS '총 결제 금액 (원)';
COMMENT ON COLUMN public.customer_histories."payment_method" IS '결제 수단 (CARD/CASH/TRANSFER 등)';
COMMENT ON COLUMN public.customer_histories.details IS '상세 정보 (JSON - 미용실: 사용약품, 시술내용 등)';
COMMENT ON COLUMN public.customer_histories.before_image_url IS '시술 전 이미지 URL';
COMMENT ON COLUMN public.customer_histories.after_image_url IS '시술 후 이미지 URL';
COMMENT ON COLUMN public.customer_histories.created_at IS '생성일시';


-- public.customers definition

-- Drop table

-- DROP TABLE customers;

CREATE TABLE customers (
                           id bigserial NOT NULL, -- 고객 ID
                           business_id int8 NOT NULL, -- 매장 ID
                           "name" varchar(50) NOT NULL, -- 이름
                           phone varchar(20) NOT NULL, -- 전화번호
                           email varchar(100) NULL, -- 이메일
                           birth_date date NULL, -- 생년월일
                           gender varchar(10) NULL, -- 성별 (MALE/FEMALE/OTHER)
                           visit_count int4 DEFAULT 0 NULL, -- 방문 횟수
                           total_spent int4 DEFAULT 0 NULL, -- 총 결제 금액 (원)
                           last_visit_date date NULL, -- 마지막 방문일
                           tags text NULL, -- 태그 (VIP, 단골, 신규 등 - 콤마 구분)
                           memo text NULL, -- 메모
                           created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 생성일시
                           updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 수정일시
                           CONSTRAINT customers_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_customers_business_id ON public.customers USING btree (business_id);
CREATE INDEX idx_customers_phone ON public.customers USING btree (business_id, phone);
COMMENT ON TABLE public.customers IS '고객';

-- Column comments

COMMENT ON COLUMN public.customers.id IS '고객 ID';
COMMENT ON COLUMN public.customers.business_id IS '매장 ID';
COMMENT ON COLUMN public.customers."name" IS '이름';
COMMENT ON COLUMN public.customers.phone IS '전화번호';
COMMENT ON COLUMN public.customers.email IS '이메일';
COMMENT ON COLUMN public.customers.birth_date IS '생년월일';
COMMENT ON COLUMN public.customers.gender IS '성별 (MALE/FEMALE/OTHER)';
COMMENT ON COLUMN public.customers.visit_count IS '방문 횟수';
COMMENT ON COLUMN public.customers.total_spent IS '총 결제 금액 (원)';
COMMENT ON COLUMN public.customers.last_visit_date IS '마지막 방문일';
COMMENT ON COLUMN public.customers.tags IS '태그 (VIP, 단골, 신규 등 - 콤마 구분)';
COMMENT ON COLUMN public.customers.memo IS '메모';
COMMENT ON COLUMN public.customers.created_at IS '생성일시';
COMMENT ON COLUMN public.customers.updated_at IS '수정일시';


-- public.portfolios definition

-- Drop table

-- DROP TABLE portfolios;

CREATE TABLE portfolios (
                            id bigserial NOT NULL, -- 포트폴리오 ID
                            staff_id int8 NOT NULL, -- 직원 ID
                            title varchar(100) NULL, -- 제목
                            description text NULL, -- 설명
                            image_url text NOT NULL, -- 이미지 URL
                            tags text NULL, -- 태그 (콤마 구분)
                            is_visible bpchar(1) DEFAULT 'Y'::bpchar NULL, -- 공개 여부 (Y/N)
                            created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 생성일시
                            CONSTRAINT portfolios_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_portfolios_staff_id ON public.portfolios USING btree (staff_id);
COMMENT ON TABLE public.portfolios IS '직원 포트폴리오';

-- Column comments

COMMENT ON COLUMN public.portfolios.id IS '포트폴리오 ID';
COMMENT ON COLUMN public.portfolios.staff_id IS '직원 ID';
COMMENT ON COLUMN public.portfolios.title IS '제목';
COMMENT ON COLUMN public.portfolios.description IS '설명';
COMMENT ON COLUMN public.portfolios.image_url IS '이미지 URL';
COMMENT ON COLUMN public.portfolios.tags IS '태그 (콤마 구분)';
COMMENT ON COLUMN public.portfolios.is_visible IS '공개 여부 (Y/N)';
COMMENT ON COLUMN public.portfolios.created_at IS '생성일시';


-- public.refresh_tokens definition

-- Drop table

-- DROP TABLE refresh_tokens;

CREATE TABLE refresh_tokens (
                                id bigserial NOT NULL, -- 토큰 ID
                                user_id int8 NOT NULL, -- 사용자 ID
                                "token" varchar(500) NOT NULL, -- 리프레시 토큰 문자열
                                expires_at timestamp NOT NULL, -- 만료 시각
                                created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 생성일시
                                CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id)
);
CREATE UNIQUE INDEX idx_refresh_tokens_token ON public.refresh_tokens USING btree (token);
CREATE INDEX idx_refresh_tokens_user_id ON public.refresh_tokens USING btree (user_id);
COMMENT ON TABLE public.refresh_tokens IS 'JWT 리프레시 토큰';

-- Column comments

COMMENT ON COLUMN public.refresh_tokens.id IS '토큰 ID';
COMMENT ON COLUMN public.refresh_tokens.user_id IS '사용자 ID';
COMMENT ON COLUMN public.refresh_tokens."token" IS '리프레시 토큰 문자열';
COMMENT ON COLUMN public.refresh_tokens.expires_at IS '만료 시각';
COMMENT ON COLUMN public.refresh_tokens.created_at IS '생성일시';


-- public.reservations definition

-- Drop table

-- DROP TABLE reservations;

CREATE TABLE reservations (
                              id bigserial NOT NULL, -- 예약 ID
                              business_id int8 NOT NULL, -- 매장 ID
                              customer_id int8 NOT NULL, -- 고객 ID
                              staff_id int8 NULL, -- 담당 직원 ID
                              reservation_date date NOT NULL, -- 예약 날짜
                              start_time time NOT NULL, -- 시작 시각
                              end_time time NOT NULL, -- 종료 시각
                              services jsonb NOT NULL, -- 서비스 목록 (JSON: [{id, name, price, duration}])
                              total_duration int4 NOT NULL, -- 총 소요시간 (분)
                              total_price int4 NOT NULL, -- 총 가격 (원)
                              status varchar(20) DEFAULT 'PENDING'::character varying NULL, -- 상태 (PENDING: 대기, CONFIRMED: 확정, COMPLETED: 완료, CANCELLED: 취소, NO_SHOW: 노쇼)
                              reservation_number varchar(50) NOT NULL, -- 예약 번호
                              customer_memo text NULL, -- 고객 요청사항
                              staff_memo text NULL, -- 직원 메모
                              cancelled_at timestamp NULL, -- 취소 시각
                              cancel_reason text NULL, -- 취소 사유
                              created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 생성일시
                              updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 수정일시
                              CONSTRAINT reservations_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_reservations_business_date ON public.reservations USING btree (business_id, reservation_date);
CREATE INDEX idx_reservations_customer_id ON public.reservations USING btree (customer_id);
CREATE UNIQUE INDEX idx_reservations_number ON public.reservations USING btree (reservation_number);
CREATE INDEX idx_reservations_staff_id ON public.reservations USING btree (staff_id);
COMMENT ON TABLE public.reservations IS '예약';

-- Column comments

COMMENT ON COLUMN public.reservations.id IS '예약 ID';
COMMENT ON COLUMN public.reservations.business_id IS '매장 ID';
COMMENT ON COLUMN public.reservations.customer_id IS '고객 ID';
COMMENT ON COLUMN public.reservations.staff_id IS '담당 직원 ID';
COMMENT ON COLUMN public.reservations.reservation_date IS '예약 날짜';
COMMENT ON COLUMN public.reservations.start_time IS '시작 시각';
COMMENT ON COLUMN public.reservations.end_time IS '종료 시각';
COMMENT ON COLUMN public.reservations.services IS '서비스 목록 (JSON: [{id, name, price, duration}])';
COMMENT ON COLUMN public.reservations.total_duration IS '총 소요시간 (분)';
COMMENT ON COLUMN public.reservations.total_price IS '총 가격 (원)';
COMMENT ON COLUMN public.reservations.status IS '상태 (PENDING: 대기, CONFIRMED: 확정, COMPLETED: 완료, CANCELLED: 취소, NO_SHOW: 노쇼)';
COMMENT ON COLUMN public.reservations.reservation_number IS '예약 번호';
COMMENT ON COLUMN public.reservations.customer_memo IS '고객 요청사항';
COMMENT ON COLUMN public.reservations.staff_memo IS '직원 메모';
COMMENT ON COLUMN public.reservations.cancelled_at IS '취소 시각';
COMMENT ON COLUMN public.reservations.cancel_reason IS '취소 사유';
COMMENT ON COLUMN public.reservations.created_at IS '생성일시';
COMMENT ON COLUMN public.reservations.updated_at IS '수정일시';


-- public.service_categories definition

-- Drop table

-- DROP TABLE service_categories;

CREATE TABLE service_categories (
                                    id bigserial NOT NULL, -- 카테고리 ID
                                    business_id int8 NOT NULL, -- 매장 ID
                                    "name" varchar(50) NOT NULL, -- 카테고리명
                                    description varchar(200) NULL, -- 설명
                                    sort_order int4 DEFAULT 0 NOT NULL, -- 정렬 순서
                                    created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 생성일시
                                    updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 수정일시
                                    CONSTRAINT service_categories_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_service_categories_business_id ON public.service_categories USING btree (business_id);
CREATE UNIQUE INDEX idx_service_categories_business_name ON public.service_categories USING btree (business_id, name);
COMMENT ON TABLE public.service_categories IS '서비스 카테고리';

-- Column comments

COMMENT ON COLUMN public.service_categories.id IS '카테고리 ID';
COMMENT ON COLUMN public.service_categories.business_id IS '매장 ID';
COMMENT ON COLUMN public.service_categories."name" IS '카테고리명';
COMMENT ON COLUMN public.service_categories.description IS '설명';
COMMENT ON COLUMN public.service_categories.sort_order IS '정렬 순서';
COMMENT ON COLUMN public.service_categories.created_at IS '생성일시';
COMMENT ON COLUMN public.service_categories.updated_at IS '수정일시';


-- public.services definition

-- Drop table

-- DROP TABLE services;

CREATE TABLE services (
                          id bigserial NOT NULL, -- 서비스 ID
                          business_id int8 NOT NULL, -- 매장 ID
                          category_id int8 NULL, -- 카테고리 ID (service_categories.id)
                          "name" varchar(100) NOT NULL, -- 서비스명
                          description text NULL, -- 설명
                          duration int4 NOT NULL, -- 소요시간 (분)
                          price int4 NOT NULL, -- 가격 (원)
                          staff_ids text NULL, -- 담당 가능 직원 ID 목록 (콤마 구분)
                          sort_order int4 DEFAULT 0 NOT NULL, -- 정렬 순서
                          is_active bpchar(1) DEFAULT 'Y'::bpchar NULL, -- 활성 여부 (Y/N)
                          created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 생성일시
                          updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 수정일시
                          CONSTRAINT services_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_services_business_id ON public.services USING btree (business_id);
CREATE INDEX idx_services_category_id ON public.services USING btree (category_id);
COMMENT ON TABLE public.services IS '서비스 메뉴 (시술/수업)';

-- Column comments

COMMENT ON COLUMN public.services.id IS '서비스 ID';
COMMENT ON COLUMN public.services.business_id IS '매장 ID';
COMMENT ON COLUMN public.services.category_id IS '카테고리 ID (service_categories.id)';
COMMENT ON COLUMN public.services."name" IS '서비스명';
COMMENT ON COLUMN public.services.description IS '설명';
COMMENT ON COLUMN public.services.duration IS '소요시간 (분)';
COMMENT ON COLUMN public.services.price IS '가격 (원)';
COMMENT ON COLUMN public.services.staff_ids IS '담당 가능 직원 ID 목록 (콤마 구분)';
COMMENT ON COLUMN public.services.sort_order IS '정렬 순서';
COMMENT ON COLUMN public.services.is_active IS '활성 여부 (Y/N)';
COMMENT ON COLUMN public.services.created_at IS '생성일시';
COMMENT ON COLUMN public.services.updated_at IS '수정일시';


-- public.special_holidays definition

-- Drop table

-- DROP TABLE special_holidays;

CREATE TABLE special_holidays (
                                  id bigserial NOT NULL, -- 휴무일 ID
                                  business_id int8 NOT NULL, -- 매장 ID
                                  "date" date NOT NULL, -- 휴무 날짜
                                  reason varchar(100) NULL, -- 휴무 사유
                                  created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 생성일시
                                  "name" varchar(100) NULL,
                                  "type" varchar(20) DEFAULT 'REGULAR'::character varying NULL,
                                  CONSTRAINT special_holidays_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_special_holidays_business_date ON public.special_holidays USING btree (business_id, date);
COMMENT ON TABLE public.special_holidays IS '특별 휴무일';

-- Column comments

COMMENT ON COLUMN public.special_holidays.id IS '휴무일 ID';
COMMENT ON COLUMN public.special_holidays.business_id IS '매장 ID';
COMMENT ON COLUMN public.special_holidays."date" IS '휴무 날짜';
COMMENT ON COLUMN public.special_holidays.reason IS '휴무 사유';
COMMENT ON COLUMN public.special_holidays.created_at IS '생성일시';


-- public.staffs definition

-- Drop table

-- DROP TABLE staffs;

CREATE TABLE staffs (
                        id bigserial NOT NULL, -- 직원 ID
                        business_id int8 NOT NULL, -- 소속 매장 ID
                        "name" varchar(50) NOT NULL, -- 이름
                        "position" varchar(50) NULL, -- 직급 (원장, 실장, 디자이너 등)
                        phone varchar(20) NULL, -- 전화번호
                        email varchar(100) NULL, -- 이메일
                        specialty text NULL, -- 전문분야 (예: 펌, 컬러, 남성컷)
                        career_years int4 DEFAULT 0 NULL, -- 경력 (년)
                        profile_image_url text NULL, -- 프로필 이미지 URL
                        introduction text NULL, -- 소개글
                        is_active bpchar(1) DEFAULT 'Y'::bpchar NULL, -- 활성 여부 (Y/N)
                        created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 생성일시
                        updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 수정일시
                        CONSTRAINT staffs_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_staffs_business_id ON public.staffs USING btree (business_id);
COMMENT ON TABLE public.staffs IS '직원 (디자이너/강사)';

-- Column comments

COMMENT ON COLUMN public.staffs.id IS '직원 ID';
COMMENT ON COLUMN public.staffs.business_id IS '소속 매장 ID';
COMMENT ON COLUMN public.staffs."name" IS '이름';
COMMENT ON COLUMN public.staffs."position" IS '직급 (원장, 실장, 디자이너 등)';
COMMENT ON COLUMN public.staffs.phone IS '전화번호';
COMMENT ON COLUMN public.staffs.email IS '이메일';
COMMENT ON COLUMN public.staffs.specialty IS '전문분야 (예: 펌, 컬러, 남성컷)';
COMMENT ON COLUMN public.staffs.career_years IS '경력 (년)';
COMMENT ON COLUMN public.staffs.profile_image_url IS '프로필 이미지 URL';
COMMENT ON COLUMN public.staffs.introduction IS '소개글';
COMMENT ON COLUMN public.staffs.is_active IS '활성 여부 (Y/N)';
COMMENT ON COLUMN public.staffs.created_at IS '생성일시';
COMMENT ON COLUMN public.staffs.updated_at IS '수정일시';


-- public.users definition

-- Drop table

-- DROP TABLE users;

CREATE TABLE users (
                       id bigserial NOT NULL, -- 사용자 ID
                       email varchar(100) NOT NULL, -- 이메일 (로그인 ID)
                       "password" varchar(255) NOT NULL, -- 비밀번호 (BCrypt 암호화)
                       "name" varchar(50) NOT NULL, -- 이름
                       phone varchar(20) NULL, -- 전화번호
                       "role" varchar(20) DEFAULT 'OWNER'::character varying NOT NULL, -- 역할 (ADMIN: 시스템관리자, OWNER: 사장님, STAFF: 직원)
                       status varchar(20) DEFAULT 'ACTIVE'::character varying NOT NULL, -- 상태 (ACTIVE: 활성, INACTIVE: 휴면, SUSPENDED: 정지)
                       staff_id int8 NULL, -- 연결된 직원 ID (STAFF 역할인 경우)
                       business_id int8 NULL, -- 소속 매장 ID (OWNER/STAFF)
                       email_verified bpchar(1) DEFAULT 'N'::bpchar NULL, -- 이메일 인증 여부 (Y/N)
                       last_login_at timestamp NULL, -- 마지막 로그인 시각
                       created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 생성일시
                       updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 수정일시
                       trial_started_at timestamp NULL,
                       trial_expires_at timestamp NULL,
                       is_premium bpchar(1) DEFAULT 'N'::bpchar NULL,
                       CONSTRAINT users_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_users_business_id ON public.users USING btree (business_id);
CREATE UNIQUE INDEX idx_users_email ON public.users USING btree (email);
CREATE INDEX idx_users_trial_expires ON public.users USING btree (trial_expires_at) WHERE (is_premium = 'N'::bpchar);
COMMENT ON TABLE public.users IS '사용자 (시스템 관리자, 매장 사장님, 직원)';

-- Column comments

COMMENT ON COLUMN public.users.id IS '사용자 ID';
COMMENT ON COLUMN public.users.email IS '이메일 (로그인 ID)';
COMMENT ON COLUMN public.users."password" IS '비밀번호 (BCrypt 암호화)';
COMMENT ON COLUMN public.users."name" IS '이름';
COMMENT ON COLUMN public.users.phone IS '전화번호';
COMMENT ON COLUMN public.users."role" IS '역할 (ADMIN: 시스템관리자, OWNER: 사장님, STAFF: 직원)';
COMMENT ON COLUMN public.users.status IS '상태 (ACTIVE: 활성, INACTIVE: 휴면, SUSPENDED: 정지)';
COMMENT ON COLUMN public.users.staff_id IS '연결된 직원 ID (STAFF 역할인 경우)';
COMMENT ON COLUMN public.users.business_id IS '소속 매장 ID (OWNER/STAFF)';
COMMENT ON COLUMN public.users.email_verified IS '이메일 인증 여부 (Y/N)';
COMMENT ON COLUMN public.users.last_login_at IS '마지막 로그인 시각';
COMMENT ON COLUMN public.users.created_at IS '생성일시';
COMMENT ON COLUMN public.users.updated_at IS '수정일시';


-- public.business_coupons definition

-- Drop table

-- DROP TABLE business_coupons;

CREATE TABLE business_coupons (
                                  id bigserial NOT NULL,
                                  business_id int8 NOT NULL,
                                  code varchar(50) NOT NULL,
                                  "name" varchar(100) NOT NULL,
                                  description text NULL,
                                  "coupon_type" public."coupon_type" NOT NULL, -- PERCENTAGE(정률), FIXED_AMOUNT(정액)
                                  discount_amount int4 NULL,
                                  discount_percentage int4 NULL,
                                  max_discount_amount int4 NULL,
                                  min_order_amount int4 DEFAULT 0 NULL,
                                  max_usage_count int4 NULL,
                                  current_usage_count int4 DEFAULT 0 NULL,
                                  valid_from timestamp NOT NULL,
                                  valid_until timestamp NOT NULL,
                                  status public."coupon_status" DEFAULT 'ACTIVE'::coupon_status NOT NULL,
                                  created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                                  updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                                  CONSTRAINT business_coupons_code_key UNIQUE (code),
                                  CONSTRAINT business_coupons_pkey PRIMARY KEY (id),
                                  CONSTRAINT business_coupons_business_id_fkey FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE
);
CREATE INDEX idx_business_coupons_business_id ON public.business_coupons USING btree (business_id);
CREATE INDEX idx_business_coupons_code ON public.business_coupons USING btree (code);
CREATE INDEX idx_business_coupons_status ON public.business_coupons USING btree (status);
CREATE INDEX idx_business_coupons_valid_until ON public.business_coupons USING btree (valid_until);
COMMENT ON TABLE public.business_coupons IS '매장별 쿠폰 관리';

-- Column comments

COMMENT ON COLUMN public.business_coupons."coupon_type" IS 'PERCENTAGE(정률), FIXED_AMOUNT(정액)';


-- public.coupons definition

-- Drop table

-- DROP TABLE coupons;

CREATE TABLE coupons (
                         id bigserial NOT NULL,
                         code varchar(50) NOT NULL, -- 쿠폰 코드 (대소문자 구분, 유니크)
                         "name" varchar(100) NOT NULL, -- 쿠폰 이름
                         description text NULL, -- 쿠폰 설명
                         "discount_type" public."discount_type" NOT NULL, -- 할인 타입: PERCENTAGE(퍼센트), FIXED_AMOUNT(고정금액)
                         discount_value int4 NOT NULL, -- 할인 값 (PERCENTAGE일 경우 1~100, FIXED_AMOUNT일 경우 원 단위)
                         max_discount_amount int4 NULL, -- 최대 할인 금액 (PERCENTAGE 타입일 때만 사용)
                         applicable_plans text NULL, -- 적용 가능 플랜 (쉼표 구분): BASIC,PRO 또는 null(모든 플랜)
                         min_purchase_amount int4 DEFAULT 0 NULL, -- 최소 구매 금액
                         valid_from timestamp NOT NULL, -- 쿠폰 유효 시작일
                         valid_until timestamp NOT NULL, -- 쿠폰 유효 종료일
                         max_total_uses int4 NULL, -- 전체 사용 가능 횟수 (null이면 무제한)
                         current_total_uses int4 DEFAULT 0 NULL, -- 현재 사용된 횟수
                         max_uses_per_business int4 DEFAULT 1 NULL, -- 매장당 사용 가능 횟수
                         status public."coupon_status" DEFAULT 'ACTIVE'::coupon_status NULL, -- 쿠폰 상태: ACTIVE, INACTIVE, EXPIRED
                         created_by int8 NULL, -- 쿠폰 생성자 (SUPER_ADMIN)
                         created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                         updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                         CONSTRAINT check_current_uses CHECK ((current_total_uses >= 0)),
                         CONSTRAINT check_discount_value CHECK ((discount_value > 0)),
                         CONSTRAINT check_max_discount_amount CHECK (((max_discount_amount IS NULL) OR (max_discount_amount > 0))),
                         CONSTRAINT check_max_uses CHECK (((max_total_uses IS NULL) OR (max_total_uses > 0))),
                         CONSTRAINT check_min_purchase_amount CHECK ((min_purchase_amount >= 0)),
                         CONSTRAINT check_uses_per_business CHECK ((max_uses_per_business > 0)),
                         CONSTRAINT check_valid_period CHECK ((valid_from < valid_until)),
                         CONSTRAINT coupons_code_key UNIQUE (code),
                         CONSTRAINT coupons_pkey PRIMARY KEY (id),
                         CONSTRAINT coupons_created_by_fkey FOREIGN KEY (created_by) REFERENCES users(id)
);
CREATE INDEX idx_coupons_code ON public.coupons USING btree (code);
CREATE INDEX idx_coupons_created_at ON public.coupons USING btree (created_at DESC);
CREATE INDEX idx_coupons_status ON public.coupons USING btree (status);
CREATE INDEX idx_coupons_valid_period ON public.coupons USING btree (valid_from, valid_until);
COMMENT ON TABLE public.coupons IS '쿠폰 관리 테이블';

-- Column comments

COMMENT ON COLUMN public.coupons.code IS '쿠폰 코드 (대소문자 구분, 유니크)';
COMMENT ON COLUMN public.coupons."name" IS '쿠폰 이름';
COMMENT ON COLUMN public.coupons.description IS '쿠폰 설명';
COMMENT ON COLUMN public.coupons."discount_type" IS '할인 타입: PERCENTAGE(퍼센트), FIXED_AMOUNT(고정금액)';
COMMENT ON COLUMN public.coupons.discount_value IS '할인 값 (PERCENTAGE일 경우 1~100, FIXED_AMOUNT일 경우 원 단위)';
COMMENT ON COLUMN public.coupons.max_discount_amount IS '최대 할인 금액 (PERCENTAGE 타입일 때만 사용)';
COMMENT ON COLUMN public.coupons.applicable_plans IS '적용 가능 플랜 (쉼표 구분): BASIC,PRO 또는 null(모든 플랜)';
COMMENT ON COLUMN public.coupons.min_purchase_amount IS '최소 구매 금액';
COMMENT ON COLUMN public.coupons.valid_from IS '쿠폰 유효 시작일';
COMMENT ON COLUMN public.coupons.valid_until IS '쿠폰 유효 종료일';
COMMENT ON COLUMN public.coupons.max_total_uses IS '전체 사용 가능 횟수 (null이면 무제한)';
COMMENT ON COLUMN public.coupons.current_total_uses IS '현재 사용된 횟수';
COMMENT ON COLUMN public.coupons.max_uses_per_business IS '매장당 사용 가능 횟수';
COMMENT ON COLUMN public.coupons.status IS '쿠폰 상태: ACTIVE, INACTIVE, EXPIRED';
COMMENT ON COLUMN public.coupons.created_by IS '쿠폰 생성자 (SUPER_ADMIN)';


-- public.password_reset_tokens definition

-- Drop table

-- DROP TABLE password_reset_tokens;

CREATE TABLE password_reset_tokens (
                                       id bigserial NOT NULL,
                                       user_id int8 NOT NULL,
                                       "token" varchar(100) NOT NULL, -- UUID 형태의 재설정 토큰
                                       expires_at timestamp NOT NULL, -- 토큰 만료 시간 (30분)
                                       used bpchar(1) DEFAULT 'N'::bpchar NULL, -- 사용 여부 (Y/N)
                                       created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                                       CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id),
                                       CONSTRAINT password_reset_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_password_reset_tokens_expires ON public.password_reset_tokens USING btree (expires_at) WHERE (used = 'N'::bpchar);
CREATE UNIQUE INDEX idx_password_reset_tokens_token ON public.password_reset_tokens USING btree (token);
CREATE INDEX idx_password_reset_tokens_user_id ON public.password_reset_tokens USING btree (user_id);
COMMENT ON TABLE public.password_reset_tokens IS '비밀번호 재설정 토큰';

-- Column comments

COMMENT ON COLUMN public.password_reset_tokens."token" IS 'UUID 형태의 재설정 토큰';
COMMENT ON COLUMN public.password_reset_tokens.expires_at IS '토큰 만료 시간 (30분)';
COMMENT ON COLUMN public.password_reset_tokens.used IS '사용 여부 (Y/N)';


-- public.payments definition

-- Drop table

-- DROP TABLE payments;

CREATE TABLE payments (
                          id bigserial NOT NULL,
                          business_id int8 NOT NULL,
                          coupon_id int8 NULL,
                          subscription_plan varchar(20) NOT NULL, -- 구독 플랜: FREE, BASIC, PRO, ENTERPRISE
                          billing_period_start date NOT NULL, -- 청구 기간 시작일
                          billing_period_end date NOT NULL, -- 청구 기간 종료일
                          amount int4 NOT NULL, -- 원래 금액 (할인 전)
                          discount_amount int4 DEFAULT 0 NULL, -- 할인 금액
                          final_amount int4 NOT NULL, -- 최종 결제 금액
                          coupon_code varchar(50) NULL,
                          "payment_method" public."payment_method" DEFAULT 'CARD'::payment_method NULL, -- 결제 수단: CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT, MOBILE
                          "payment_status" public."payment_status" DEFAULT 'PENDING'::payment_status NOT NULL, -- 결제 상태: PENDING, COMPLETED, FAILED, REFUNDED
                          pg_provider varchar(50) NULL, -- PG사 이름 (예: toss, iamport)
                          pg_transaction_id varchar(200) NULL, -- PG사 거래 ID
                          webhook_received_at timestamp NULL,
                          webhook_data jsonb NULL, -- 웹훅으로 받은 원본 데이터 (JSON)
                          paid_at timestamp NULL,
                          failed_reason text NULL,
                          refunded_at timestamp NULL,
                          refund_amount int4 NULL,
                          created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                          updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                          CONSTRAINT check_billing_period CHECK ((billing_period_start <= billing_period_end)),
                          CONSTRAINT check_payment_amount CHECK (((amount >= 0) AND (discount_amount >= 0) AND (final_amount >= 0))),
                          CONSTRAINT payments_pkey PRIMARY KEY (id),
                          CONSTRAINT payments_business_id_fkey FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
                          CONSTRAINT payments_coupon_id_fkey FOREIGN KEY (coupon_id) REFERENCES coupons(id)
);
CREATE INDEX idx_payments_billing_period ON public.payments USING btree (billing_period_start, billing_period_end);
CREATE INDEX idx_payments_business_id ON public.payments USING btree (business_id);
CREATE INDEX idx_payments_created_at ON public.payments USING btree (created_at DESC);
CREATE INDEX idx_payments_pg_transaction_id ON public.payments USING btree (pg_transaction_id);
CREATE INDEX idx_payments_status ON public.payments USING btree (payment_status);
COMMENT ON TABLE public.payments IS '구독 결제 내역';

-- Column comments

COMMENT ON COLUMN public.payments.subscription_plan IS '구독 플랜: FREE, BASIC, PRO, ENTERPRISE';
COMMENT ON COLUMN public.payments.billing_period_start IS '청구 기간 시작일';
COMMENT ON COLUMN public.payments.billing_period_end IS '청구 기간 종료일';
COMMENT ON COLUMN public.payments.amount IS '원래 금액 (할인 전)';
COMMENT ON COLUMN public.payments.discount_amount IS '할인 금액';
COMMENT ON COLUMN public.payments.final_amount IS '최종 결제 금액';
COMMENT ON COLUMN public.payments."payment_method" IS '결제 수단: CARD, BANK_TRANSFER, VIRTUAL_ACCOUNT, MOBILE';
COMMENT ON COLUMN public.payments."payment_status" IS '결제 상태: PENDING, COMPLETED, FAILED, REFUNDED';
COMMENT ON COLUMN public.payments.pg_provider IS 'PG사 이름 (예: toss, iamport)';
COMMENT ON COLUMN public.payments.pg_transaction_id IS 'PG사 거래 ID';
COMMENT ON COLUMN public.payments.webhook_data IS '웹훅으로 받은 원본 데이터 (JSON)';


-- public.sns_accounts definition

-- Drop table

-- DROP TABLE sns_accounts;

CREATE TABLE sns_accounts (
                              id bigserial NOT NULL,
                              user_id int8 NOT NULL,
                              provider varchar(20) NOT NULL, -- SNS 제공자 (GOOGLE, NAVER, KAKAO)
                              provider_user_id varchar(100) NOT NULL, -- SNS 제공자의 사용자 고유 ID
                              email varchar(100) NULL, -- SNS에서 제공한 이메일
                              "name" varchar(100) NULL, -- SNS에서 제공한 이름
                              profile_image_url text NULL, -- SNS 프로필 이미지 URL
                              created_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                              updated_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                              CONSTRAINT sns_accounts_pkey PRIMARY KEY (id),
                              CONSTRAINT sns_accounts_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_sns_accounts_email ON public.sns_accounts USING btree (email);
CREATE UNIQUE INDEX idx_sns_accounts_provider_user ON public.sns_accounts USING btree (provider, provider_user_id);
CREATE INDEX idx_sns_accounts_user_id ON public.sns_accounts USING btree (user_id);
COMMENT ON TABLE public.sns_accounts IS 'SNS 계정 연동 정보';

-- Column comments

COMMENT ON COLUMN public.sns_accounts.provider IS 'SNS 제공자 (GOOGLE, NAVER, KAKAO)';
COMMENT ON COLUMN public.sns_accounts.provider_user_id IS 'SNS 제공자의 사용자 고유 ID';
COMMENT ON COLUMN public.sns_accounts.email IS 'SNS에서 제공한 이메일';
COMMENT ON COLUMN public.sns_accounts."name" IS 'SNS에서 제공한 이름';
COMMENT ON COLUMN public.sns_accounts.profile_image_url IS 'SNS 프로필 이미지 URL';


-- public.business_coupon_usages definition

-- Drop table

-- DROP TABLE business_coupon_usages;

CREATE TABLE business_coupon_usages (
                                        id bigserial NOT NULL,
                                        coupon_id int8 NOT NULL,
                                        user_id int8 NOT NULL,
                                        payment_id int8 NULL,
                                        discount_amount int4 NOT NULL,
                                        used_at timestamp DEFAULT CURRENT_TIMESTAMP NULL,
                                        canceled bpchar(1) DEFAULT 'N'::bpchar NULL,
                                        canceled_at timestamp NULL,
                                        CONSTRAINT business_coupon_usages_pkey PRIMARY KEY (id),
                                        CONSTRAINT business_coupon_usages_coupon_id_fkey FOREIGN KEY (coupon_id) REFERENCES business_coupons(id) ON DELETE CASCADE,
                                        CONSTRAINT business_coupon_usages_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE SET NULL,
                                        CONSTRAINT business_coupon_usages_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_business_coupon_usages_coupon_id ON public.business_coupon_usages USING btree (coupon_id);
CREATE INDEX idx_business_coupon_usages_payment_id ON public.business_coupon_usages USING btree (payment_id);
CREATE INDEX idx_business_coupon_usages_user_id ON public.business_coupon_usages USING btree (user_id);
COMMENT ON TABLE public.business_coupon_usages IS '매장 쿠폰 사용 내역';


-- public.coupon_usages definition

-- Drop table

-- DROP TABLE coupon_usages;

CREATE TABLE coupon_usages (
                               id bigserial NOT NULL,
                               coupon_id int8 NOT NULL,
                               business_id int8 NOT NULL,
                               payment_id int8 NULL,
                               discount_amount int4 NOT NULL, -- 실제 할인된 금액
                               used_at timestamp DEFAULT CURRENT_TIMESTAMP NULL, -- 쿠폰 사용 시각
                               CONSTRAINT check_discount_amount CHECK ((discount_amount >= 0)),
                               CONSTRAINT coupon_usages_coupon_id_business_id_key UNIQUE (coupon_id, business_id),
                               CONSTRAINT coupon_usages_pkey PRIMARY KEY (id),
                               CONSTRAINT coupon_usages_business_id_fkey FOREIGN KEY (business_id) REFERENCES businesses(id) ON DELETE CASCADE,
                               CONSTRAINT coupon_usages_coupon_id_fkey FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE,
                               CONSTRAINT coupon_usages_payment_id_fkey FOREIGN KEY (payment_id) REFERENCES payments(id)
);
CREATE INDEX idx_coupon_usages_business_id ON public.coupon_usages USING btree (business_id);
CREATE INDEX idx_coupon_usages_coupon_id ON public.coupon_usages USING btree (coupon_id);
CREATE INDEX idx_coupon_usages_payment_id ON public.coupon_usages USING btree (payment_id);
CREATE INDEX idx_coupon_usages_used_at ON public.coupon_usages USING btree (used_at DESC);
COMMENT ON TABLE public.coupon_usages IS '쿠폰 사용 내역';

-- Column comments

COMMENT ON COLUMN public.coupon_usages.discount_amount IS '실제 할인된 금액';
COMMENT ON COLUMN public.coupon_usages.used_at IS '쿠폰 사용 시각';