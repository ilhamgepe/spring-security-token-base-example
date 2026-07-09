-- file: V5__user_tables.sql
CREATE TABLE IF NOT EXISTS users
(
    id         UUID PRIMARY KEY,
    status     VARCHAR(100)  NOT NULL DEFAULT 'ACTIVE',     -- ACTIVE, DISABLED, SUSPENDED
    kyc_status VARCHAR(100)  NOT NULL DEFAULT 'UNVERIFIED', -- UNVERIFIED, PENDING, VERIFIED, REJECTED
    email      citext UNIQUE NOT NULL,
    nickname   citext UNIQUE NOT NULL,

    -- audit fields
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    deleted_by UUID
);
-- cari user by nickname
CREATE INDEX idx_users_nickname_trgm
    ON users USING GIN (nickname gin_trgm_ops);

-- operator cari user by email (partial: "john@gm...")
-- citext sudah case-insensitive, trigram untuk partial match
CREATE INDEX idx_users_email_trgm
    ON users USING GIN (email gin_trgm_ops);

CREATE TABLE IF NOT EXISTS user_profiles
(
    user_id                UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    avatar_url             TEXT,
    stream_key             VARCHAR(255) UNIQUE NOT NULL,
    message_for_supporters TEXT,
    bio                    TEXT,
    social_links           JSONB,
    meta                   JSONB
);

CREATE TABLE IF NOT EXISTS roles
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE, -- MERCHANT, ADMIN
    name        VARCHAR(100) NOT NULL,        -- Merchant, Administrator
    description TEXT
);

CREATE TABLE IF NOT EXISTS user_roles
(
    user_id UUID REFERENCES users (id) ON DELETE CASCADE,
    role_id BIGINT REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS permissions
(
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code        VARCHAR(100) NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS role_permissions
(
    role_id       BIGINT REFERENCES roles (id) ON DELETE CASCADE,
    permission_id BIGINT REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);


-- 1. SEED ROLES
-- Menambahkan role dasar yang biasanya dibutuhkan dalam aplikasi SaaS/Platform Finansial
INSERT INTO roles (code, name, description)
VALUES ('SUPER_ADMIN', 'Super Administrator', 'Akses penuh ke seluruh sistem dan konfigurasi.'),
       ('OPERATION', 'Operation Staff', 'Mengelola operasional harian, verifikasi merchant, dan tiket bantuan.'),
       ('FINANCE', 'Finance & Accounting',
        'Mengelola transaksi, rekonsiliasi dana, pendaftaran bank, dan laporan keuangan.'),
       ('USER', 'User', 'Pengguna reguler yang menggunakan layanan untuk menerima dukungan/pembayaran.')
ON CONFLICT (code) DO NOTHING;


-- 2. SEED USERS
-- Menambahkan 3 user utama: System, Admin, dan Operation
INSERT INTO users (id, status, email, nickname, created_at, updated_at)
VALUES
-- System / Internal Automations
('00000000-0000-0000-0000-000000000000', 'ACTIVE', 'system@bayr.app', 'system', now(), now()),

-- Admin User
('019eb662-54fa-7138-b0ce-88d71251e48f', 'ACTIVE', 'admin@bayr.app', 'admin.bayr', now(), now()),

-- Operation User
('019eb662-7916-7551-86fb-5202f78adc69', 'ACTIVE', 'ops.john@bayr.app', 'john.ops', now(), now())
ON CONFLICT (id) DO NOTHING;


-- 3. SEED USER PROFILES
-- Memberikan profil dasar dan stream_key unik (menggunakan string acak/placeholder aman)
INSERT INTO user_profiles (user_id, avatar_url, stream_key, bio, social_links, meta)
VALUES ('00000000-0000-0000-0000-000000000000', 'https://api.dicebear.com/7.x/bottts/svg?seed=system',
        'live_sk_system_internal_secret_key', 'Internal automated system account.', '{}', '{
    "internal": true
  }'),
       ('019eb662-54fa-7138-b0ce-88d71251e48f', 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin',
        'live_sk_admin_super_secure_key_123', 'Chief Administrator Officer.', '{
         "twitter": "@bayr_app"
       }', '{
         "department": "IT"
       }'),
       ('019eb662-7916-7551-86fb-5202f78adc69', 'https://api.dicebear.com/7.x/avataaars/svg?seed=ops',
        'live_sk_ops_secure_key_456', 'Head of Platform Operations.', '{}', '{
         "department": "Operations"
       }')
ON CONFLICT (user_id) DO NOTHING;


-- 4. SEED USER ROLES (Mapping Hubungan User & Role)
INSERT INTO user_roles (user_id, role_id)
VALUES
-- System dapet Super Admin
('00000000-0000-0000-0000-000000000000', (SELECT id FROM roles WHERE code = 'SUPER_ADMIN')),

-- Admin dapet Super Admin
('019eb662-54fa-7138-b0ce-88d71251e48f', (SELECT id FROM roles WHERE code = 'SUPER_ADMIN')),

-- Operation dapet role Operation
('019eb662-7916-7551-86fb-5202f78adc69', (SELECT id FROM roles WHERE code = 'OPERATION'))
ON CONFLICT (user_id, role_id) DO NOTHING;