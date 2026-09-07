CREATE TABLE users (
    id            VARCHAR(36)  NOT NULL,
    version       BIGINT       NULL,             -- @Version: null => INSERT, set => UPDATE
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,          -- Argon2id encoded, prefixed with {argon2id}
    role          VARCHAR(20)  NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    phone         VARCHAR(30)  NULL,
    crm           VARCHAR(20)  NULL,              -- doctors only
    specialty     VARCHAR(100) NULL,              -- doctors only
    created_at    DATETIME     NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('DOCTOR', 'NURSE', 'PATIENT', 'ADMIN'))
);
