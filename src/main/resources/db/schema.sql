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