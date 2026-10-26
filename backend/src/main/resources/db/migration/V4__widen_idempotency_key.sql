-- Circle idempotency keys combine a prefix + two UUIDs, which can exceed 80 chars.
ALTER TABLE transactions ALTER COLUMN idempotency_key SET DATA TYPE VARCHAR(120);
