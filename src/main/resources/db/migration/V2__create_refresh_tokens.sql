CREATE TABLE refresh_tokens (
    id         VARCHAR(36) NOT NULL,
    version    BIGINT      NULL,                  -- @Version: null => INSERT, set => UPDATE
    token_hash VARCHAR(64) NOT NULL,              -- SHA-256 (hex) of the opaque token; raw value never stored
    user_id    VARCHAR(36) NOT NULL,
    expires_at DATETIME    NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at DATETIME    NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
