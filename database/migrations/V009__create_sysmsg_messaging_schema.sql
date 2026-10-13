-- //sysmsg-start，總共1次修改，第1次//
/*
正式 sysmsg schema migration（SQL Server / Flyway）。
可安全套用於 schema／資料表已存在的環境；既有 sysmsg 資料不會被刪除。
核心舊欄位若缺失會明確中止，避免以虛構值污染訊息；可安全推導的新欄位則自動補齊。
測試資料刻意不包含於 migration；需要時另行手動執行 sysmsg基本資料/MsgTestData.sql。
msg_function_sequence 可能已由既有資料庫建立，因此只補表、約束與缺少的 prefix，
不覆寫既有 current_value。
*/
SET XACT_ABORT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET NUMERIC_ROUNDABORT OFF;
GO

IF SCHEMA_ID('sysmsg') IS NULL
    EXEC('CREATE SCHEMA sysmsg AUTHORIZATION dbo');
GO

IF OBJECT_ID(N'sysmsg.send', N'U') IS NULL
BEGIN
CREATE TABLE sysmsg.send
(
    send_id INT IDENTITY(1,1) NOT NULL,
    msgfrom_seller_id INT NOT NULL,
    msg_function VARCHAR(6) NOT NULL,
    msg_label NVARCHAR(50) NOT NULL,
    send_title NVARCHAR(100) NOT NULL,
    send_content NVARCHAR(1000) NOT NULL,
    send_upd_at DATETIME2(7) NOT NULL CONSTRAINT DF_sysmsg_send_upd_at DEFAULT SYSDATETIME(),
    send_status NVARCHAR(10) NOT NULL,
    CONSTRAINT PK_sysmsg_send PRIMARY KEY (send_id),
    CONSTRAINT CK_send_label_not_blank CHECK (LEN(LTRIM(RTRIM(msg_label))) > 0),
    CONSTRAINT CK_send_title_not_blank CHECK (LEN(LTRIM(RTRIM(send_title))) > 0),
    CONSTRAINT CK_send_content_not_blank CHECK (LEN(LTRIM(RTRIM(send_content))) > 0),
    CONSTRAINT CK_send_status CHECK (send_status IN ('SEND', 'SAVE', 'DELETE')),
    CONSTRAINT CK_send_msg_function CHECK
    (
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'OA-[0-9][0-9][0-9]' AND msg_function <> 'OA-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'OC-[0-9][0-9][0-9]' AND msg_function <> 'OC-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'OS-[0-9][0-9][0-9]' AND msg_function <> 'OS-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'AC-[0-9][0-9][0-9]' AND msg_function <> 'AC-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'AS-[0-9][0-9][0-9]' AND msg_function <> 'AS-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'SC-[0-9][0-9][0-9]' AND msg_function <> 'SC-000')
    )
);
END;
GO

-- 舊表必須至少保留父訊息的權威欄位；缺少時無法無損推導，立即停止部署。
IF COL_LENGTH(N'sysmsg.send', N'send_id') IS NULL
 OR COL_LENGTH(N'sysmsg.send', N'msgfrom_seller_id') IS NULL
 OR COL_LENGTH(N'sysmsg.send', N'msg_function') IS NULL
 OR COL_LENGTH(N'sysmsg.send', N'msg_label') IS NULL
 OR COL_LENGTH(N'sysmsg.send', N'send_title') IS NULL
 OR COL_LENGTH(N'sysmsg.send', N'send_content') IS NULL
 OR COL_LENGTH(N'sysmsg.send', N'send_status') IS NULL
    THROW 51010, N'既有 sysmsg.send 缺少核心欄位，無法自動升級', 1;
GO

IF COL_LENGTH(N'sysmsg.send', N'send_upd_at') IS NULL
BEGIN
    ALTER TABLE sysmsg.send ADD send_upd_at datetime2(7) NULL;
    UPDATE sysmsg.send SET send_upd_at = SYSDATETIME() WHERE send_upd_at IS NULL;
    ALTER TABLE sysmsg.send ALTER COLUMN send_upd_at datetime2(7) NOT NULL;
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.default_constraints dc JOIN sys.columns c ON c.object_id=dc.parent_object_id AND c.column_id=dc.parent_column_id WHERE dc.parent_object_id=OBJECT_ID(N'sysmsg.send') AND c.name=N'send_upd_at')
    ALTER TABLE sysmsg.send ADD CONSTRAINT DF_sysmsg_send_upd_at DEFAULT SYSDATETIME() FOR send_upd_at;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'sysmsg.send') AND name=N'UX_sysmsg_send_msg_function_save')
    CREATE UNIQUE INDEX UX_sysmsg_send_msg_function_save ON sysmsg.send(msg_function) WHERE send_status = 'SAVE';
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'sysmsg.send') AND name=N'UX_sysmsg_send_id_msg_function')
    CREATE UNIQUE INDEX UX_sysmsg_send_id_msg_function ON sysmsg.send(send_id, msg_function);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'sysmsg.send') AND name=N'IX_sysmsg_send_owner')
    CREATE INDEX IX_sysmsg_send_owner ON sysmsg.send(msgfrom_seller_id, send_status, send_upd_at DESC, send_id DESC) INCLUDE (msg_function);
GO

IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send') AND name=N'PK_sysmsg_send')
    ALTER TABLE sysmsg.send ADD CONSTRAINT PK_sysmsg_send PRIMARY KEY (send_id);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send') AND name=N'CK_send_label_not_blank')
    ALTER TABLE sysmsg.send WITH CHECK ADD CONSTRAINT CK_send_label_not_blank CHECK (LEN(LTRIM(RTRIM(msg_label))) > 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send') AND name=N'CK_send_title_not_blank')
    ALTER TABLE sysmsg.send WITH CHECK ADD CONSTRAINT CK_send_title_not_blank CHECK (LEN(LTRIM(RTRIM(send_title))) > 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send') AND name=N'CK_send_content_not_blank')
    ALTER TABLE sysmsg.send WITH CHECK ADD CONSTRAINT CK_send_content_not_blank CHECK (LEN(LTRIM(RTRIM(send_content))) > 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send') AND name=N'CK_send_status')
    ALTER TABLE sysmsg.send WITH CHECK ADD CONSTRAINT CK_send_status CHECK (send_status IN ('SEND', 'SAVE', 'DELETE'));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send') AND name=N'CK_send_msg_function')
    ALTER TABLE sysmsg.send WITH CHECK ADD CONSTRAINT CK_send_msg_function CHECK
    (
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'OA-[0-9][0-9][0-9]' AND msg_function <> 'OA-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'OC-[0-9][0-9][0-9]' AND msg_function <> 'OC-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'OS-[0-9][0-9][0-9]' AND msg_function <> 'OS-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'AC-[0-9][0-9][0-9]' AND msg_function <> 'AC-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'AS-[0-9][0-9][0-9]' AND msg_function <> 'AS-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'SC-[0-9][0-9][0-9]' AND msg_function <> 'SC-000')
    );
GO

IF OBJECT_ID('sysmsg.msg_function_sequence', 'U') IS NULL
BEGIN
    CREATE TABLE sysmsg.msg_function_sequence
    (
        prefix VARCHAR(2) NOT NULL,
        current_value INT NOT NULL,
        CONSTRAINT PK_sysmsg_msg_function_sequence PRIMARY KEY (prefix),
        CONSTRAINT CK_sysmsg_msg_function_sequence_prefix
            CHECK (prefix IN ('OA', 'OC', 'OS', 'AC', 'AS', 'SC')),
        CONSTRAINT CK_sysmsg_msg_function_sequence_value
            CHECK (current_value BETWEEN 1 AND 999)
    );
END;
GO

INSERT INTO sysmsg.msg_function_sequence(prefix, current_value)
SELECT seed.prefix, 1
FROM (VALUES ('OA'), ('OC'), ('OS'), ('AC'), ('AS'), ('SC')) AS seed(prefix)
WHERE NOT EXISTS
(
    SELECT 1 FROM sysmsg.msg_function_sequence AS existing
    WHERE existing.prefix = seed.prefix
);
GO

IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.msg_function_sequence') AND name=N'PK_sysmsg_msg_function_sequence')
    ALTER TABLE sysmsg.msg_function_sequence ADD CONSTRAINT PK_sysmsg_msg_function_sequence PRIMARY KEY (prefix);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.msg_function_sequence') AND name=N'CK_sysmsg_msg_function_sequence_prefix')
    ALTER TABLE sysmsg.msg_function_sequence WITH CHECK ADD CONSTRAINT CK_sysmsg_msg_function_sequence_prefix CHECK (prefix IN ('OA', 'OC', 'OS', 'AC', 'AS', 'SC'));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.msg_function_sequence') AND name=N'CK_sysmsg_msg_function_sequence_value')
    ALTER TABLE sysmsg.msg_function_sequence WITH CHECK ADD CONSTRAINT CK_sysmsg_msg_function_sequence_value CHECK (current_value BETWEEN 1 AND 999);
GO

IF OBJECT_ID(N'sysmsg.send_order', N'U') IS NULL
BEGIN
CREATE TABLE sysmsg.send_order
(
    send_order_id INT NOT NULL,
    order_id INT NOT NULL,
    order_no NVARCHAR(30) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    payment_method_id INT NULL,
    method_name NVARCHAR(50) NULL,
    created_at DATETIME2(7) NOT NULL,
    status NVARCHAR(30) NOT NULL,
    CONSTRAINT PK_sysmsg_send_order PRIMARY KEY (send_order_id),
    CONSTRAINT CK_send_order_total_amount CHECK (total_amount >= 0),
    CONSTRAINT CK_send_order_order_no_not_blank CHECK (LEN(LTRIM(RTRIM(order_no))) > 0),
    CONSTRAINT CK_send_order_payment_snapshot CHECK ((payment_method_id IS NULL AND method_name IS NULL) OR (payment_method_id IS NOT NULL AND method_name IS NOT NULL AND LEN(LTRIM(RTRIM(method_name))) > 0)),
    CONSTRAINT CK_send_order_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT FK_send_order_send FOREIGN KEY (send_order_id)
        REFERENCES sysmsg.send(send_id) ON DELETE CASCADE
);
END;
GO

IF COL_LENGTH(N'sysmsg.send_order', N'send_order_id') IS NULL OR COL_LENGTH(N'sysmsg.send_order', N'order_id') IS NULL OR COL_LENGTH(N'sysmsg.send_order', N'order_no') IS NULL OR COL_LENGTH(N'sysmsg.send_order', N'total_amount') IS NULL OR COL_LENGTH(N'sysmsg.send_order', N'created_at') IS NULL OR COL_LENGTH(N'sysmsg.send_order', N'status') IS NULL
    THROW 51011, N'既有 sysmsg.send_order 缺少核心欄位，無法自動升級', 1;
IF COL_LENGTH(N'sysmsg.send_order', N'payment_method_id') IS NULL ALTER TABLE sysmsg.send_order ADD payment_method_id int NULL;
IF COL_LENGTH(N'sysmsg.send_order', N'method_name') IS NULL ALTER TABLE sysmsg.send_order ADD method_name nvarchar(50) NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send_order') AND name=N'PK_sysmsg_send_order')
    ALTER TABLE sysmsg.send_order ADD CONSTRAINT PK_sysmsg_send_order PRIMARY KEY (send_order_id);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send_order') AND name=N'CK_send_order_total_amount')
    ALTER TABLE sysmsg.send_order WITH CHECK ADD CONSTRAINT CK_send_order_total_amount CHECK (total_amount >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send_order') AND name=N'CK_send_order_order_no_not_blank')
    ALTER TABLE sysmsg.send_order WITH CHECK ADD CONSTRAINT CK_send_order_order_no_not_blank CHECK (LEN(LTRIM(RTRIM(order_no))) > 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send_order') AND name=N'CK_send_order_payment_snapshot')
    ALTER TABLE sysmsg.send_order WITH CHECK ADD CONSTRAINT CK_send_order_payment_snapshot CHECK ((payment_method_id IS NULL AND method_name IS NULL) OR (payment_method_id IS NOT NULL AND method_name IS NOT NULL AND LEN(LTRIM(RTRIM(method_name))) > 0));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send_order') AND name=N'CK_send_order_status')
    ALTER TABLE sysmsg.send_order WITH CHECK ADD CONSTRAINT CK_send_order_status CHECK (status IN ('PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED'));
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID(N'sysmsg.send_order') AND name=N'FK_send_order_send')
    ALTER TABLE sysmsg.send_order WITH CHECK ADD CONSTRAINT FK_send_order_send FOREIGN KEY (send_order_id) REFERENCES sysmsg.send(send_id) ON DELETE CASCADE;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'sysmsg.send_order') AND name=N'IX_sysmsg_send_order_event')
    CREATE INDEX IX_sysmsg_send_order_event ON sysmsg.send_order(order_id, status, send_order_id);
GO

IF OBJECT_ID(N'sysmsg.send_disorder', N'U') IS NULL
BEGIN
CREATE TABLE sysmsg.send_disorder
(
    send_disorder_id INT NOT NULL,
    order_id INT NOT NULL,
    order_no NVARCHAR(30) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    payment_method_id INT NULL,
    method_name NVARCHAR(50) NULL,
    cancel_reason NVARCHAR(500) NULL,
    cancelled_at DATETIME2(7) NOT NULL,
    status NVARCHAR(30) NOT NULL,
    CONSTRAINT PK_sysmsg_send_disorder PRIMARY KEY (send_disorder_id),
    CONSTRAINT CK_send_disorder_total_amount CHECK (total_amount >= 0),
    CONSTRAINT CK_send_disorder_order_no_not_blank CHECK (LEN(LTRIM(RTRIM(order_no))) > 0),
    CONSTRAINT CK_send_disorder_payment_snapshot CHECK ((payment_method_id IS NULL AND method_name IS NULL) OR (payment_method_id IS NOT NULL AND method_name IS NOT NULL AND LEN(LTRIM(RTRIM(method_name))) > 0)),
    CONSTRAINT CK_send_disorder_status CHECK (status = 'CANCELLED'),
    CONSTRAINT FK_send_disorder_send FOREIGN KEY (send_disorder_id)
        REFERENCES sysmsg.send(send_id) ON DELETE CASCADE
);
END;
GO

IF COL_LENGTH(N'sysmsg.send_disorder', N'send_disorder_id') IS NULL OR COL_LENGTH(N'sysmsg.send_disorder', N'order_id') IS NULL OR COL_LENGTH(N'sysmsg.send_disorder', N'order_no') IS NULL OR COL_LENGTH(N'sysmsg.send_disorder', N'total_amount') IS NULL OR COL_LENGTH(N'sysmsg.send_disorder', N'cancelled_at') IS NULL OR COL_LENGTH(N'sysmsg.send_disorder', N'status') IS NULL
    THROW 51012, N'既有 sysmsg.send_disorder 缺少核心欄位，無法自動升級', 1;
IF COL_LENGTH(N'sysmsg.send_disorder', N'payment_method_id') IS NULL ALTER TABLE sysmsg.send_disorder ADD payment_method_id int NULL;
IF COL_LENGTH(N'sysmsg.send_disorder', N'method_name') IS NULL ALTER TABLE sysmsg.send_disorder ADD method_name nvarchar(50) NULL;
IF COL_LENGTH(N'sysmsg.send_disorder', N'cancel_reason') IS NULL ALTER TABLE sysmsg.send_disorder ADD cancel_reason nvarchar(500) NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send_disorder') AND name=N'PK_sysmsg_send_disorder')
    ALTER TABLE sysmsg.send_disorder ADD CONSTRAINT PK_sysmsg_send_disorder PRIMARY KEY (send_disorder_id);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send_disorder') AND name=N'CK_send_disorder_total_amount')
    ALTER TABLE sysmsg.send_disorder WITH CHECK ADD CONSTRAINT CK_send_disorder_total_amount CHECK (total_amount >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send_disorder') AND name=N'CK_send_disorder_order_no_not_blank')
    ALTER TABLE sysmsg.send_disorder WITH CHECK ADD CONSTRAINT CK_send_disorder_order_no_not_blank CHECK (LEN(LTRIM(RTRIM(order_no))) > 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send_disorder') AND name=N'CK_send_disorder_payment_snapshot')
    ALTER TABLE sysmsg.send_disorder WITH CHECK ADD CONSTRAINT CK_send_disorder_payment_snapshot CHECK ((payment_method_id IS NULL AND method_name IS NULL) OR (payment_method_id IS NOT NULL AND method_name IS NOT NULL AND LEN(LTRIM(RTRIM(method_name))) > 0));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send_disorder') AND name=N'CK_send_disorder_status')
    ALTER TABLE sysmsg.send_disorder WITH CHECK ADD CONSTRAINT CK_send_disorder_status CHECK (status = 'CANCELLED');
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID(N'sysmsg.send_disorder') AND name=N'FK_send_disorder_send')
    ALTER TABLE sysmsg.send_disorder WITH CHECK ADD CONSTRAINT FK_send_disorder_send FOREIGN KEY (send_disorder_id) REFERENCES sysmsg.send(send_id) ON DELETE CASCADE;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'sysmsg.send_disorder') AND name=N'IX_sysmsg_send_disorder_event')
    CREATE INDEX IX_sysmsg_send_disorder_event ON sysmsg.send_disorder(order_id, status, send_disorder_id);
GO

IF OBJECT_ID(N'sysmsg.send_seller', N'U') IS NULL
BEGIN
CREATE TABLE sysmsg.send_seller
(
    send_seller_id INT NOT NULL,
    order_no NVARCHAR(30) NULL,
    -- Cloudinary HTTPS URL；附件本體不寫入 SQL Server。
    img_one NVARCHAR(500) NULL,
    img_one_public_id NVARCHAR(255) NULL,
    img_two NVARCHAR(500) NULL,
    img_two_public_id NVARCHAR(255) NULL,
    img_three NVARCHAR(500) NULL,
    img_three_public_id NVARCHAR(255) NULL,
    send_remark NVARCHAR(1000) NULL,
    CONSTRAINT PK_sysmsg_send_seller PRIMARY KEY (send_seller_id),
    CONSTRAINT CK_sysmsg_send_seller_order_no_not_blank CHECK (order_no IS NULL OR LEN(LTRIM(RTRIM(order_no))) > 0),
    CONSTRAINT CK_sysmsg_send_seller_cloudinary_refs CHECK
    (
        (img_one IS NULL AND img_one_public_id IS NULL OR img_one IS NOT NULL AND img_one_public_id IS NOT NULL AND LEN(LTRIM(RTRIM(img_one))) > 0 AND LEN(LTRIM(RTRIM(img_one_public_id))) > 0) AND
        (img_two IS NULL AND img_two_public_id IS NULL OR img_two IS NOT NULL AND img_two_public_id IS NOT NULL AND LEN(LTRIM(RTRIM(img_two))) > 0 AND LEN(LTRIM(RTRIM(img_two_public_id))) > 0) AND
        (img_three IS NULL AND img_three_public_id IS NULL OR img_three IS NOT NULL AND img_three_public_id IS NOT NULL AND LEN(LTRIM(RTRIM(img_three))) > 0 AND LEN(LTRIM(RTRIM(img_three_public_id))) > 0)
    ),
    CONSTRAINT FK_send_seller_send FOREIGN KEY (send_seller_id)
        REFERENCES sysmsg.send(send_id) ON DELETE CASCADE
);
END;
GO

IF COL_LENGTH(N'sysmsg.send_seller', N'send_seller_id') IS NULL
    THROW 51013, N'既有 sysmsg.send_seller 缺少 send_seller_id，無法自動升級', 1;
IF COL_LENGTH(N'sysmsg.send_seller', N'order_no') IS NULL ALTER TABLE sysmsg.send_seller ADD order_no nvarchar(30) NULL;
IF COL_LENGTH(N'sysmsg.send_seller', N'img_one') IS NULL ALTER TABLE sysmsg.send_seller ADD img_one nvarchar(500) NULL;
IF COL_LENGTH(N'sysmsg.send_seller', N'img_one_public_id') IS NULL ALTER TABLE sysmsg.send_seller ADD img_one_public_id nvarchar(255) NULL;
IF COL_LENGTH(N'sysmsg.send_seller', N'img_two') IS NULL ALTER TABLE sysmsg.send_seller ADD img_two nvarchar(500) NULL;
IF COL_LENGTH(N'sysmsg.send_seller', N'img_two_public_id') IS NULL ALTER TABLE sysmsg.send_seller ADD img_two_public_id nvarchar(255) NULL;
IF COL_LENGTH(N'sysmsg.send_seller', N'img_three') IS NULL ALTER TABLE sysmsg.send_seller ADD img_three nvarchar(500) NULL;
IF COL_LENGTH(N'sysmsg.send_seller', N'img_three_public_id') IS NULL ALTER TABLE sysmsg.send_seller ADD img_three_public_id nvarchar(255) NULL;
IF COL_LENGTH(N'sysmsg.send_seller', N'send_remark') IS NULL ALTER TABLE sysmsg.send_seller ADD send_remark nvarchar(1000) NULL;
GO
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send_seller') AND name=N'PK_sysmsg_send_seller')
    ALTER TABLE sysmsg.send_seller ADD CONSTRAINT PK_sysmsg_send_seller PRIMARY KEY (send_seller_id);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send_seller') AND name=N'CK_sysmsg_send_seller_order_no_not_blank')
    ALTER TABLE sysmsg.send_seller WITH CHECK ADD CONSTRAINT CK_sysmsg_send_seller_order_no_not_blank CHECK (order_no IS NULL OR LEN(LTRIM(RTRIM(order_no))) > 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send_seller') AND name=N'CK_sysmsg_send_seller_cloudinary_refs')
    ALTER TABLE sysmsg.send_seller WITH CHECK ADD CONSTRAINT CK_sysmsg_send_seller_cloudinary_refs CHECK ((img_one IS NULL AND img_one_public_id IS NULL OR img_one IS NOT NULL AND img_one_public_id IS NOT NULL AND LEN(LTRIM(RTRIM(img_one))) > 0 AND LEN(LTRIM(RTRIM(img_one_public_id))) > 0) AND (img_two IS NULL AND img_two_public_id IS NULL OR img_two IS NOT NULL AND img_two_public_id IS NOT NULL AND LEN(LTRIM(RTRIM(img_two))) > 0 AND LEN(LTRIM(RTRIM(img_two_public_id))) > 0) AND (img_three IS NULL AND img_three_public_id IS NULL OR img_three IS NOT NULL AND img_three_public_id IS NOT NULL AND LEN(LTRIM(RTRIM(img_three))) > 0 AND LEN(LTRIM(RTRIM(img_three_public_id))) > 0));
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID(N'sysmsg.send_seller') AND name=N'FK_send_seller_send')
    ALTER TABLE sysmsg.send_seller WITH CHECK ADD CONSTRAINT FK_send_seller_send FOREIGN KEY (send_seller_id) REFERENCES sysmsg.send(send_id) ON DELETE CASCADE;
GO

IF OBJECT_ID(N'sysmsg.record', N'U') IS NULL
BEGIN
CREATE TABLE sysmsg.record
(
    record_id INT IDENTITY(1,1) NOT NULL,
    send_id INT NOT NULL,
    msg_function VARCHAR(6) NOT NULL,
    msgfrom_seller_id INT NOT NULL,
    msgto_member_id INT NULL,
    msgto_seller_id INT NULL,
    order_id INT NULL,
    order_status NVARCHAR(30) NULL,
    member_inbox AS
    (
        CASE
            WHEN msgto_member_id IS NULL THEN NULL
            WHEN LEFT(msg_function, 2) IN ('OA', 'OC') THEN 'SYSTEM_INBOX'
            WHEN LEFT(msg_function, 2) = 'AC' THEN 'ORDER_INBOX'
            WHEN LEFT(msg_function, 2) = 'SC' THEN 'SELLER_INBOX'
            ELSE NULL
        END
    ) PERSISTED,
    seller_inbox AS
    (
        CASE
            WHEN msgto_seller_id IS NULL THEN NULL
            WHEN LEFT(msg_function, 2) IN ('OA', 'OS') THEN 'SYSTEM_NOTICE'
            WHEN LEFT(msg_function, 2) = 'AS' AND order_status = 'CANCELLED' THEN 'CANCELLED_ORDER'
            WHEN LEFT(msg_function, 2) = 'AS'
                 AND order_status IN ('PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED')
                THEN 'NEW_ORDER'
            ELSE NULL
        END
    ) PERSISTED,
    record_status NVARCHAR(10) NOT NULL CONSTRAINT DF_sysmsg_record_status DEFAULT 'UNREAD',
    record_created_at DATETIME2(7) NOT NULL CONSTRAINT DF_sysmsg_record_created_at DEFAULT SYSDATETIME(),
    CONSTRAINT PK_sysmsg_record PRIMARY KEY (record_id),
    CONSTRAINT CK_record_status CHECK (record_status IN ('UNREAD', 'READ', 'DELETE')),
    CONSTRAINT CK_record_msg_function CHECK
    (
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'OA-[0-9][0-9][0-9]' AND msg_function <> 'OA-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'OC-[0-9][0-9][0-9]' AND msg_function <> 'OC-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'OS-[0-9][0-9][0-9]' AND msg_function <> 'OS-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'AC-[0-9][0-9][0-9]' AND msg_function <> 'AC-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'AS-[0-9][0-9][0-9]' AND msg_function <> 'AS-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'SC-[0-9][0-9][0-9]' AND msg_function <> 'SC-000')
    ),
    CONSTRAINT FK_sysmsg_record_send_function FOREIGN KEY (send_id, msg_function)
        REFERENCES sysmsg.send(send_id, msg_function),
    CONSTRAINT CK_sysmsg_record_exactly_one_recipient CHECK
    (
        (msgto_member_id IS NOT NULL AND msgto_seller_id IS NULL) OR
        (msgto_member_id IS NULL AND msgto_seller_id IS NOT NULL)
    ),
    CONSTRAINT CK_sysmsg_record_recipient_function CHECK
    (
        (msgto_member_id IS NOT NULL AND LEFT(msg_function, 2) IN ('OA', 'OC', 'AC', 'SC')) OR
        (msgto_seller_id IS NOT NULL AND LEFT(msg_function, 2) IN ('OA', 'OS', 'AS'))
    ),
    CONSTRAINT CK_sysmsg_record_order_contract CHECK
    (
        (LEFT(msg_function, 2) IN ('AC', 'AS') AND order_id IS NOT NULL AND order_status IS NOT NULL AND order_status IN ('PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED')) OR
        (LEFT(msg_function, 2) IN ('OA', 'OC', 'OS', 'SC') AND order_id IS NULL AND order_status IS NULL)
    ),
    CONSTRAINT CK_sysmsg_record_order_snapshot CHECK
    (
        (order_id IS NULL AND order_status IS NULL) OR
        (order_id IS NOT NULL AND order_status IS NOT NULL AND order_status IN ('PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED'))
    )
);
END;
GO

IF COL_LENGTH(N'sysmsg.record', N'record_id') IS NULL OR COL_LENGTH(N'sysmsg.record', N'send_id') IS NULL OR COL_LENGTH(N'sysmsg.record', N'msg_function') IS NULL OR COL_LENGTH(N'sysmsg.record', N'msgfrom_seller_id') IS NULL OR COL_LENGTH(N'sysmsg.record', N'msgto_member_id') IS NULL OR COL_LENGTH(N'sysmsg.record', N'msgto_seller_id') IS NULL
    THROW 51014, N'既有 sysmsg.record 缺少核心欄位，無法自動升級', 1;
IF COL_LENGTH(N'sysmsg.record', N'order_id') IS NULL ALTER TABLE sysmsg.record ADD order_id int NULL;
IF COL_LENGTH(N'sysmsg.record', N'order_status') IS NULL ALTER TABLE sysmsg.record ADD order_status nvarchar(30) NULL;
IF COL_LENGTH(N'sysmsg.record', N'record_status') IS NULL
BEGIN
    ALTER TABLE sysmsg.record ADD record_status nvarchar(10) NULL;
    UPDATE sysmsg.record SET record_status='UNREAD' WHERE record_status IS NULL;
    ALTER TABLE sysmsg.record ALTER COLUMN record_status nvarchar(10) NOT NULL;
END;
IF COL_LENGTH(N'sysmsg.record', N'record_created_at') IS NULL
BEGIN
    ALTER TABLE sysmsg.record ADD record_created_at datetime2(7) NULL;
    UPDATE sysmsg.record SET record_created_at=SYSDATETIME() WHERE record_created_at IS NULL;
    ALTER TABLE sysmsg.record ALTER COLUMN record_created_at datetime2(7) NOT NULL;
END;
GO

IF COL_LENGTH(N'sysmsg.record', N'member_inbox') IS NULL
    EXEC(N'ALTER TABLE sysmsg.record ADD member_inbox AS (CASE WHEN msgto_member_id IS NULL THEN NULL WHEN LEFT(msg_function,2) IN (''OA'',''OC'') THEN ''SYSTEM_INBOX'' WHEN LEFT(msg_function,2)=''AC'' THEN ''ORDER_INBOX'' WHEN LEFT(msg_function,2)=''SC'' THEN ''SELLER_INBOX'' ELSE NULL END) PERSISTED');
IF COL_LENGTH(N'sysmsg.record', N'seller_inbox') IS NULL
    EXEC(N'ALTER TABLE sysmsg.record ADD seller_inbox AS (CASE WHEN msgto_seller_id IS NULL THEN NULL WHEN LEFT(msg_function,2) IN (''OA'',''OS'') THEN ''SYSTEM_NOTICE'' WHEN LEFT(msg_function,2)=''AS'' AND order_status=''CANCELLED'' THEN ''CANCELLED_ORDER'' WHEN LEFT(msg_function,2)=''AS'' AND order_status IN (''PAID'',''PROCESSING'',''SHIPPED'',''DELIVERED'',''COMPLETED'') THEN ''NEW_ORDER'' ELSE NULL END) PERSISTED');
GO

IF NOT EXISTS (SELECT 1 FROM sys.default_constraints dc JOIN sys.columns c ON c.object_id=dc.parent_object_id AND c.column_id=dc.parent_column_id WHERE dc.parent_object_id=OBJECT_ID(N'sysmsg.record') AND c.name=N'record_status')
    ALTER TABLE sysmsg.record ADD CONSTRAINT DF_sysmsg_record_status DEFAULT 'UNREAD' FOR record_status;
IF NOT EXISTS (SELECT 1 FROM sys.default_constraints dc JOIN sys.columns c ON c.object_id=dc.parent_object_id AND c.column_id=dc.parent_column_id WHERE dc.parent_object_id=OBJECT_ID(N'sysmsg.record') AND c.name=N'record_created_at')
    ALTER TABLE sysmsg.record ADD CONSTRAINT DF_sysmsg_record_created_at DEFAULT SYSDATETIME() FOR record_created_at;
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.record') AND name=N'PK_sysmsg_record')
    ALTER TABLE sysmsg.record ADD CONSTRAINT PK_sysmsg_record PRIMARY KEY (record_id);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.record') AND name=N'CK_record_status')
    ALTER TABLE sysmsg.record WITH CHECK ADD CONSTRAINT CK_record_status CHECK (record_status IN ('UNREAD','READ','DELETE'));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.record') AND name=N'CK_record_msg_function')
    ALTER TABLE sysmsg.record WITH CHECK ADD CONSTRAINT CK_record_msg_function CHECK
    (
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'OA-[0-9][0-9][0-9]' AND msg_function <> 'OA-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'OC-[0-9][0-9][0-9]' AND msg_function <> 'OC-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'OS-[0-9][0-9][0-9]' AND msg_function <> 'OS-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'AC-[0-9][0-9][0-9]' AND msg_function <> 'AC-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'AS-[0-9][0-9][0-9]' AND msg_function <> 'AS-000') OR
        (msg_function COLLATE Latin1_General_100_BIN2 LIKE 'SC-[0-9][0-9][0-9]' AND msg_function <> 'SC-000')
    );
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.record') AND name=N'CK_sysmsg_record_exactly_one_recipient')
    ALTER TABLE sysmsg.record WITH CHECK ADD CONSTRAINT CK_sysmsg_record_exactly_one_recipient CHECK ((msgto_member_id IS NOT NULL AND msgto_seller_id IS NULL) OR (msgto_member_id IS NULL AND msgto_seller_id IS NOT NULL));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.record') AND name=N'CK_sysmsg_record_recipient_function')
    ALTER TABLE sysmsg.record WITH CHECK ADD CONSTRAINT CK_sysmsg_record_recipient_function CHECK ((msgto_member_id IS NOT NULL AND LEFT(msg_function,2) IN ('OA','OC','AC','SC')) OR (msgto_seller_id IS NOT NULL AND LEFT(msg_function,2) IN ('OA','OS','AS')));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.record') AND name=N'CK_sysmsg_record_order_contract')
    ALTER TABLE sysmsg.record WITH CHECK ADD CONSTRAINT CK_sysmsg_record_order_contract CHECK ((LEFT(msg_function,2) IN ('AC','AS') AND order_id IS NOT NULL AND order_status IS NOT NULL AND order_status IN ('PENDING_PAYMENT','PAID','PROCESSING','SHIPPED','DELIVERED','COMPLETED','CANCELLED')) OR (LEFT(msg_function,2) IN ('OA','OC','OS','SC') AND order_id IS NULL AND order_status IS NULL));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.record') AND name=N'CK_sysmsg_record_order_snapshot')
    ALTER TABLE sysmsg.record WITH CHECK ADD CONSTRAINT CK_sysmsg_record_order_snapshot CHECK ((order_id IS NULL AND order_status IS NULL) OR (order_id IS NOT NULL AND order_status IS NOT NULL AND order_status IN ('PENDING_PAYMENT','PAID','PROCESSING','SHIPPED','DELIVERED','COMPLETED','CANCELLED')));
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID(N'sysmsg.record') AND name=N'FK_sysmsg_record_send_function')
    ALTER TABLE sysmsg.record WITH CHECK ADD CONSTRAINT FK_sysmsg_record_send_function FOREIGN KEY (send_id,msg_function) REFERENCES sysmsg.send(send_id,msg_function);
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'sysmsg.record') AND name=N'IX_sysmsg_record_member_category')
    CREATE INDEX IX_sysmsg_record_member_category ON sysmsg.record(msgto_member_id, member_inbox, record_status, record_created_at DESC, record_id DESC) INCLUDE (send_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'sysmsg.record') AND name=N'IX_sysmsg_record_seller_category')
    CREATE INDEX IX_sysmsg_record_seller_category ON sysmsg.record(msgto_seller_id, seller_inbox, record_status, record_created_at DESC, record_id DESC) INCLUDE (send_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'sysmsg.record') AND name=N'IX_sysmsg_record_send_id')
    CREATE INDEX IX_sysmsg_record_send_id ON sysmsg.record(send_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'sysmsg.record') AND name=N'UX_sysmsg_record_id_send_id')
    CREATE UNIQUE INDEX UX_sysmsg_record_id_send_id ON sysmsg.record(record_id, send_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'sysmsg.record') AND name=N'UX_sysmsg_record_order_member_once')
    CREATE UNIQUE INDEX UX_sysmsg_record_order_member_once ON sysmsg.record(order_id, order_status, msgto_member_id) WHERE order_id IS NOT NULL AND msgto_member_id IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'sysmsg.record') AND name=N'UX_sysmsg_record_order_seller_once')
    CREATE UNIQUE INDEX UX_sysmsg_record_order_seller_once ON sysmsg.record(order_id, order_status, msgto_seller_id) WHERE order_id IS NOT NULL AND msgto_seller_id IS NOT NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'sysmsg.record') AND name=N'IX_sysmsg_record_msgfrom')
    CREATE INDEX IX_sysmsg_record_msgfrom ON sysmsg.record(msgfrom_seller_id, msg_function, record_status, record_created_at DESC) INCLUDE (msgto_member_id, msgto_seller_id, record_id, send_id);
