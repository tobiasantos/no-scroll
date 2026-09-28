CREATE TABLE sessions (
    id               SERIAL PRIMARY KEY,
    setup_id         INTEGER NOT NULL REFERENCES setups (id) ON DELETE CASCADE,
    started_at       TIMESTAMPTZ NOT NULL,
    duration_minutes INTEGER NOT NULL
);

CREATE INDEX sessions_setup ON sessions (setup_id);
