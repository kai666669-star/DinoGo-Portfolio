IF COL_LENGTH('seller.Coupon', 'per_member_usage_policy') IS NULL
BEGIN
    ALTER TABLE seller.Coupon
        ADD per_member_usage_policy varchar(20) NOT NULL
            CONSTRAINT df_coupon_per_member_usage_policy DEFAULT ('ONCE') WITH VALUES;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.default_constraints dc
    JOIN sys.columns c
      ON c.object_id = dc.parent_object_id
     AND c.column_id = dc.parent_column_id
    WHERE dc.parent_object_id = OBJECT_ID('seller.Coupon')
      AND c.name = 'per_member_usage_policy'
)
BEGIN
    ALTER TABLE seller.Coupon
        ADD CONSTRAINT df_coupon_per_member_usage_policy DEFAULT ('ONCE')
        FOR per_member_usage_policy;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'ck_coupon_per_member_usage_policy'
      AND parent_object_id = OBJECT_ID('seller.Coupon')
)
BEGIN
    ALTER TABLE seller.Coupon WITH CHECK
        ADD CONSTRAINT ck_coupon_per_member_usage_policy
        CHECK (per_member_usage_policy IN ('ONCE', 'REPEAT'));
END;
GO

IF EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'ck_coupon_usage'
      AND parent_object_id = OBJECT_ID('seller.Coupon')
)
BEGIN
    ALTER TABLE seller.Coupon DROP CONSTRAINT ck_coupon_usage;
END;
GO

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE name = 'ck_coupon_usage'
      AND parent_object_id = OBJECT_ID('seller.Coupon')
)
BEGIN
    ALTER TABLE seller.Coupon WITH CHECK
        ADD CONSTRAINT ck_coupon_usage
        CHECK ((min_purchase_amount IS NULL OR min_purchase_amount >= 0) AND used_count >= 0);
END;
GO

IF EXISTS (
    SELECT 1
    FROM sys.indexes
    WHERE name = 'uq_orders_member_coupon'
      AND object_id = OBJECT_ID('sales.Orders')
)
BEGIN
    DROP INDEX uq_orders_member_coupon ON sales.Orders;
END;
GO