GO

IF OBJECT_ID(N'sysmsg.record_channel', N'U') IS NULL
BEGIN
CREATE TABLE sysmsg.record_channel
(
    record_channel_id INT IDENTITY(1,1) NOT NULL,
    send_id INT NOT NULL,
    record_id INT NOT NULL,
    channel_type VARCHAR(10) NOT NULL,
    notification_type VARCHAR(20) NOT NULL,
    sent_at DATETIME2(7) NULL,
    provider_message_id NVARCHAR(200) NULL,
    error_message NVARCHAR(1000) NULL,
    attempt_count INT NOT NULL CONSTRAINT DF_sysmsg_record_channel_attempt_count DEFAULT (0),
    last_attempt_at DATETIME2(7) NULL,
    next_retry_at DATETIME2(7) NULL,
    failure_code NVARCHAR(50) NULL,
    dead_lettered_at DATETIME2(7) NULL,
    CONSTRAINT PK_sysmsg_record_channel PRIMARY KEY (record_channel_id),
    CONSTRAINT UX_sysmsg_record_channel_type UNIQUE (record_id, channel_type),
    CONSTRAINT FK_sysmsg_record_channel_send FOREIGN KEY (send_id)
        REFERENCES sysmsg.send(send_id),
    CONSTRAINT FK_sysmsg_record_channel_record_send FOREIGN KEY (record_id, send_id)
        REFERENCES sysmsg.record(record_id, send_id) ON DELETE CASCADE,
    CONSTRAINT CK_sysmsg_record_channel_type CHECK (channel_type IN ('EMAIL', 'LINE')),
    CONSTRAINT CK_sysmsg_record_channel_notification_type CHECK (notification_type IN ('ORDER', 'MARKETING')),
    CONSTRAINT CK_sysmsg_record_channel_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT CK_sysmsg_record_channel_result CHECK
    (
        (attempt_count=0 AND sent_at IS NULL AND provider_message_id IS NULL AND error_message IS NULL AND failure_code IS NULL AND last_attempt_at IS NULL AND next_retry_at IS NULL AND dead_lettered_at IS NULL)
        OR (attempt_count>0 AND sent_at IS NULL AND provider_message_id IS NULL AND error_message IS NULL AND failure_code IS NULL AND last_attempt_at IS NOT NULL AND next_retry_at IS NULL AND dead_lettered_at IS NULL)
        OR (attempt_count>0 AND sent_at IS NOT NULL AND provider_message_id IS NOT NULL AND LEN(LTRIM(RTRIM(provider_message_id)))>0 AND error_message IS NULL AND failure_code IS NULL AND last_attempt_at IS NOT NULL AND next_retry_at IS NULL AND dead_lettered_at IS NULL)
        OR (attempt_count>0 AND sent_at IS NULL AND provider_message_id IS NULL AND error_message IS NOT NULL AND LEN(LTRIM(RTRIM(error_message)))>0 AND failure_code IS NOT NULL AND LEN(LTRIM(RTRIM(failure_code)))>0 AND last_attempt_at IS NOT NULL AND next_retry_at IS NOT NULL AND dead_lettered_at IS NULL)
        OR (attempt_count>0 AND sent_at IS NULL AND provider_message_id IS NULL AND error_message IS NOT NULL AND LEN(LTRIM(RTRIM(error_message)))>0 AND failure_code IS NOT NULL AND LEN(LTRIM(RTRIM(failure_code)))>0 AND last_attempt_at IS NOT NULL AND next_retry_at IS NULL AND dead_lettered_at IS NOT NULL)
    )
);
END;
GO

