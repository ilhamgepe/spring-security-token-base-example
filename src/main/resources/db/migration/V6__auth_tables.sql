--  file: V6__auth_tables.sql

CREATE TABLE IF NOT EXISTS credentials
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       UUID        NOT NULL,
    provider      VARCHAR(50) NOT NULL, -- local, google, github, discord
    -- For OAuth2: the subject/id from the provider (e.g. Google's "sub")
    -- For local:  same as email (acts as the unique identifier)
    provider_id   VARCHAR(255),
    password_hash TEXT,                 -- local only password hash
    last_login_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- One provider entry per user
    CONSTRAINT uq_credentials_user_provider
        UNIQUE (user_id, provider),

    -- A provider uid must be globally unique per provider (prevents account takeover)
    CONSTRAINT uq_credentials_provider_id
        UNIQUE (provider, provider_id)
);

CREATE TABLE IF NOT EXISTS signing_keys
(
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    -- JWK Key ID sent in the JWT header ("kid" claim) – must be unique
    kid             VARCHAR(255) UNIQUE NOT NULL,
    algorithm       VARCHAR(20)         NOT NULL,-- e.g. 'RS256'

    public_key      TEXT                NOT NULL,
    private_key_enc TEXT                NOT NULL,

    is_active       BOOLEAN             NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT now(),
    expires_at      TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS refresh_sessions
(
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id            UUID        NOT NULL,

    session_id         UUID        NOT NULL UNIQUE,
    refresh_token_hash TEXT        NOT NULL,
    signing_key_id     BIGINT      NOT NULL REFERENCES signing_keys (id),

    user_agent         TEXT,
    ip_address         INET,

    is_revoked         BOOLEAN     NOT NULL DEFAULT FALSE,
    revoked_at         TIMESTAMPTZ,
    revoke_reason      VARCHAR(50), -- 'LOGOUT' | 'ROTATED' | 'SECURITY' | 'EXPIRED'

    expires_at         TIMESTAMPTZ NOT NULL,

    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_refresh_sessions_user_id
    ON refresh_sessions (user_id);

CREATE INDEX IF NOT EXISTS idx_refresh_sessions_expires_revoked
    ON refresh_sessions (expires_at, is_revoked)
    WHERE is_revoked = FALSE; -- partial index – only live sessions