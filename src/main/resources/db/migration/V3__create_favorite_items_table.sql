CREATE TABLE favorite_items (
                                id BIGSERIAL PRIMARY KEY,
                                user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                item_id VARCHAR(50) NOT NULL,
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT uq_favorite_items_user_item UNIQUE (user_id, item_id)
);

CREATE INDEX idx_favorite_items_user ON favorite_items (user_id);