IF COL_LENGTH(N'sysmsg.record_channel', N'record_channel_id') IS NULL OR COL_LENGTH(N'sysmsg.record_channel', N'send_id') IS NULL OR COL_LENGTH(N'sysmsg.record_channel', N'record_id') IS NULL OR COL_LENGTH(N'sysmsg.record_channel', N'channel_type') IS NULL
    THROW 51015, N'既有 sysmsg.record_channel 缺少核心欄位，無法自動升級', 1;
IF COL_LENGTH(N'sysmsg.record_channel', N'notification_type') IS NULL
BEGIN
    ALTER TABLE sysmsg.record_channel ADD notification_type varchar(20) NULL;
    UPDATE channel
    SET notification_type = CASE
        WHEN LEFT(parent.msg_function,2) IN ('AC','SC') AND parent.msgto_member_id IS NOT NULL THEN 'ORDER'
        WHEN LEFT(parent.msg_function,2) = 'AS' AND parent.msgto_seller_id IS NOT NULL THEN 'ORDER'
        ELSE 'MARKETING' END
    FROM sysmsg.record_channel channel
    INNER JOIN sysmsg.record parent ON parent.record_id=channel.record_id AND parent.send_id=channel.send_id
    WHERE channel.notification_type IS NULL;
    IF EXISTS (SELECT 1 FROM sysmsg.record_channel WHERE notification_type IS NULL)
        THROW 51016, N'既有 record_channel 無法推導 notification_type', 1;
    ALTER TABLE sysmsg.record_channel ALTER COLUMN notification_type varchar(20) NOT NULL;
