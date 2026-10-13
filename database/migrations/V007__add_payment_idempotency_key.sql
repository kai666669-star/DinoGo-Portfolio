-- Existing payment records predate idempotency support, so the new column is
-- nullable. All newly created payments receive a key from the API contract.
IF COL_LENGTH('sales.Payment', 'idempotency_key') IS NULL
BEGIN
    ALTER TABLE sales.Payment ADD idempotency_key varchar(64) NULL;
END;

-- A filtered index lets legacy null rows coexist while guaranteeing that a
-- retried request cannot create a second payment for the same order.
IF NOT EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'uq_payment_order_idempotency_key'
      AND object_id = OBJECT_ID('sales.Payment')
)
BEGIN
    CREATE UNIQUE INDEX uq_payment_order_idempotency_key
        ON sales.Payment(order_id, idempotency_key)
        WHERE idempotency_key IS NOT NULL;
END;
