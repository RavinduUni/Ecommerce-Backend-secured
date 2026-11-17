-- Wider text columns (safe to run even if you already changed them by hand).
ALTER TABLE products MODIFY COLUMN description TEXT;
ALTER TABLE products MODIFY COLUMN image_url VARCHAR(1000);

-- Shop listing filters on active + category.
CREATE INDEX idx_products_active_category ON products (active, category);

-- Full-text search over name, category and description.
-- The column list must match the MATCH(...) in ProductCatalogRepository exactly.
ALTER TABLE products ADD FULLTEXT INDEX ft_products_search (name, category, description);

-- Order history by user (the foreign key already indexes user_id, this adds newest-first).
CREATE INDEX idx_order_user_created ON order_entity (user_id, created_at);