END;
IF COL_LENGTH(N'sysmsg.record_channel', N'sent_at') IS NULL ALTER TABLE sysmsg.record_channel ADD sent_at datetime2(7) NULL;
IF COL_LENGTH(N'sysmsg.record_channel', N'provider_message_id') IS NULL ALTER TABLE sysmsg.record_channel ADD provider_message_id nvarchar(200) NULL;
IF COL_LENGTH(N'sysmsg.record_channel', N'error_message') IS NULL ALTER TABLE sysmsg.record_channel ADD error_message nvarchar(1000) NULL;
IF COL_LENGTH(N'sysmsg.record_channel', N'attempt_count') IS NULL ALTER TABLE sysmsg.record_channel ADD attempt_count int NOT NULL CONSTRAINT DF_sysmsg_record_channel_attempt_count DEFAULT (0) WITH VALUES;
IF COL_LENGTH(N'sysmsg.record_channel', N'last_attempt_at') IS NULL ALTER TABLE sysmsg.record_channel ADD last_attempt_at datetime2(7) NULL;
IF COL_LENGTH(N'sysmsg.record_channel', N'next_retry_at') IS NULL ALTER TABLE sysmsg.record_channel ADD next_retry_at datetime2(7) NULL;
IF COL_LENGTH(N'sysmsg.record_channel', N'failure_code') IS NULL ALTER TABLE sysmsg.record_channel ADD failure_code nvarchar(50) NULL;
IF COL_LENGTH(N'sysmsg.record_channel', N'dead_lettered_at') IS NULL ALTER TABLE sysmsg.record_channel ADD dead_lettered_at datetime2(7) NULL;
GO

