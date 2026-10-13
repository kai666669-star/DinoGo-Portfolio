-- 修正 V001：DELIVERED 是物流狀態，不是訂單狀態。
-- V001 將 DELIVERED 定義為「已送達取貨點但買家尚未取貨」，
-- 因此尚未完成配送的舊訂單應回復為 SHIPPED；若物流本身已是 DELIVERED，
-- 訂單必須同步修正為 COMPLETED，避免留下跨表狀態不一致。

SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'sales.Orders', N'U') IS NULL
        THROW 51000, 'Table sales.Orders does not exist.', 1;

    IF OBJECT_ID(N'sales.Shipment', N'U') IS NULL
        THROW 51001, 'Table sales.Shipment does not exist.', 1;

    UPDATE orders
    SET status = CASE
            WHEN shipment.status = 'DELIVERED' THEN 'COMPLETED'
            ELSE 'SHIPPED'
        END,
        completed_at = CASE
            WHEN shipment.status = 'DELIVERED'
                THEN COALESCE(
                    orders.completed_at,
                    shipment.delivered_at,
                    SYSDATETIME()
                )
            ELSE NULL
        END,
        updated_at = SYSDATETIME()
    FROM sales.Orders AS orders
    LEFT JOIN sales.Shipment AS shipment
        ON shipment.order_id = orders.order_id
    WHERE orders.status = 'DELIVERED'
       OR (
           orders.status = 'SHIPPED'
           AND shipment.status = 'DELIVERED'
       );

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
