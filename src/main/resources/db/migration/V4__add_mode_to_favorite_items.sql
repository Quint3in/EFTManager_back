ALTER TABLE favorite_items DROP CONSTRAINT uq_favorite_items_user_item;
ALTER TABLE favorite_items ADD COLUMN mode VARCHAR(10) NOT NULL DEFAULT 'PVP';
ALTER TABLE favorite_items ALTER COLUMN mode DROP DEFAULT;
ALTER TABLE favorite_items ADD CONSTRAINT uq_favorite_items_user_item_mode UNIQUE (user_id, item_id, mode);
CREATE INDEX idx_favorite_items_user_mode ON favorite_items (user_id, mode);