-- 舊版 Channel 沒有嘗試資訊；依已有結果回填最小可驗證狀態。
UPDATE sysmsg.record_channel
SET attempt_count=1,
    last_attempt_at=COALESCE(last_attempt_at, sent_at)
WHERE sent_at IS NOT NULL AND attempt_count=0;
UPDATE sysmsg.record_channel
SET attempt_count=1,
    last_attempt_at=COALESCE(last_attempt_at, SYSUTCDATETIME()),
    failure_code=COALESCE(failure_code, N'LEGACY_FAILURE'),
    next_retry_at=COALESCE(next_retry_at, DATEADD(MINUTE, 1, SYSUTCDATETIME()))
WHERE sent_at IS NULL AND error_message IS NOT NULL AND attempt_count=0 AND dead_lettered_at IS NULL;
GO

IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.record_channel') AND name=N'PK_sysmsg_record_channel')
    ALTER TABLE sysmsg.record_channel ADD CONSTRAINT PK_sysmsg_record_channel PRIMARY KEY (record_channel_id);
IF NOT EXISTS (SELECT 1 FROM sys.key_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.record_channel') AND name=N'UX_sysmsg_record_channel_type')
    ALTER TABLE sysmsg.record_channel ADD CONSTRAINT UX_sysmsg_record_channel_type UNIQUE (record_id,channel_type);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.record_channel') AND name=N'CK_sysmsg_record_channel_type')
    ALTER TABLE sysmsg.record_channel WITH CHECK ADD CONSTRAINT CK_sysmsg_record_channel_type CHECK (channel_type IN ('EMAIL','LINE'));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.record_channel') AND name=N'CK_sysmsg_record_channel_notification_type')
    ALTER TABLE sysmsg.record_channel WITH CHECK ADD CONSTRAINT CK_sysmsg_record_channel_notification_type CHECK (notification_type IN ('ORDER','MARKETING'));
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.record_channel') AND name=N'CK_sysmsg_record_channel_attempt_count')
    ALTER TABLE sysmsg.record_channel WITH CHECK ADD CONSTRAINT CK_sysmsg_record_channel_attempt_count CHECK (attempt_count >= 0);
IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.record_channel') AND name=N'CK_sysmsg_record_channel_result')
    ALTER TABLE sysmsg.record_channel WITH CHECK ADD CONSTRAINT CK_sysmsg_record_channel_result CHECK (NOT (sent_at IS NOT NULL AND error_message IS NOT NULL) AND ((sent_at IS NULL AND provider_message_id IS NULL) OR (sent_at IS NOT NULL AND provider_message_id IS NOT NULL AND LEN(LTRIM(RTRIM(provider_message_id))) > 0)) AND (error_message IS NULL OR LEN(LTRIM(RTRIM(error_message))) > 0));
IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.record_channel') AND name=N'CK_sysmsg_record_channel_result_contract')
    ALTER TABLE sysmsg.record_channel DROP CONSTRAINT CK_sysmsg_record_channel_result_contract;
ALTER TABLE sysmsg.record_channel WITH CHECK ADD CONSTRAINT CK_sysmsg_record_channel_result_contract CHECK ((attempt_count=0 AND sent_at IS NULL AND provider_message_id IS NULL AND error_message IS NULL AND failure_code IS NULL AND last_attempt_at IS NULL AND next_retry_at IS NULL AND dead_lettered_at IS NULL) OR (attempt_count>0 AND sent_at IS NULL AND provider_message_id IS NULL AND error_message IS NULL AND failure_code IS NULL AND last_attempt_at IS NOT NULL AND next_retry_at IS NULL AND dead_lettered_at IS NULL) OR (attempt_count>0 AND sent_at IS NOT NULL AND provider_message_id IS NOT NULL AND LEN(LTRIM(RTRIM(provider_message_id)))>0 AND error_message IS NULL AND failure_code IS NULL AND last_attempt_at IS NOT NULL AND next_retry_at IS NULL AND dead_lettered_at IS NULL) OR (attempt_count>0 AND sent_at IS NULL AND provider_message_id IS NULL AND error_message IS NOT NULL AND LEN(LTRIM(RTRIM(error_message)))>0 AND failure_code IS NOT NULL AND LEN(LTRIM(RTRIM(failure_code)))>0 AND last_attempt_at IS NOT NULL AND next_retry_at IS NOT NULL AND dead_lettered_at IS NULL) OR (attempt_count>0 AND sent_at IS NULL AND provider_message_id IS NULL AND error_message IS NOT NULL AND LEN(LTRIM(RTRIM(error_message)))>0 AND failure_code IS NOT NULL AND LEN(LTRIM(RTRIM(failure_code)))>0 AND last_attempt_at IS NOT NULL AND next_retry_at IS NULL AND dead_lettered_at IS NOT NULL));
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID(N'sysmsg.record_channel') AND name=N'FK_sysmsg_record_channel_send')
    ALTER TABLE sysmsg.record_channel WITH CHECK ADD CONSTRAINT FK_sysmsg_record_channel_send FOREIGN KEY (send_id) REFERENCES sysmsg.send(send_id);
IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE parent_object_id=OBJECT_ID(N'sysmsg.record_channel') AND name=N'FK_sysmsg_record_channel_record_send')
    ALTER TABLE sysmsg.record_channel WITH CHECK ADD CONSTRAINT FK_sysmsg_record_channel_record_send FOREIGN KEY (record_id,send_id) REFERENCES sysmsg.record(record_id,send_id) ON DELETE CASCADE;
GO

IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'sysmsg.record_channel') AND name=N'IX_sysmsg_record_channel_pending')
    DROP INDEX IX_sysmsg_record_channel_pending ON sysmsg.record_channel;
CREATE INDEX IX_sysmsg_record_channel_pending ON sysmsg.record_channel(channel_type, record_channel_id) INCLUDE (record_id, send_id, notification_type) WHERE sent_at IS NULL AND provider_message_id IS NULL AND error_message IS NULL AND attempt_count=0 AND last_attempt_at IS NULL AND dead_lettered_at IS NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'sysmsg.record_channel') AND name=N'IX_sysmsg_record_channel_send')
    CREATE INDEX IX_sysmsg_record_channel_send ON sysmsg.record_channel(send_id, record_id, channel_type);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'sysmsg.record_channel') AND name=N'IX_sysmsg_record_channel_retry_due')
    CREATE INDEX IX_sysmsg_record_channel_retry_due ON sysmsg.record_channel(channel_type, next_retry_at, record_channel_id) INCLUDE (record_id, send_id, attempt_count, failure_code, error_message) WHERE channel_type='EMAIL' AND sent_at IS NULL AND provider_message_id IS NULL AND error_message IS NOT NULL AND next_retry_at IS NOT NULL AND dead_lettered_at IS NULL;
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'sysmsg.record_channel') AND name=N'IX_sysmsg_record_channel_dead_letter')
    CREATE INDEX IX_sysmsg_record_channel_dead_letter ON sysmsg.record_channel(dead_lettered_at, record_channel_id) INCLUDE (record_id, send_id, attempt_count, failure_code, error_message) WHERE dead_lettered_at IS NOT NULL;
IF EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'sysmsg.record_channel') AND name=N'IX_sysmsg_record_channel_failed_email')
    DROP INDEX IX_sysmsg_record_channel_failed_email ON sysmsg.record_channel;
GO

-- CHECK/FK 無法跨多張子表；部署結束前拒絕既有不一致資料。
IF EXISTS
(
    SELECT 1
    FROM sysmsg.send AS parent
    LEFT JOIN sysmsg.send_order AS normal_order ON normal_order.send_order_id = parent.send_id
    LEFT JOIN sysmsg.send_disorder AS cancelled_order ON cancelled_order.send_disorder_id = parent.send_id
    LEFT JOIN sysmsg.send_seller AS seller_message ON seller_message.send_seller_id = parent.send_id
    CROSS APPLY
    (
        SELECT CASE WHEN normal_order.send_order_id IS NULL THEN 0 ELSE 1 END
             + CASE WHEN cancelled_order.send_disorder_id IS NULL THEN 0 ELSE 1 END
             + CASE WHEN seller_message.send_seller_id IS NULL THEN 0 ELSE 1 END AS subtype_count
    ) AS subtype
    WHERE subtype.subtype_count > 1
       OR (LEFT(parent.msg_function, 2) IN ('AC', 'AS') AND
           (subtype.subtype_count <> 1 OR seller_message.send_seller_id IS NOT NULL))
       OR (LEFT(parent.msg_function, 2) = 'SC' AND
           (subtype.subtype_count <> 1 OR seller_message.send_seller_id IS NULL))
       OR (LEFT(parent.msg_function, 2) IN ('OA', 'OC', 'OS') AND subtype.subtype_count <> 0)
)
BEGIN
    THROW 51001, N'sysmsg.send 父子表業務一致性檢查失敗', 1;
