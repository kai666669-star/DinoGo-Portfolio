
/*
 * V008 Review schema migration for SQL Server.
 * 可安全套用於 schema／資料表已存在的環境；既有 review 資料不會被刪除。
 */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
SET XACT_ABORT ON;
GO

IF SCHEMA_ID(N'review') IS NULL
    EXEC(N'CREATE SCHEMA review AUTHORIZATION dbo');
GO

IF OBJECT_ID(N'review.history', N'U') IS NULL
BEGIN
    CREATE TABLE review.history
    (
        history_id int IDENTITY(1,1)
            CONSTRAINT PK_review_history PRIMARY KEY,
        seller_id int NOT NULL,
        member_id int NOT NULL,
        order_id int NOT NULL,
        order_no nvarchar(30) NOT NULL,
        CONSTRAINT UQ_review_history_order UNIQUE (order_id)
    );
END;
GO

-- 舊版 history 若沒有 order_no，先以可追蹤的 legacy 值補資料，再改為 NOT NULL。
IF COL_LENGTH(N'review.history', N'order_no') IS NULL
BEGIN
    ALTER TABLE review.history ADD order_no nvarchar(30) NULL;
END;
GO

UPDATE review.history
SET order_no = CONCAT(N'LEGACY-', order_id)
WHERE order_no IS NULL;
ALTER TABLE review.history ALTER COLUMN order_no nvarchar(30) NOT NULL;
GO

-- 修正：原檔缺少右引號 N'UQ_review_history_order 造成語法錯誤，已補上結尾單引號。
IF NOT EXISTS
(
    SELECT 1
    FROM sys.key_constraints
    WHERE parent_object_id = OBJECT_ID(N'review.history')
      AND [type] = N'UQ'
      AND [name] = N'UQ_review_history_order'
)
BEGIN
    ALTER TABLE review.history
        ADD CONSTRAINT UQ_review_history_order UNIQUE (order_id);
END;
GO

-- //review-start，優化3：支援 getSellerRatingSummary 依 seller_id 聚合查詢，避免全表掃描//
IF NOT EXISTS
(
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'review.history')
      AND [name] = N'IX_review_history_seller'
)
BEGIN
    CREATE INDEX IX_review_history_seller
        ON review.history (seller_id)
        INCLUDE (history_id);
END;
GO
-- //review-end，優化3//

