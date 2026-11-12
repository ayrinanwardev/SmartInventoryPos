-- Products without a barcode are matched by name (case-insensitive) when restocking.
-- Without a unique key, two requests adding the same new name at the same moment could each
-- create a product, splitting its stock between duplicates.
--
-- Only active products must be unique: a deleted (inactive) product keeps its history and may
-- share its name with the active one that replaced it.
--
-- PostgreSQL only (partial expression index). H2, used by the API tests, has no equivalent.
CREATE UNIQUE INDEX IF NOT EXISTS ux_products_active_name ON products (lower(name)) WHERE active;
