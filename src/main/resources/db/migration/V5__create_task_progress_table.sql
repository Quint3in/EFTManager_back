CREATE TABLE task_progress (
                               id BIGSERIAL PRIMARY KEY,
                               user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               task_id VARCHAR(50) NOT NULL,
                               mode VARCHAR(15) NOT NULL,
                               completed BOOLEAN NOT NULL DEFAULT FALSE,
                               completed_at TIMESTAMP,
                               CONSTRAINT uq_task_progress_user_task_mode UNIQUE (user_id, task_id, mode)
);

CREATE INDEX idx_task_progress_user_mode ON task_progress (user_id, mode);