IF OBJECT_ID(N'review.star', N'U') IS NULL
BEGIN
    CREATE TABLE review.star
    (
        star_id int IDENTITY(1,1)
            CONSTRAINT PK_review_star PRIMARY KEY,
        history_id int NOT NULL,
        order_item_id int NOT NULL,
        product_id int NOT NULL,
        product_name nvarchar(100) NOT NULL,
        image_url nvarchar(500) NULL,
        base_price decimal(12,2) NOT NULL,
        img_one nvarchar(500) NULL,
        img_one_public_id nvarchar(255) NULL,
        img_two nvarchar(500) NULL,
        img_two_public_id nvarchar(255) NULL,
        img_three nvarchar(500) NULL,
        img_three_public_id nvarchar(255) NULL,
        feedback nvarchar(500) NULL,
        five_star tinyint NULL,
        version bigint NOT NULL
            CONSTRAINT DF_review_star_version DEFAULT 0,
        star_upd_at datetime2(7) NOT NULL
            CONSTRAINT DF_review_star_updated_at DEFAULT SYSDATETIME(),
        review_priority AS
        (
            CASE
                WHEN five_star IS NULL THEN CONVERT(tinyint, 0)
                WHEN NULLIF(LTRIM(RTRIM(feedback)), N'') IS NOT NULL
                     AND NULLIF(LTRIM(RTRIM(img_one)), N'') IS NOT NULL
                     AND NULLIF(LTRIM(RTRIM(img_two)), N'') IS NOT NULL
                     AND NULLIF(LTRIM(RTRIM(img_three)), N'') IS NOT NULL
                    THEN CONVERT(tinyint, 5)
                WHEN NULLIF(LTRIM(RTRIM(feedback)), N'') IS NOT NULL
                     AND ((CASE WHEN NULLIF(LTRIM(RTRIM(img_one)), N'') IS NOT NULL THEN 1 ELSE 0 END)
                        + (CASE WHEN NULLIF(LTRIM(RTRIM(img_two)), N'') IS NOT NULL THEN 1 ELSE 0 END)
                        + (CASE WHEN NULLIF(LTRIM(RTRIM(img_three)), N'') IS NOT NULL THEN 1 ELSE 0 END)) = 2
                    THEN CONVERT(tinyint, 4)
                WHEN NULLIF(LTRIM(RTRIM(feedback)), N'') IS NOT NULL
                     AND (NULLIF(LTRIM(RTRIM(img_one)), N'') IS NOT NULL
                          OR NULLIF(LTRIM(RTRIM(img_two)), N'') IS NOT NULL
                          OR NULLIF(LTRIM(RTRIM(img_three)), N'') IS NOT NULL)
                    THEN CONVERT(tinyint, 3)
                WHEN NULLIF(LTRIM(RTRIM(feedback)), N'') IS NOT NULL THEN CONVERT(tinyint, 2)
                WHEN NULLIF(LTRIM(RTRIM(img_one)), N'') IS NOT NULL
                     OR NULLIF(LTRIM(RTRIM(img_two)), N'') IS NOT NULL
                     OR NULLIF(LTRIM(RTRIM(img_three)), N'') IS NOT NULL
                    THEN CONVERT(tinyint, 1)
                ELSE CONVERT(tinyint, 0)
            END
        ) PERSISTED,
        CONSTRAINT CK_review_star_five_star
            CHECK (five_star IS NULL OR five_star BETWEEN 1 AND 5),
        CONSTRAINT CK_review_star_cloudinary_refs
            CHECK
            (
                (img_one IS NULL AND img_one_public_id IS NULL OR img_one IS NOT NULL AND img_one_public_id IS NOT NULL)
                AND (img_two IS NULL AND img_two_public_id IS NULL OR img_two IS NOT NULL AND img_two_public_id IS NOT NULL)
                AND (img_three IS NULL AND img_three_public_id IS NULL OR img_three IS NOT NULL AND img_three_public_id IS NOT NULL)
            ),
        CONSTRAINT CK_review_star_unreviewed_content
            CHECK
            (
                five_star IS NOT NULL
                OR
                (
                    feedback IS NULL
                    AND img_one IS NULL
                    AND img_two IS NULL
                    AND img_three IS NULL
                )
            ),
        CONSTRAINT FK_review_star_history
            FOREIGN KEY (history_id)
            REFERENCES review.history(history_id)
            ON DELETE CASCADE,
        CONSTRAINT UQ_review_star_history_order_item
            UNIQUE (history_id, order_item_id)
    );
END;
GO

IF EXISTS
(
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'review.star')
      AND [name] = N'IX_review_star_product_keyset'
)
    DROP INDEX IX_review_star_product_keyset ON review.star;
GO

IF COL_LENGTH(N'review.star', N'image_url') IS NOT NULL
    ALTER TABLE review.star ALTER COLUMN image_url nvarchar(500) NULL;
GO

IF COL_LENGTH(N'review.star', N'product_name') IS NOT NULL
    ALTER TABLE review.star ALTER COLUMN product_name nvarchar(100) NOT NULL;
GO

IF COL_LENGTH(N'review.star', N'base_price') IS NOT NULL
    ALTER TABLE review.star ALTER COLUMN base_price decimal(12,2) NOT NULL;
GO

IF COL_LENGTH(N'review.star', N'img_one_public_id') IS NULL
    ALTER TABLE review.star ADD img_one_public_id nvarchar(255) NULL;
IF COL_LENGTH(N'review.star', N'img_two_public_id') IS NULL
    ALTER TABLE review.star ADD img_two_public_id nvarchar(255) NULL;
IF COL_LENGTH(N'review.star', N'img_three_public_id') IS NULL
    ALTER TABLE review.star ADD img_three_public_id nvarchar(255) NULL;
GO

-- 舊版資料表可能尚未包含樂觀鎖與更新時間欄位；先補欄位再補 default constraint。
IF COL_LENGTH(N'review.star', N'version') IS NULL
    ALTER TABLE review.star ADD version bigint NOT NULL
        CONSTRAINT DF_review_star_version DEFAULT 0 WITH VALUES;
IF COL_LENGTH(N'review.star', N'star_upd_at') IS NULL
    ALTER TABLE review.star ADD star_upd_at datetime2(7) NOT NULL
        CONSTRAINT DF_review_star_updated_at DEFAULT SYSDATETIME() WITH VALUES;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.default_constraints dc
    INNER JOIN sys.columns c
        ON c.object_id = dc.parent_object_id
       AND c.column_id = dc.parent_column_id
    WHERE dc.parent_object_id = OBJECT_ID(N'review.star')
      AND c.[name] = N'version'
)
BEGIN
    ALTER TABLE review.star
        ADD CONSTRAINT DF_review_star_version DEFAULT 0 FOR version;
