-- 儲存會員的 Email 通知偏好；既有會員保留訂單通知、預設拒收行銷通知。

SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'member.Member', N'U') IS NULL
        THROW 51000, 'Table member.Member does not exist.', 1;

    IF COL_LENGTH(N'member.Member', N'email_order_notifications') IS NULL
    BEGIN
        ALTER TABLE member.Member
        ADD email_order_notifications bit NOT NULL
            CONSTRAINT df_member_email_order_notifications DEFAULT 1;
    END;

    IF COL_LENGTH(N'member.Member', N'email_marketing_notifications') IS NULL
    BEGIN
        ALTER TABLE member.Member
        ADD email_marketing_notifications bit NOT NULL
            CONSTRAINT df_member_email_marketing_notifications DEFAULT 0;
    END;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
