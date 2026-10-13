-- 將 DELIVERED 納入訂單狀態。
-- 業務語意：物流已送達取貨點，但買家尚未取貨。

SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'sales.Orders', N'U') IS NULL
        THROW 51000, 'Table sales.Orders does not exist.', 1;

    IF EXISTS (
        SELECT 1
        FROM sys.check_constraints
        WHERE parent_object_id = OBJECT_ID(N'sales.Orders')
          AND name = N'ck_orders_status'
    )
    BEGIN
        ALTER TABLE sales.Orders DROP CONSTRAINT ck_orders_status;
    END;

    ALTER TABLE sales.Orders WITH CHECK
    ADD CONSTRAINT ck_orders_status CHECK (
        status IN (
            'PENDING_PAYMENT',
            'PAID',
            'PROCESSING',
            'SHIPPED',
            'DELIVERED',
            'COMPLETED',
            'CANCELLED'
        )
    );

    ALTER TABLE sales.Orders CHECK CONSTRAINT ck_orders_status;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