END;
GO

IF NOT EXISTS
(
    SELECT 1
    FROM sys.default_constraints dc
    INNER JOIN sys.columns c
        ON c.object_id = dc.parent_object_id
       AND c.column_id = dc.parent_column_id
    WHERE dc.parent_object_id = OBJECT_ID(N'review.star')
      AND c.[name] = N'star_upd_at'
)
BEGIN
    ALTER TABLE review.star
        ADD CONSTRAINT DF_review_star_updated_at DEFAULT SYSDATETIME() FOR star_upd_at;
END;
GO

-- five_star 改為 tinyint 前，先移除依賴該欄位的 constraint／computed column。
IF EXISTS
(
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'review.star')
      AND [name] = N'CK_review_star_five_star'
)
    ALTER TABLE review.star DROP CONSTRAINT CK_review_star_five_star;
IF EXISTS
(
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'review.star')
      AND [name] = N'CK_review_star_unreviewed_content'
)
    ALTER TABLE review.star DROP CONSTRAINT CK_review_star_unreviewed_content;
IF COL_LENGTH(N'review.star', N'review_priority') IS NOT NULL
    ALTER TABLE review.star DROP COLUMN review_priority;
GO

IF EXISTS
(
    SELECT 1 FROM sys.columns
    WHERE object_id = OBJECT_ID(N'review.star')
      AND [name] = N'five_star'
      AND system_type_id <> TYPE_ID(N'tinyint')
)
    ALTER TABLE review.star ALTER COLUMN five_star tinyint NULL;
GO

