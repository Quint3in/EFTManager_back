CREATE TABLE hideout_progress (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    station_id VARCHAR(50) NOT NULL,
    mode VARCHAR(10) NOT NULL,
    level INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_hideout_progress_user_station_mode
      UNIQUE (user_id, station_id, mode)
);

CREATE INDEX idx_hideout_progress_user_mode
    ON hideout_progress (user_id, mode);