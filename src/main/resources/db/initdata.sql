-- 테스트용 사용자 생성
-- 비밀번호는 모두 "password123" (BCrypt 암호화됨)

-- 1. ADMIN 계정
INSERT INTO users (email, password, name, phone, role, status, email_verified, created_at, updated_at)
VALUES (
           'admin@moer.io',
           '$2a$10$8kH0QG2YX5zqKZ5Vn5F3Y.8q5K5rN5K5K5K5K5K5K5K5K5K5K5K5K',  -- password123
           '시스템관리자',
           '010-0000-0000',
           'ADMIN',
           'ACTIVE',
           true,
           CURRENT_TIMESTAMP,
           CURRENT_TIMESTAMP
       );

-- 2. OWNER 계정 (Business ID 1 소속)
INSERT INTO users (email, password, name, phone, role, status, business_id, email_verified, created_at, updated_at)
VALUES (
           'owner@moer.io',
           '$2a$10$8kH0QG2YX5zqKZ5Vn5F3Y.8q5K5rN5K5K5K5K5K5K5K5K5K5K5K5K',  -- password123
           '김사장',
           '010-1111-1111',
           'OWNER',
           'ACTIVE',
           1,  -- 매장 ID
           true,
           CURRENT_TIMESTAMP,
           CURRENT_TIMESTAMP
       );

-- 3. STAFF 계정 (Business ID 1, Staff ID 1)
INSERT INTO users (email, password, name, phone, role, status, staff_id, business_id, email_verified, created_at, updated_at)
VALUES (
           'staff@moer.io',
           '$2a$10$8kH0QG2YX5zqKZ5Vn5F3Y.8q5K5rN5K5K5K5K5K5K5K5K5K5K5K5K',  -- password123
           '이디자이너',
           '010-2222-2222',
           'STAFF',
           'ACTIVE',
           1,  -- Staff ID
           1,  -- 매장 ID
           true,
           CURRENT_TIMESTAMP,
           CURRENT_TIMESTAMP
       );

COMMENT ON TABLE users IS '사용자 정보 (관리자, 사장님, 직원)';