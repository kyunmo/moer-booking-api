-- Phase 2: 비밀번호 찾기 시스템 추가
-- password_reset_tokens 테이블 생성

CREATE TABLE password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used CHAR(1) DEFAULT 'N',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 인덱스 생성
CREATE UNIQUE INDEX idx_password_reset_tokens_token
  ON password_reset_tokens(token);

CREATE INDEX idx_password_reset_tokens_user_id
  ON password_reset_tokens(user_id);

CREATE INDEX idx_password_reset_tokens_expires
  ON password_reset_tokens(expires_at) WHERE used = 'N';

-- 코멘트 추가
COMMENT ON TABLE password_reset_tokens IS '비밀번호 재설정 토큰';
COMMENT ON COLUMN password_reset_tokens.token IS 'UUID 형태의 재설정 토큰';
COMMENT ON COLUMN password_reset_tokens.expires_at IS '토큰 만료 시간 (30분)';
COMMENT ON COLUMN password_reset_tokens.used IS '사용 여부 (Y/N)';
