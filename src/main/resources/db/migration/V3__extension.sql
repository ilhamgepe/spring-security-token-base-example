-- file: V3__extension.sql
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "citext";     -- case-insensitive email
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements"; -- track SQL statements
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- enable pg_trgm
CREATE EXTENSION IF NOT EXISTS "btree_gin";