EXEC(N'ALTER TABLE review.star
        ADD review_priority AS
        (
            CASE
                WHEN five_star IS NULL THEN CONVERT(tinyint, 0)
                WHEN NULLIF(LTRIM(RTRIM(feedback)), N'''') IS NOT NULL
                     AND NULLIF(LTRIM(RTRIM(img_one)), N'''') IS NOT NULL
                     AND NULLIF(LTRIM(RTRIM(img_two)), N'''') IS NOT NULL
                     AND NULLIF(LTRIM(RTRIM(img_three)), N'''') IS NOT NULL
                    THEN CONVERT(tinyint, 5)
                WHEN NULLIF(LTRIM(RTRIM(feedback)), N'''') IS NOT NULL
                     AND ((CASE WHEN NULLIF(LTRIM(RTRIM(img_one)), N'''') IS NOT NULL THEN 1 ELSE 0 END)
                        + (CASE WHEN NULLIF(LTRIM(RTRIM(img_two)), N'''') IS NOT NULL THEN 1 ELSE 0 END)
                        + (CASE WHEN NULLIF(LTRIM(RTRIM(img_three)), N'''') IS NOT NULL THEN 1 ELSE 0 END)) = 2
                    THEN CONVERT(tinyint, 4)
                WHEN NULLIF(LTRIM(RTRIM(feedback)), N'''') IS NOT NULL
                     AND (NULLIF(LTRIM(RTRIM(img_one)), N'''') IS NOT NULL
                          OR NULLIF(LTRIM(RTRIM(img_two)), N'''') IS NOT NULL
                          OR NULLIF(LTRIM(RTRIM(img_three)), N'''') IS NOT NULL)
                    THEN CONVERT(tinyint, 3)
                WHEN NULLIF(LTRIM(RTRIM(feedback)), N'''') IS NOT NULL THEN CONVERT(tinyint, 2)
                WHEN NULLIF(LTRIM(RTRIM(img_one)), N'''') IS NOT NULL
                     OR NULLIF(LTRIM(RTRIM(img_two)), N'''') IS NOT NULL
                     OR NULLIF(LTRIM(RTRIM(img_three)), N'''') IS NOT NULL
                    THEN CONVERT(tinyint, 1)
                ELSE CONVERT(tinyint, 0)
            END
        ) PERSISTED');
GO

IF NOT EXISTS
(
    SELECT 1 FROM sys.foreign_keys
    WHERE parent_object_id = OBJECT_ID(N'review.star')
      AND [name] = N'FK_review_star_history'
)
BEGIN
    ALTER TABLE review.star WITH CHECK
        ADD CONSTRAINT FK_review_star_history
        FOREIGN KEY (history_id)
        REFERENCES review.history(history_id)
        ON DELETE CASCADE;
END;
GO

IF NOT EXISTS
(
    SELECT 1 FROM sys.key_constraints
    WHERE parent_object_id = OBJECT_ID(N'review.star')
      AND [type] = N'UQ'
      AND [name] = N'UQ_review_star_history_order_item'
)
BEGIN
    ALTER TABLE review.star
        ADD CONSTRAINT UQ_review_star_history_order_item
        UNIQUE (history_id, order_item_id);
END;
GO

-- //review-start，優化2 修正：CK_review_star_cloudinary_refs 改用 WITH CHECK，
-- 新增約束前先回填／清理違規的舊資料，不再用 NOCHECK 悄悄放行髒資料//
IF NOT EXISTS
(
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'review.star')
      AND [name] = N'CK_review_star_cloudinary_refs'
)
BEGIN
    -- Cloudinary secure URL 典型格式：
    -- https://res.cloudinary.com/{cloud_name}/image/upload/v{version}/{public_id}.{ext}
    -- 嘗試從既有 URL 反解析出 public_id 回填；無法解析的舊列，
    -- 因為沒有 public_id 就無法安全刪除/替換該資產，直接把該欄位 URL 一併清空，
    -- 避免殘留「有 URL 沒有 public_id」的不完整資料擋住 WITH CHECK。

    -- 三個圖片欄位先轉成列，再共用同一套安全解析規則。
    -- LEFT 的長度即使遇到完全不合法的 URL 也只會是 0，不可能傳入負數。
    IF OBJECT_ID(N'tempdb..#review_cloudinary_backfill') IS NOT NULL
        DROP TABLE #review_cloudinary_backfill;

    CREATE TABLE #review_cloudinary_backfill
    (
        star_id int NOT NULL,
        image_slot tinyint NOT NULL,
        parsed_public_id nvarchar(255) NULL,
        PRIMARY KEY (star_id, image_slot)
    );

    INSERT INTO #review_cloudinary_backfill (star_id, image_slot, parsed_public_id)
    SELECT
        s.star_id,
        asset.image_slot,
        CASE
            WHEN length_info.safe_length BETWEEN 1 AND 255
            THEN LEFT(
                    parsed.public_path,
                    CASE WHEN length_info.safe_length BETWEEN 1 AND 255
                         THEN length_info.safe_length ELSE 0 END)
            ELSE NULL
        END
    FROM review.star AS s
    CROSS APPLY
    (
        VALUES
            (CONVERT(tinyint, 1), s.img_one, s.img_one_public_id),
            (CONVERT(tinyint, 2), s.img_two, s.img_two_public_id),
            (CONVERT(tinyint, 3), s.img_three, s.img_three_public_id)
    ) AS asset(image_slot, asset_url, current_public_id)
    CROSS APPLY
    (
        SELECT CHARINDEX(N'/upload/', asset.asset_url) AS upload_position
    ) AS upload_info
    CROSS APPLY
    (
        SELECT CASE
            WHEN upload_info.upload_position > 0
            THEN SUBSTRING(
                    asset.asset_url,
                    upload_info.upload_position + LEN(N'/upload/'),
                    LEN(asset.asset_url))
            ELSE NULL
        END AS after_upload
    ) AS uploaded
    CROSS APPLY
    (
        SELECT CASE
            WHEN uploaded.after_upload LIKE N'v[0-9]%/%'
            THEN SUBSTRING(
                    uploaded.after_upload,
                    CHARINDEX(N'/', uploaded.after_upload) + 1,
                    LEN(uploaded.after_upload))
            ELSE NULL
        END AS public_path
    ) AS parsed
    CROSS APPLY
    (
        SELECT CHARINDEX(N'.', REVERSE(parsed.public_path)) AS extension_position
    ) AS extension_info
    CROSS APPLY
    (
        SELECT CASE
            WHEN parsed.public_path IS NOT NULL
                 AND extension_info.extension_position > 0
            THEN LEN(parsed.public_path) - extension_info.extension_position
            ELSE 0
        END AS safe_length
    ) AS length_info
    WHERE asset.asset_url IS NOT NULL
      AND asset.current_public_id IS NULL;

    ;WITH parsed AS
    (
        SELECT
            star_id,
            MAX(CASE WHEN image_slot = 1 THEN parsed_public_id END) AS img_one_public_id,
            MAX(CASE WHEN image_slot = 2 THEN parsed_public_id END) AS img_two_public_id,
            MAX(CASE WHEN image_slot = 3 THEN parsed_public_id END) AS img_three_public_id
        FROM #review_cloudinary_backfill
        GROUP BY star_id
    )
    UPDATE s
    SET
        img_one_public_id = COALESCE(s.img_one_public_id, parsed.img_one_public_id),
        img_two_public_id = COALESCE(s.img_two_public_id, parsed.img_two_public_id),
        img_three_public_id = COALESCE(s.img_three_public_id, parsed.img_three_public_id)
    FROM review.star AS s
    INNER JOIN parsed ON parsed.star_id = s.star_id;

    DROP TABLE #review_cloudinary_backfill;

    -- 仍然無法回填 public_id 的孤兒 URL，直接清空該欄位，讓資料回到「未上傳」的合法狀態
    UPDATE review.star SET img_one   = NULL WHERE img_one   IS NOT NULL AND img_one_public_id   IS NULL;
    UPDATE review.star SET img_two   = NULL WHERE img_two   IS NOT NULL AND img_two_public_id   IS NULL;
    UPDATE review.star SET img_three = NULL WHERE img_three IS NOT NULL AND img_three_public_id IS NULL;

    -- 回填/清理完成後，改用 WITH CHECK 真正驗證既有資料，不再用 NOCHECK 跳過
    ALTER TABLE review.star WITH CHECK
        ADD CONSTRAINT CK_review_star_cloudinary_refs
        CHECK
        (
            (img_one IS NULL AND img_one_public_id IS NULL OR img_one IS NOT NULL AND img_one_public_id IS NOT NULL)
            AND (img_two IS NULL AND img_two_public_id IS NULL OR img_two IS NOT NULL AND img_two_public_id IS NOT NULL)
            AND (img_three IS NULL AND img_three_public_id IS NULL OR img_three IS NOT NULL AND img_three_public_id IS NOT NULL)
        );
END;
GO
-- //review-end，優化2 修正//

-- 約束若來自舊版 WITH NOCHECK，重新驗證既有資料並標記為 trusted。
ALTER TABLE review.star WITH CHECK CHECK CONSTRAINT CK_review_star_cloudinary_refs;
GO

IF NOT EXISTS
(
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'review.star')
      AND [name] = N'CK_review_star_five_star'
)
BEGIN
    ALTER TABLE review.star WITH CHECK
        ADD CONSTRAINT CK_review_star_five_star
        CHECK (five_star IS NULL OR five_star BETWEEN 1 AND 5);
END;
GO

ALTER TABLE review.star WITH CHECK CHECK CONSTRAINT CK_review_star_five_star;
GO

IF NOT EXISTS
(
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'review.star')
      AND [name] = N'CK_review_star_unreviewed_content'
)
BEGIN
    ALTER TABLE review.star WITH CHECK
        ADD CONSTRAINT CK_review_star_unreviewed_content
        CHECK
        (
            five_star IS NOT NULL
            OR
            (
                -- 嚴格要求 NULL；空字串或全空白字串不可偽裝成未評論內容。
                feedback IS NULL
                AND img_one IS NULL AND img_one_public_id IS NULL
                AND img_two IS NULL AND img_two_public_id IS NULL
                AND img_three IS NULL AND img_three_public_id IS NULL
            )
        );
END;
GO

ALTER TABLE review.star WITH CHECK CHECK CONSTRAINT CK_review_star_unreviewed_content;
GO

-- 優化6：同一個 filtered covering index 支援產品摘要單次聚合查詢。
-- 優化7：鍵順序符合 Offset 的 ORDER BY/OFFSET/FETCH；保留既有名稱以避免重建依賴。
IF NOT EXISTS
(
    SELECT 1 FROM sys.indexes
    WHERE object_id = OBJECT_ID(N'review.star')
      AND [name] = N'IX_review_star_product_keyset'
)
BEGIN
    CREATE INDEX IX_review_star_product_keyset
        ON review.star
        (
            product_id,
            review_priority DESC,
            star_upd_at DESC,
            star_id DESC
        )
        INCLUDE
        (
            history_id,
            order_item_id,
            product_name,
            image_url,
            base_price,
            five_star,
            feedback,
            img_one,
            img_two,
            img_three
        )
        WHERE five_star IS NOT NULL;
END;
GO