END;
GO

-- Record 必須指向 SEND，且 sender／訂單快照與 Send 及其唯一子型別一致。
IF EXISTS
(
    SELECT 1
    FROM sysmsg.record AS recipient
    INNER JOIN sysmsg.send AS parent
        ON parent.send_id=recipient.send_id AND parent.msg_function=recipient.msg_function
    LEFT JOIN sysmsg.send_order AS normal_order ON normal_order.send_order_id=parent.send_id
    LEFT JOIN sysmsg.send_disorder AS cancelled_order ON cancelled_order.send_disorder_id=parent.send_id
    WHERE parent.send_status <> 'SEND'
       OR recipient.msgfrom_seller_id <> parent.msgfrom_seller_id
       OR (LEFT(parent.msg_function,2) IN ('AC','AS') AND
           (recipient.order_id IS NULL OR recipient.order_status IS NULL OR
            (normal_order.send_order_id IS NOT NULL AND (recipient.order_id<>normal_order.order_id OR recipient.order_status<>normal_order.status)) OR
            (cancelled_order.send_disorder_id IS NOT NULL AND (recipient.order_id<>cancelled_order.order_id OR recipient.order_status<>cancelled_order.status))))
       OR (LEFT(parent.msg_function,2) IN ('OA','OC','OS','SC') AND (recipient.order_id IS NOT NULL OR recipient.order_status IS NOT NULL))
)
BEGIN
    THROW 51002, N'sysmsg.record 與 send／子型別一致性檢查失敗', 1;
END;
GO

-- notification_type 必須由 Record 收件人與 prefix 唯一推導，禁止外部任意指定。
IF EXISTS
(
    SELECT 1
    FROM sysmsg.record_channel AS channel
    INNER JOIN sysmsg.record AS recipient
        ON recipient.record_id=channel.record_id AND recipient.send_id=channel.send_id
    CROSS APPLY
    (
        SELECT CASE
            WHEN recipient.msgto_member_id IS NOT NULL AND LEFT(recipient.msg_function,2) IN ('AC','SC') THEN 'ORDER'
            WHEN recipient.msgto_seller_id IS NOT NULL AND LEFT(recipient.msg_function,2)='AS' THEN 'ORDER'
            WHEN recipient.msgto_member_id IS NOT NULL AND LEFT(recipient.msg_function,2) IN ('OA','OC') THEN 'MARKETING'
            WHEN recipient.msgto_seller_id IS NOT NULL AND LEFT(recipient.msg_function,2) IN ('OA','OS') THEN 'MARKETING'
            ELSE 'INVALID'
        END AS expected_notification_type
    ) AS expected
    WHERE expected.expected_notification_type='INVALID'
       OR channel.notification_type<>expected.expected_notification_type
)
BEGIN
    THROW 51003, N'sysmsg.record_channel 通知分類一致性檢查失敗', 1;
END;
GO

IF EXISTS
(
    SELECT 1 FROM sysmsg.record_channel
    WHERE attempt_count<0 OR NOT
    (
        (attempt_count=0 AND sent_at IS NULL AND provider_message_id IS NULL AND error_message IS NULL AND failure_code IS NULL AND last_attempt_at IS NULL AND next_retry_at IS NULL AND dead_lettered_at IS NULL)
        OR (attempt_count>0 AND sent_at IS NULL AND provider_message_id IS NULL AND error_message IS NULL AND failure_code IS NULL AND last_attempt_at IS NOT NULL AND next_retry_at IS NULL AND dead_lettered_at IS NULL)
        OR (attempt_count>0 AND sent_at IS NOT NULL AND provider_message_id IS NOT NULL AND error_message IS NULL AND failure_code IS NULL AND last_attempt_at IS NOT NULL AND next_retry_at IS NULL AND dead_lettered_at IS NULL)
        OR (attempt_count>0 AND sent_at IS NULL AND provider_message_id IS NULL AND error_message IS NOT NULL AND failure_code IS NOT NULL AND last_attempt_at IS NOT NULL AND next_retry_at IS NOT NULL AND dead_lettered_at IS NULL)
        OR (attempt_count>0 AND sent_at IS NULL AND provider_message_id IS NULL AND error_message IS NOT NULL AND failure_code IS NOT NULL AND last_attempt_at IS NOT NULL AND next_retry_at IS NULL AND dead_lettered_at IS NOT NULL)
    )
)
BEGIN
    THROW 51004, N'sysmsg.record_channel 發送／重試／死信狀態檢查失敗', 1;
END;
GO
-- //sysmsg-end，總共1次修改，第1次//

-- 原 V011 constraint 升級內容直接整併於 V009：
-- 即使 sysmsg schema 已存在，重新執行 V009 仍會更新訂單通知狀態白名單。
SET XACT_ABORT ON;
GO

BEGIN TRY
    BEGIN TRANSACTION;

    IF OBJECT_ID(N'sysmsg.send_order', N'U') IS NULL
        THROW 51020, N'找不到 sysmsg.send_order，無法擴充訂單通知狀態', 1;
    IF OBJECT_ID(N'sysmsg.record', N'U') IS NULL
        THROW 51021, N'找不到 sysmsg.record，無法擴充訂單通知狀態', 1;

    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.send_order') AND name=N'CK_send_order_status')
        ALTER TABLE sysmsg.send_order DROP CONSTRAINT CK_send_order_status;
    ALTER TABLE sysmsg.send_order WITH CHECK ADD CONSTRAINT CK_send_order_status CHECK
    (status IN ('PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED'));
    ALTER TABLE sysmsg.send_order CHECK CONSTRAINT CK_send_order_status;

    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.record') AND name=N'CK_sysmsg_record_order_contract')
        ALTER TABLE sysmsg.record DROP CONSTRAINT CK_sysmsg_record_order_contract;
    ALTER TABLE sysmsg.record WITH CHECK ADD CONSTRAINT CK_sysmsg_record_order_contract CHECK
    (
        (LEFT(msg_function, 2) IN ('AC', 'AS') AND order_id IS NOT NULL
            AND order_status IN ('PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED')) OR
        (LEFT(msg_function, 2) IN ('OA', 'OC', 'OS', 'SC') AND order_id IS NULL AND order_status IS NULL)
    );
    ALTER TABLE sysmsg.record CHECK CONSTRAINT CK_sysmsg_record_order_contract;

    IF EXISTS (SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'sysmsg.record') AND name=N'CK_sysmsg_record_order_snapshot')
        ALTER TABLE sysmsg.record DROP CONSTRAINT CK_sysmsg_record_order_snapshot;
    ALTER TABLE sysmsg.record WITH CHECK ADD CONSTRAINT CK_sysmsg_record_order_snapshot CHECK
    (
        (order_id IS NULL AND order_status IS NULL) OR
        (order_id IS NOT NULL
            AND order_status IN ('PENDING_PAYMENT', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'COMPLETED', 'CANCELLED'))
    );
    ALTER TABLE sysmsg.record CHECK CONSTRAINT CK_sysmsg_record_order_snapshot;

    COMMIT TRANSACTION;
END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0
        ROLLBACK TRANSACTION;
    THROW;
END CATCH;
GO
