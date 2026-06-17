-- file: V4__audit_logs.sql
CREATE TABLE IF NOT EXISTS audit_logs
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    actor_user_id UUID,
    actor_email VARCHAR(255),

    request_id UUID,

    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,

    action VARCHAR(100) NOT NULL,

    old_data JSONB,
    new_data JSONB,

    metadata JSONB,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_entity
    ON audit_logs(entity_type, entity_id);

CREATE INDEX idx_audit_logs_actor
    ON audit_logs(actor_user_id);

CREATE INDEX idx_audit_logs_created_at
    ON audit_logs(created_at DESC);

-- audit_logs_archive

CREATE TABLE audit_logs_archive
(
    LIKE audit_logs INCLUDING ALL
);
