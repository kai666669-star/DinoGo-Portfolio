-- 會員停權、恢復與自主註銷的帳號狀態與稽核歷程。
SET XACT_ABORT ON;

BEGIN TRY
    BEGIN TRANSACTION;

    -- 舊版停用帳號統一納入可由管理員恢復的 SUSPENDED 狀態，再加入新約束。
    UPDATE member.Member
    SET status = 'SUSPENDED', updated_at = SYSDATETIME()
    WHERE status = 'INACTIVE';

    IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name = 'ck_member_status')
        ALTER TABLE member.Member
        ADD CONSTRAINT ck_member_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DEACTIVATED'));

    IF OBJECT_ID(N'member.MemberAccountStatusHistory', N'U') IS NULL
    BEGIN
        CREATE TABLE member.MemberAccountStatusHistory (
            history_id int IDENTITY(1,1) NOT NULL CONSTRAINT pk_member_account_status_history PRIMARY KEY,
            member_id int NOT NULL CONSTRAINT fk_member_account_status_history_member
                FOREIGN KEY REFERENCES member.Member(member_id),
            previous_status varchar(20) NOT NULL,
            new_status varchar(20) NOT NULL,
            reason nvarchar(500) NULL,
            changed_by int NULL CONSTRAINT fk_member_account_status_history_changed_by
                FOREIGN KEY REFERENCES member.Member(member_id),
            changed_at datetime2 NOT NULL CONSTRAINT df_member_account_status_history_changed_at DEFAULT SYSDATETIME()
        );
        CREATE INDEX ix_member_account_status_history_member_changed_at
            ON member.MemberAccountStatusHistory(member_id, changed_at DESC);
    END;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;
