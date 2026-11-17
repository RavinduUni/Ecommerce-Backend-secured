-- One cart line per user and product (addToCart already merges quantities).
-- Also replaces the full scan in "find this product in this user's cart".
ALTER TABLE cart_item_entity ADD CONSTRAINT uk_cart_item_user_product UNIQUE (user_id, product_id);