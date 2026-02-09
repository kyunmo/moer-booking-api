-- Phase 3: SNS 로그인 시스템 추가
-- sns_accounts 테이블 생성

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

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 인덱스 생성
CREATE UNIQUE INDEX idx_sns_accounts_provider_user
  ON sns_accounts(provider, provider_user_id);

CREATE INDEX idx_sns_accounts_user_id
  ON sns_accounts(user_id);

CREATE INDEX idx_sns_accounts_email
  ON sns_accounts(email);

-- 코멘트 추가
COMMENT ON TABLE sns_accounts IS 'SNS 계정 연동 정보';
COMMENT ON COLUMN sns_accounts.provider IS 'SNS 제공자 (GOOGLE, NAVER, KAKAO)';
COMMENT ON COLUMN sns_accounts.provider_user_id IS 'SNS 제공자의 사용자 고유 ID';
COMMENT ON COLUMN sns_accounts.email IS 'SNS에서 제공한 이메일';
COMMENT ON COLUMN sns_accounts.name IS 'SNS에서 제공한 이름';
COMMENT ON COLUMN sns_accounts.profile_image_url IS 'SNS 프로필 이미지 URL';
