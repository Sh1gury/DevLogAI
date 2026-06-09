-- DevLog initial schema
-- Generated from JPA entities; FKs added explicitly (entities use UUID userId, not @ManyToOne).

CREATE TABLE users (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    username      VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT uq_users_username UNIQUE (username)
);

CREATE TABLE entries (
    id         UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id    UUID         NOT NULL,
    content    TEXT         NOT NULL,
    entry_date DATE         NOT NULL,
    mood_score INTEGER,
    is_public  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6),
    CONSTRAINT fk_entries_user  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_entries_mood CHECK (mood_score BETWEEN 1 AND 5)
);

CREATE TABLE tags (
    id      UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID         NOT NULL,
    name    VARCHAR(255) NOT NULL,
    color   VARCHAR(255),
    CONSTRAINT fk_tags_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE entry_tags (
    entry_id UUID NOT NULL,
    tag_id   UUID NOT NULL,
    PRIMARY KEY (entry_id, tag_id),
    CONSTRAINT fk_entry_tags_entry FOREIGN KEY (entry_id) REFERENCES entries(id) ON DELETE CASCADE,
    CONSTRAINT fk_entry_tags_tag   FOREIGN KEY (tag_id)   REFERENCES tags(id)   ON DELETE CASCADE
);

CREATE TABLE digests (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id      UUID         NOT NULL,
    week_start   DATE         NOT NULL,
    ai_summary   TEXT,
    stats        JSONB,
    generated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_digests_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE standups (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id      UUID         NOT NULL,
    standup_date DATE         NOT NULL,
    yesterday    TEXT,
    today        TEXT,
    blockers     TEXT,
    generated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_standups_user_date UNIQUE (user_id, standup_date),
    CONSTRAINT fk_standups_user      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for common read patterns
CREATE INDEX idx_entries_user_date  ON entries  (user_id, entry_date DESC);
CREATE INDEX idx_tags_user          ON tags     (user_id);
CREATE INDEX idx_digests_user       ON digests  (user_id);
CREATE INDEX idx_standups_user_date ON standups (user_id, standup_date DESC);
