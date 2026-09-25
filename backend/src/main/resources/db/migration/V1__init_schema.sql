CREATE EXTENSION IF NOT EXISTS postgis;

CREATE TYPE okey_table_status AS ENUM ('OPEN', 'FULL', 'CANCELLED');
CREATE TYPE table_request_status AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED');

CREATE TABLE users (
    id UUID PRIMARY KEY,
    firebase_uid VARCHAR(128) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    full_name VARCHAR(60) NOT NULL,
    avatar_url TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE okey_tables (
    id UUID PRIMARY KEY,
    host_id UUID NOT NULL REFERENCES users(id),
    title VARCHAR(100) NOT NULL,
    venue_name VARCHAR(100) NOT NULL,
    location GEOGRAPHY(Point, 4326) NOT NULL,
    current_players INT NOT NULL DEFAULT 1,
    max_players INT NOT NULL DEFAULT 4,
    status okey_table_status NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_okey_tables_current_players_min CHECK (current_players >= 1),
    CONSTRAINT chk_okey_tables_current_players_max CHECK (current_players <= 4),
    CONSTRAINT chk_okey_tables_max_players CHECK (max_players = 4)
);

CREATE TABLE table_requests (
    id UUID PRIMARY KEY,
    table_id UUID NOT NULL REFERENCES okey_tables(id),
    user_id UUID NOT NULL REFERENCES users(id),
    status table_request_status NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    responded_at TIMESTAMPTZ NULL,
    CONSTRAINT uq_table_requests_table_user UNIQUE (table_id, user_id)
);

CREATE TABLE chat_messages (
    id UUID PRIMARY KEY,
    table_id UUID NOT NULL REFERENCES okey_tables(id),
    sender_id UUID NOT NULL REFERENCES users(id),
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_chat_messages_message_not_blank CHECK (length(btrim(message)) > 0)
);

CREATE INDEX idx_okey_tables_location
ON okey_tables
USING GIST(location);

CREATE INDEX idx_table_requests_table_status
ON table_requests(table_id, status);

CREATE INDEX idx_table_requests_user_status
ON table_requests(user_id, status);

CREATE INDEX idx_chat_messages_table_created_at
ON chat_messages(table_id, created_at);
