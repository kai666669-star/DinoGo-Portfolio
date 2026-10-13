-- 每次變更密碼時遞增版本，讓先前簽發的 JWT 立即失效。

SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'member.Member', N'U') IS NULL
        THROW 51000, 'Table member.Member does not exist.', 1;

    IF COL_LENGTH(N'member.Member', N'auth_version') IS NULL
    BEGIN
        ALTER TABLE member.Member
        ADD auth_version int NOT NULL
            CONSTRAINT df_member_auth_version DEFAULT 0;
    END;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
