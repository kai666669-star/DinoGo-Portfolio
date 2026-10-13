# DinoGo Database Schema

> Actual SQL Server database schema snapshot.
>
> Source: `0827.bacpac` → `model.xml`
> Database: `DinoGo`
> Generated: 2026-08-27
>
> 此文件反映產生當下的實際 SQL Server Schema。若 Java Entity、舊 ERD、舊 Markdown 或其他文件與本文件衝突，請先回報 schema mismatch，不得自行推測或修改任一方。

## Schema Summary

| Schema | Tables |
| --- | ---: |
| `ai` | 1 |
| `cart` | 3 |
| `catalog` | 6 |
| `member` | 6 |
| `msg` | 0 |
| `review` | 2 |
| `sales` | 6 |
| `seller` | 6 |
| `service` | 5 |
| `sysmsg` | 7 |
| **Total** | **42** |

## Table List

| Schema | Table |
| --- | --- |
| `ai` | `AiConversation` |
| `cart` | `Cart` |
| `cart` | `CartItem` |
| `cart` | `Favorite` |
| `catalog` | `Brand` |
| `catalog` | `Category` |
| `catalog` | `Product` |
| `catalog` | `ProductImage` |
| `catalog` | `ProductSku` |
| `catalog` | `Subcategory` |
| `member` | `Address` |
| `member` | `Member` |
| `member` | `MemberAccountStatusHistory` |
| `member` | `MemberOAuthAccount` |
| `member` | `MemberRole` |
| `member` | `Role` |
| `review` | `history` |
| `review` | `star` |
| `sales` | `OrderItem` |
| `sales` | `Orders` |
| `sales` | `Payment` |
| `sales` | `PaymentMethod` |
| `sales` | `Shipment` |
| `sales` | `ShipmentEvent` |
| `seller` | `Coupon` |
| `seller` | `MemberCoupon` |
| `seller` | `Seller` |
| `seller` | `SellerAiSalesAnalysis` |
| `seller` | `SellerApplication` |
| `seller` | `withdrawal_request` |
| `service` | `Demand` |
| `service` | `Reply` |
| `service` | `Role` |
| `service` | `Subtheme` |
| `service` | `Topic` |
| `sysmsg` | `msg_function_sequence` |
| `sysmsg` | `record` |
| `sysmsg` | `record_channel` |
| `sysmsg` | `send` |
| `sysmsg` | `send_disorder` |
| `sysmsg` | `send_order` |
| `sysmsg` | `send_seller` |

---

## `ai.AiConversation`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `conversation_id` | `int` | NO | YES |  | NO |
| `cloud_conversation_id` | `varchar(255)` | NO | NO |  | NO |
| `member_id` | `int` | NO | NO |  | NO |
| `provider` | `varchar(50)` | NO | NO |  | NO |
| `log_file_path` | `nvarchar(500)` | NO | NO |  | NO |
| `created_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `updated_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |

### Computed Columns
無。

### Primary Key
- `pk_ai_conversation`: `conversation_id`

### Foreign Keys
- `fk_ai_conversation_member`: `member_id` → `member.Member` (`member_id`)

### Unique Constraints
- `uq_ai_conversation_cloud_id`: `provider`, `cloud_conversation_id`

### Check Constraints
無。

### Default Constraints
- `df_ai_conversation_created_at` on `created_at`: `(sysdatetime())`
- `df_ai_conversation_updated_at` on `updated_at`: `(sysdatetime())`

### Indexes

無。

---

## `cart.Cart`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `cart_id` | `int` | NO | YES |  | NO |
| `member_id` | `int` | NO | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `pk_cart`: `cart_id`

### Foreign Keys
- `fk_cart_member`: `member_id` → `member.Member` (`member_id`)

### Unique Constraints
- `uq_cart_member`: `member_id`

### Check Constraints
無。

### Default Constraints
無。

### Indexes

無。

---

## `cart.CartItem`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `cart_item_id` | `int` | NO | YES |  | NO |
| `cart_id` | `int` | NO | NO |  | NO |
| `sku_id` | `int` | NO | NO |  | NO |
| `quantity` | `int` | NO | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `pk_cart_item`: `cart_item_id`

### Foreign Keys
- `fk_cart_item_cart`: `cart_id` → `cart.Cart` (`cart_id`)
- `fk_cart_item_sku`: `sku_id` → `catalog.ProductSku` (`sku_id`)

### Unique Constraints
- `uq_cart_item_cart_sku`: `cart_id`, `sku_id`

### Check Constraints
- `ck_cart_item_quantity`: `[quantity]>(0)`

### Default Constraints
無。

### Indexes

無。

---

## `cart.Favorite`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `favorite_id` | `int` | NO | YES |  | NO |
| `member_id` | `int` | NO | NO |  | NO |
| `product_id` | `int` | NO | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `pk_favorite`: `favorite_id`

### Foreign Keys
- `fk_favorite_member`: `member_id` → `member.Member` (`member_id`)
- `fk_favorite_product`: `product_id` → `catalog.Product` (`product_id`)

### Unique Constraints
- `uq_favorite_member_product`: `member_id`, `product_id`

### Check Constraints
無。

### Default Constraints
無。

### Indexes

無。

---

## `catalog.Brand`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `brand_id` | `int` | NO | YES |  | NO |
| `brand_name` | `nvarchar(100)` | NO | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `pk_brand`: `brand_id`

### Foreign Keys
無。

### Unique Constraints
- `uq_brand_brand_name`: `brand_name`

### Check Constraints
無。

### Default Constraints
無。

### Indexes

無。

---

## `catalog.Category`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `category_id` | `int` | NO | YES |  | NO |
| `category_name` | `nvarchar(100)` | NO | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `pk_category`: `category_id`

### Foreign Keys
無。

### Unique Constraints
- `uk_category_name`: `category_name`
- `uq_category_name`: `category_name`

### Check Constraints
無。

### Default Constraints
無。

### Indexes

無。

---

## `catalog.Product`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `product_id` | `int` | NO | YES |  | NO |
| `seller_id` | `int` | NO | NO |  | NO |
| `subcategory_id` | `int` | NO | NO |  | NO |
| `brand_id` | `int` | NO | NO |  | NO |
| `product_name` | `nvarchar(50)` | NO | NO |  | NO |
| `description` | `nvarchar(3000)` | YES | NO |  | NO |
| `base_price` | `decimal(10,2)` | NO | NO |  | NO |
| `status` | `tinyint` | NO | NO | ((0)) | NO |
| `view_count` | `int` | NO | NO | ((0)) | NO |
| `sold_count` | `int` | NO | NO | ((0)) | NO |
| `created_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `updated_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |

### Computed Columns
無。

### Primary Key
- `pk_product`: `product_id`

### Foreign Keys
- `fk_product_brand`: `brand_id` → `catalog.Brand` (`brand_id`)
- `fk_product_seller`: `seller_id` → `seller.Seller` (`seller_id`)
- `fk_product_subcategory`: `subcategory_id` → `catalog.Subcategory` (`subcategory_id`)
- `FKku369nri8u3s17uom8or57trs`: `subcategory_id` → `catalog.Subcategory` (`subcategory_id`)
- `FKs6cydsualtsrprvlf2bb3lcam`: `brand_id` → `catalog.Brand` (`brand_id`)

### Unique Constraints
無。

### Check Constraints
- `ck_product_nonnegative_values`: `[base_price]>=(0) AND [view_count]>=(0) AND [sold_count]>=(0)`
- `ck_product_status`: `[status]=(3) OR [status]=(2) OR [status]=(1) OR [status]=(0)`

### Default Constraints
- `df_product_created_at` on `created_at`: `(sysdatetime())`
- `df_product_sold_count` on `sold_count`: `((0))`
- `df_product_status` on `status`: `((0))`
- `df_product_updated_at` on `updated_at`: `(sysdatetime())`
- `df_product_view_count` on `view_count`: `((0))`

### Indexes

無。

---

## `catalog.ProductImage`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `image_id` | `int` | NO | YES |  | NO |
| `product_id` | `int` | NO | NO |  | NO |
| `image_url` | `nvarchar(255)` | NO | NO |  | NO |
| `sort_order` | `int` | NO | NO |  | NO |
| `is_main` | `bit` | NO | NO | ((0)) | NO |

### Computed Columns
無。

### Primary Key
- `pk_product_image`: `image_id`

### Foreign Keys
- `fk_product_image_product`: `product_id` → `catalog.Product` (`product_id`)

### Unique Constraints
- `uq_product_image_sort_order`: `product_id`, `sort_order`

### Check Constraints
無。

### Default Constraints
- `df_product_image_is_main` on `is_main`: `((0))`

### Indexes

| Name | Unique | Clustered | Key columns | Included columns | Filter |
| --- | ---: | ---: | --- | --- | --- |
| `uq_product_image_one_main` | YES | NO | `product_id` ASC |  | ([is_main]=(1)) |

---

## `catalog.ProductSku`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `sku_id` | `int` | NO | YES |  | NO |
| `product_id` | `int` | NO | NO |  | NO |
| `spec1_name` | `nvarchar(30)` | YES | NO |  | NO |
| `spec1_value` | `nvarchar(50)` | YES | NO |  | NO |
| `spec2_name` | `nvarchar(30)` | YES | NO |  | NO |
| `spec2_value` | `nvarchar(50)` | YES | NO |  | NO |
| `price` | `decimal(10,2)` | NO | NO |  | NO |
| `stock` | `int` | NO | NO |  | NO |
| `status` | `tinyint` | NO | NO | ((1)) | NO |

### Computed Columns
無。

### Primary Key
- `pk_product_sku`: `sku_id`

### Foreign Keys
- `fk_product_sku_product`: `product_id` → `catalog.Product` (`product_id`)

### Unique Constraints
- `uq_product_sku_sku_product`: `sku_id`, `product_id`

### Check Constraints
- `ck_product_sku_price`: `[price]>=(0)`
- `ck_product_sku_stock`: `[stock]>=(0)`

### Default Constraints
- `df_product_sku_status` on `status`: `((1))`

### Indexes

無。

---

## `catalog.Subcategory`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `subcategory_id` | `int` | NO | YES |  | NO |
| `category_id` | `int` | NO | NO |  | NO |
| `subcategory_name` | `nvarchar(100)` | NO | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `pk_subcategory`: `subcategory_id`

### Foreign Keys
- `fk_subcategory_category`: `category_id` → `catalog.Category` (`category_id`)
- `FKe4hdbsmrx9bs9gpj1fh4mg0ku`: `category_id` → `catalog.Category` (`category_id`)

### Unique Constraints
- `uk_subcategory_category_name`: `category_id`, `subcategory_name`
- `uq_subcategory_category_name`: `category_id`, `subcategory_name`

### Check Constraints
無。

### Default Constraints
無。

### Indexes

無。

---

## `member.Address`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `address_id` | `int` | NO | YES |  | NO |
| `member_id` | `int` | NO | NO |  | NO |
| `receiver_name` | `nvarchar(100)` | NO | NO |  | NO |
| `receiver_phone` | `varchar(20)` | NO | NO |  | NO |
| `postal_code` | `varchar(10)` | YES | NO |  | NO |
| `city` | `nvarchar(10)` | NO | NO |  | NO |
| `district` | `nvarchar(10)` | NO | NO |  | NO |
| `detail_address` | `nvarchar(255)` | NO | NO |  | NO |
| `is_default` | `bit` | NO | NO | ((0)) | NO |

### Computed Columns
無。

### Primary Key
- `pk_address`: `address_id`

### Foreign Keys
- `fk_address_member`: `member_id` → `member.Member` (`member_id`)

### Unique Constraints
無。

### Check Constraints
無。

### Default Constraints
- `df_address_is_default` on `is_default`: `((0))`

### Indexes

| Name | Unique | Clustered | Key columns | Included columns | Filter |
| --- | ---: | ---: | --- | --- | --- |
| `uq_address_one_default_per_member` | YES | NO | `member_id` ASC |  | ([is_default]=(1)) |

---

## `member.Member`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `member_id` | `int` | NO | YES |  | NO |
| `email` | `varchar(100)` | NO | NO |  | NO |
| `password_hash` | `varchar(255)` | NO | NO |  | NO |
| `last_name` | `nvarchar(50)` | NO | NO |  | NO |
| `first_name` | `nvarchar(50)` | NO | NO |  | NO |
| `birth_date` | `date` | YES | NO |  | NO |
| `phone` | `varchar(20)` | YES | NO |  | NO |
| `status` | `varchar(20)` | NO | NO | ('ACTIVE') | NO |
| `created_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `updated_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `auth_version` | `int` | NO | NO | ((0)) | NO |
| `email_order_notifications` | `bit` | NO | NO | ((1)) | NO |
| `email_marketing_notifications` | `bit` | NO | NO | ((0)) | NO |

### Computed Columns
無。

### Primary Key
- `pk_member`: `member_id`

### Foreign Keys
無。

### Unique Constraints
- `uq_member_email`: `email`

### Check Constraints
- `ck_member_status`: `[status]='DEACTIVATED' OR [status]='SUSPENDED' OR [status]='ACTIVE'`

### Default Constraints
- `df_member_auth_version` on `auth_version`: `((0))`
- `df_member_created_at` on `created_at`: `(sysdatetime())`
- `df_member_email_marketing_notifications` on `email_marketing_notifications`: `((0))`
- `df_member_email_order_notifications` on `email_order_notifications`: `((1))`
- `df_member_status` on `status`: `('ACTIVE')`
- `df_member_updated_at` on `updated_at`: `(sysdatetime())`

### Indexes

無。

---

## `member.MemberAccountStatusHistory`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `history_id` | `int` | NO | YES |  | NO |
| `member_id` | `int` | NO | NO |  | NO |
| `previous_status` | `varchar(20)` | NO | NO |  | NO |
| `new_status` | `varchar(20)` | NO | NO |  | NO |
| `reason` | `nvarchar(500)` | YES | NO |  | NO |
| `changed_by` | `int` | YES | NO |  | NO |
| `changed_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |

### Computed Columns
無。

### Primary Key
- `pk_member_account_status_history`: `history_id`

### Foreign Keys
- `fk_member_account_status_history_changed_by`: `changed_by` → `member.Member` (`member_id`)
- `fk_member_account_status_history_member`: `member_id` → `member.Member` (`member_id`)

### Unique Constraints
無。

### Check Constraints
無。

### Default Constraints
- `df_member_account_status_history_changed_at` on `changed_at`: `(sysdatetime())`

### Indexes

| Name | Unique | Clustered | Key columns | Included columns | Filter |
| --- | ---: | ---: | --- | --- | --- |
| `ix_member_account_status_history_member_changed_at` | NO | NO | `member_id` ASC, `changed_at` ASC |  |  |

---

## `member.MemberOAuthAccount`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `oauth_account_id` | `int` | NO | YES |  | NO |
| `member_id` | `int` | NO | NO |  | NO |
| `provider` | `varchar(30)` | NO | NO |  | NO |
| `provider_user_id` | `varchar(255)` | NO | NO |  | NO |
| `provider_email` | `varchar(255)` | YES | NO |  | NO |
| `created_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `updated_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |

### Computed Columns
無。

### Primary Key
- `pk_member_oauth_account`: `oauth_account_id`

### Foreign Keys
- `fk_member_oauth_account_member`: `member_id` → `member.Member` (`member_id`)

### Unique Constraints
- `uq_member_oauth_member_provider`: `member_id`, `provider`
- `uq_member_oauth_provider_user`: `provider`, `provider_user_id`

### Check Constraints
無。

### Default Constraints
- `df_member_oauth_account_created_at` on `created_at`: `(sysdatetime())`
- `df_member_oauth_account_updated_at` on `updated_at`: `(sysdatetime())`

### Indexes

| Name | Unique | Clustered | Key columns | Included columns | Filter |
| --- | ---: | ---: | --- | --- | --- |
| `ix_member_oauth_account_member_id` | NO | NO | `member_id` ASC |  |  |

---

## `member.MemberRole`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `member_id` | `int` | NO | NO |  | NO |
| `role_id` | `int` | NO | NO |  | NO |
| `assigned_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |

### Computed Columns
無。

### Primary Key
- `pk_member_role`: `member_id`, `role_id`

### Foreign Keys
- `fk_member_role_member`: `member_id` → `member.Member` (`member_id`)
- `fk_member_role_role`: `role_id` → `member.Role` (`role_id`)

### Unique Constraints
無。

### Check Constraints
無。

### Default Constraints
- `df_member_role_assigned_at` on `assigned_at`: `(sysdatetime())`

### Indexes

無。

---

## `member.Role`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `role_id` | `int` | NO | YES |  | NO |
| `role_name` | `varchar(50)` | NO | NO |  | NO |
| `description` | `nvarchar(100)` | YES | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `pk_role`: `role_id`

### Foreign Keys
無。

### Unique Constraints
- `uq_role_role_name`: `role_name`

### Check Constraints
無。

### Default Constraints
無。

### Indexes

無。

---

## `review.history`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `history_id` | `int` | NO | YES |  | NO |
| `seller_id` | `int` | NO | NO |  | NO |
| `member_id` | `int` | NO | NO |  | NO |
| `order_id` | `int` | NO | NO |  | NO |
| `order_no` | `nvarchar(30)` | NO | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `PK_review_history`: `history_id`

### Foreign Keys
無。

### Unique Constraints
- `UQ_review_history_order`: `order_id`

### Check Constraints
無。

### Default Constraints
無。

### Indexes

無。

---

## `review.star`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `star_id` | `int` | NO | YES |  | NO |
| `history_id` | `int` | NO | NO |  | NO |
| `order_item_id` | `int` | NO | NO |  | NO |
| `product_id` | `int` | NO | NO |  | NO |
| `product_name` | `nvarchar(100)` | NO | NO |  | NO |
| `image_url` | `nvarchar(500)` | YES | NO |  | NO |
| `base_price` | `decimal(12,2)` | NO | NO |  | NO |
| `img_one` | `varbinary` | YES | NO |  | NO |
| `img_two` | `varbinary` | YES | NO |  | NO |
| `img_three` | `varbinary` | YES | NO |  | NO |
| `feedback` | `nvarchar(500)` | YES | NO |  | NO |
| `five_star` | `int` | YES | NO |  | NO |
| `version` | `bigint` | NO | NO | ((0)) | NO |
| `star_upd_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `review_priority` | `computed` | YES | NO |  | YES |

### Computed Columns
- `review_priority`: `(case when [feedback] IS NOT NULL AND ltrim(rtrim([feedback]))<>N'' AND [img_one] IS NOT NULL AND datalength([img_one])>(0) then CONVERT([tinyint],(2)) when [feedback] IS NOT NULL AND ltrim(rtrim([feedback]))<>N'' OR [img_one] IS NOT NULL AND datalength([img_one])>(0) then CONVERT([tinyint],(1)) else CONVERT([tinyint],(0)) end)` (persisted: YES)

### Primary Key
- `PK_review_star`: `star_id`

### Foreign Keys
- `FK_review_star_history`: `history_id` → `review.history` (`history_id`)

### Unique Constraints
- `UQ_review_star_history_order_item`: `history_id`, `order_item_id`

### Check Constraints
- `CK_review_star_five_star`: `[five_star] IS NULL OR [five_star]>=(1) AND [five_star]<=(5)`
- `CK_review_star_unreviewed_content`: `[five_star] IS NOT NULL OR [feedback] IS NULL AND [img_one] IS NULL AND [img_two] IS NULL AND [img_three] IS NULL`

### Default Constraints
- `DF_review_star_updated_at` on `star_upd_at`: `(sysdatetime())`
- `DF_review_star_version` on `version`: `((0))`

### Indexes

| Name | Unique | Clustered | Key columns | Included columns | Filter |
| --- | ---: | ---: | --- | --- | --- |
| `IX_review_star_product_keyset` | NO | NO | `product_id` ASC, `review_priority` ASC, `star_upd_at` ASC, `star_id` ASC |  | ([five_star] IS NOT NULL) |

---

## `sales.OrderItem`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `order_item_id` | `int` | NO | YES |  | NO |
| `order_id` | `int` | NO | NO |  | NO |
| `product_id` | `int` | NO | NO |  | NO |
| `sku_id` | `int` | NO | NO |  | NO |
| `product_name` | `nvarchar(100)` | NO | NO |  | NO |
| `sku_spec` | `nvarchar(200)` | YES | NO |  | NO |
| `product_image_url` | `nvarchar(500)` | YES | NO |  | NO |
| `unit_price` | `decimal(12,2)` | NO | NO |  | NO |
| `quantity` | `int` | NO | NO |  | NO |
| `subtotal` | `decimal(12,2)` | NO | NO |  | NO |
| `is_reviewed` | `bit` | NO | NO | ((0)) | NO |
| `created_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |

### Computed Columns
無。

### Primary Key
- `pk_order_item`: `order_item_id`

### Foreign Keys
- `fk_order_item_orders`: `order_id` → `sales.Orders` (`order_id`)
- `fk_order_item_product`: `product_id` → `catalog.Product` (`product_id`)
- `fk_order_item_sku`: `sku_id` → `catalog.ProductSku` (`sku_id`)
- `fk_order_item_sku_product`: `sku_id`, `product_id` → `catalog.ProductSku` (`sku_id`, `product_id`)

### Unique Constraints
無。

### Check Constraints
- `ck_order_item_amount`: `[unit_price]>=(0) AND [subtotal]>=(0)`
- `ck_order_item_amounts`: `[unit_price]>=(0) AND [subtotal]>=(0) AND [subtotal]=[unit_price]*[quantity]`
- `ck_order_item_quantity`: `[quantity]>(0)`

### Default Constraints
- `df_order_item_created_at` on `created_at`: `(sysdatetime())`
- `df_order_item_is_reviewed` on `is_reviewed`: `((0))`

### Indexes

無。

---

## `sales.Orders`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `order_id` | `int` | NO | YES |  | NO |
| `order_no` | `varchar(30)` | NO | NO |  | NO |
| `buyer_id` | `int` | NO | NO |  | NO |
| `seller_id` | `int` | NO | NO |  | NO |
| `address_id` | `int` | YES | NO |  | NO |
| `receiver_name` | `nvarchar(100)` | NO | NO |  | NO |
| `receiver_phone` | `varchar(20)` | NO | NO |  | NO |
| `shipping_postal_code` | `varchar(10)` | YES | NO |  | NO |
| `shipping_city` | `nvarchar(50)` | NO | NO |  | NO |
| `shipping_district` | `nvarchar(50)` | NO | NO |  | NO |
| `shipping_detail_address` | `nvarchar(255)` | NO | NO |  | NO |
| `status` | `varchar(30)` | NO | NO | ('PENDING_PAYMENT') | NO |
| `subtotal_amount` | `decimal(12,2)` | NO | NO |  | NO |
| `shipping_fee` | `decimal(12,2)` | NO | NO | ((0)) | NO |
| `discount_amount` | `decimal(12,2)` | NO | NO | ((0)) | NO |
| `total_amount` | `decimal(12,2)` | NO | NO |  | NO |
| `buyer_remark` | `nvarchar(500)` | YES | NO |  | NO |
| `cancel_reason` | `nvarchar(500)` | YES | NO |  | NO |
| `cancelled_by` | `varchar(20)` | YES | NO |  | NO |
| `cancelled_at` | `datetime2(7)` | YES | NO |  | NO |
| `completed_at` | `datetime2(7)` | YES | NO |  | NO |
| `created_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `updated_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `member_coupon_id` | `int` | YES | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `pk_orders`: `order_id`

### Foreign Keys
- `fk_orders_address`: `address_id` → `member.Address` (`address_id`)
- `fk_orders_buyer`: `buyer_id` → `member.Member` (`member_id`)
- `fk_orders_member_coupon_buyer`: `member_coupon_id`, `buyer_id` → `seller.MemberCoupon` (`member_coupon_id`, `member_id`)
- `fk_orders_seller`: `seller_id` → `seller.Seller` (`seller_id`)

### Unique Constraints
- `uq_orders_order_no`: `order_no`

### Check Constraints
- `ck_orders_amount`: `[subtotal_amount]>=(0) AND [shipping_fee]>=(0) AND [discount_amount]>=(0) AND [total_amount]>=(0)`
- `ck_orders_amounts`: `[subtotal_amount]>=(0) AND [shipping_fee]>=(0) AND [discount_amount]>=(0) AND [total_amount]>=(0) AND [total_amount]=(([subtotal_amount]+[shipping_fee])-[discount_amount])`
- `ck_orders_cancelled_by`: `[cancelled_by] IS NULL OR ([cancelled_by]='SYSTEM' OR [cancelled_by]='SELLER' OR [cancelled_by]='BUYER')`
- `ck_orders_status`: `[status]='CANCELLED' OR [status]='COMPLETED' OR [status]='DELIVERED' OR [status]='SHIPPED' OR [status]='PROCESSING' OR [status]='PAID' OR [status]='PENDING_PAYMENT'`

### Default Constraints
- `df_orders_created_at` on `created_at`: `(sysdatetime())`
- `df_orders_discount_amount` on `discount_amount`: `((0))`
- `df_orders_shipping_fee` on `shipping_fee`: `((0))`
- `df_orders_status` on `status`: `('PENDING_PAYMENT')`
- `df_orders_updated_at` on `updated_at`: `(sysdatetime())`

### Indexes

無。

---

## `sales.Payment`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `payment_id` | `int` | NO | YES |  | NO |
| `payment_no` | `varchar(40)` | NO | NO |  | NO |
| `order_id` | `int` | NO | NO |  | NO |
| `payment_method_id` | `int` | NO | NO |  | NO |
| `amount` | `decimal(12,2)` | NO | NO |  | NO |
| `status` | `varchar(20)` | NO | NO | ('PENDING') | NO |
| `transaction_no` | `varchar(100)` | YES | NO |  | NO |
| `failure_reason` | `nvarchar(255)` | YES | NO |  | NO |
| `paid_at` | `datetime2(7)` | YES | NO |  | NO |
| `created_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `updated_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `idempotency_key` | `varchar(64)` | NO | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `pk_payment`: `payment_id`

### Foreign Keys
- `fk_payment_orders`: `order_id` → `sales.Orders` (`order_id`)
- `fk_payment_payment_method`: `payment_method_id` → `sales.PaymentMethod` (`payment_method_id`)

### Unique Constraints
- `uq_payment_order_idempotency_key`: `order_id`, `idempotency_key`
- `uq_payment_payment_no`: `payment_no`

### Check Constraints
- `ck_payment_amount`: `[amount]>=(0)`
- `ck_payment_status`: `[status]='CANCELLED' OR [status]='FAILED' OR [status]='SUCCESS' OR [status]='PENDING'`

### Default Constraints
- `df_payment_created_at` on `created_at`: `(sysdatetime())`
- `df_payment_status` on `status`: `('PENDING')`
- `df_payment_updated_at` on `updated_at`: `(sysdatetime())`

### Indexes

| Name | Unique | Clustered | Key columns | Included columns | Filter |
| --- | ---: | ---: | --- | --- | --- |
| `uq_payment_one_success_per_order` | YES | NO | `order_id` ASC |  | ([status]='SUCCESS') |

---

## `sales.PaymentMethod`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `payment_method_id` | `int` | NO | YES |  | NO |
| `method_code` | `varchar(30)` | NO | NO |  | NO |
| `method_name` | `nvarchar(50)` | NO | NO |  | NO |
| `created_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `updated_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |

### Computed Columns
無。

### Primary Key
- `pk_payment_method`: `payment_method_id`

### Foreign Keys
無。

### Unique Constraints
- `uq_payment_method_method_code`: `method_code`

### Check Constraints
無。

### Default Constraints
- `df_payment_method_created_at` on `created_at`: `(sysdatetime())`
- `df_payment_method_updated_at` on `updated_at`: `(sysdatetime())`

### Indexes

無。

---

## `sales.Shipment`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `shipment_id` | `int` | NO | YES |  | NO |
| `order_id` | `int` | NO | NO |  | NO |
| `carrier_name` | `nvarchar(100)` | YES | NO |  | NO |
| `tracking_no` | `varchar(100)` | YES | NO |  | NO |
| `status` | `varchar(30)` | NO | NO | ('PREPARING') | NO |
| `shipped_at` | `datetime2(7)` | YES | NO |  | NO |
| `available_pickup_at` | `datetime2(7)` | YES | NO |  | NO |
| `delivered_at` | `datetime2(7)` | YES | NO |  | NO |
| `delivery_photo_url` | `nvarchar(500)` | YES | NO |  | NO |
| `created_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `updated_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |

### Computed Columns
無。

### Primary Key
- `pk_shipment`: `shipment_id`

### Foreign Keys
- `fk_shipment_orders`: `order_id` → `sales.Orders` (`order_id`)

### Unique Constraints
- `uq_shipment_order`: `order_id`

### Check Constraints
- `ck_shipment_status`: `[status]='DELIVERED' OR [status]='AVAILABLE_FOR_PICKUP' OR [status]='SHIPPED' OR [status]='PREPARING'`

### Default Constraints
- `df_shipment_created_at` on `created_at`: `(sysdatetime())`
- `df_shipment_status` on `status`: `('PREPARING')`
- `df_shipment_updated_at` on `updated_at`: `(sysdatetime())`

### Indexes

無。

---

## `sales.ShipmentEvent`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `shipment_event_id` | `int` | NO | YES |  | NO |
| `shipment_id` | `int` | NO | NO |  | NO |
| `event_type` | `varchar(30)` | NO | NO |  | NO |
| `source` | `varchar(20)` | NO | NO |  | NO |
| `remark` | `nvarchar(500)` | YES | NO |  | NO |
| `occurred_at` | `datetime2(7)` | NO | NO |  | NO |
| `created_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |

### Computed Columns
無。

### Primary Key
- `pk_shipment_event`: `shipment_event_id`

### Foreign Keys
- `fk_shipment_event_shipment`: `shipment_id` → `sales.Shipment` (`shipment_id`)

### Unique Constraints
無。

### Check Constraints
- `ck_shipment_event_source`: `[source]='BUYER' OR [source]='SYSTEM' OR [source]='CARRIER' OR [source]='SELLER'`
- `ck_shipment_event_type`: `[event_type]='DELIVERED' OR [event_type]='AVAILABLE_FOR_PICKUP' OR [event_type]='OUT_FOR_DELIVERY' OR [event_type]='IN_TRANSIT' OR [event_type]='HANDED_OVER' OR [event_type]='LABEL_CREATED'`

### Default Constraints
- `df_shipment_event_created_at` on `created_at`: `(sysdatetime())`

### Indexes

| Name | Unique | Clustered | Key columns | Included columns | Filter |
| --- | ---: | ---: | --- | --- | --- |
| `ix_shipment_event_shipment_occurred` | NO | NO | `shipment_id` ASC, `occurred_at` ASC |  |  |

---

## `seller.Coupon`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `coupon_id` | `int` | NO | YES |  | NO |
| `seller_id` | `int` | NO | NO |  | NO |
| `coupon_code` | `varchar(100)` | NO | NO |  | NO |
| `coupon_name` | `nvarchar(100)` | NO | NO |  | NO |
| `discount_type` | `varchar(30)` | NO | NO |  | NO |
| `discount_value` | `decimal(18,2)` | NO | NO |  | NO |
| `min_purchase_amount` | `decimal(18,2)` | YES | NO |  | NO |
| `start_at` | `datetime2(7)` | NO | NO |  | NO |
| `end_at` | `datetime2(7)` | NO | NO |  | NO |
| `limit_count` | `int` | YES | NO |  | NO |
| `used_count` | `int` | NO | NO | ((0)) | NO |
| `per_member_usage_policy` | `varchar(20)` | NO | NO | ('ONCE') | NO |
| `scope_type` | `varchar(30)` | NO | NO |  | NO |
| `category_id` | `int` | YES | NO |  | NO |
| `product_id` | `int` | YES | NO |  | NO |
| `status` | `varchar(30)` | NO | NO |  | NO |
| `created_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `updated_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `subcategory_id` | `int` | YES | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `pk_coupon`: `coupon_id`

### Foreign Keys
- `fk_coupon_category`: `category_id` → `catalog.Category` (`category_id`)
- `fk_coupon_product`: `product_id` → `catalog.Product` (`product_id`)
- `fk_coupon_seller`: `seller_id` → `seller.Seller` (`seller_id`)
- `fk_coupon_subcategory`: `subcategory_id` → `catalog.Subcategory` (`subcategory_id`)

### Unique Constraints
- `uq_coupon_seller_code`: `seller_id`, `coupon_code`

### Check Constraints
- `ck_coupon_discount_type`: `[discount_type]='AMOUNT' OR [discount_type]='PERCENT'`
- `ck_coupon_discount_value`: `[discount_value]>(0)`
- `ck_coupon_limit_count`: `[limit_count] IS NULL OR [limit_count]>(0)`
- `ck_coupon_percent_range`: `[discount_type]<>'PERCENT' OR [discount_value]<=(100)`
- `ck_coupon_per_member_usage_policy`: `[per_member_usage_policy]='REPEAT' OR [per_member_usage_policy]='ONCE'`
- `ck_coupon_scope_fields_v2`: `[scope_type]='STORE' AND [category_id] IS NULL AND [subcategory_id] IS NULL AND [product_id] IS NULL OR [scope_type]='CATEGORY' AND [category_id] IS NOT NULL AND [subcategory_id] IS NULL AND [product_id] IS NULL OR [scope_type]='SUBCATEGORY' AND [category_id] IS NULL AND [subcategory_id] IS NOT NULL AND [product_id] IS NULL OR [scope_type]='PRODUCT' AND [category_id] IS NULL AND [subcategory_id] IS NULL AND [product_id] IS NOT NULL`
- `ck_coupon_scope_type_v2`: `[scope_type]='PRODUCT' OR [scope_type]='SUBCATEGORY' OR [scope_type]='CATEGORY' OR [scope_type]='STORE'`
- `ck_coupon_status`: `[status]='EXPIRED' OR [status]='DISABLED' OR [status]='ACTIVE' OR [status]='DRAFT'`
- `ck_coupon_usage`: `([min_purchase_amount] IS NULL OR [min_purchase_amount]>=(0)) AND [used_count]>=(0)`
- `ck_coupon_valid_time`: `[end_at]>[start_at]`

### Default Constraints
- `df_coupon_created_at` on `created_at`: `(sysdatetime())`
- `df_coupon_per_member_usage_policy` on `per_member_usage_policy`: `('ONCE')`
- `df_coupon_updated_at` on `updated_at`: `(sysdatetime())`
- `df_coupon_used_count` on `used_count`: `((0))`

### Indexes

| Name | Unique | Clustered | Key columns | Included columns | Filter |
| --- | ---: | ---: | --- | --- | --- |
| `ix_coupon_status` | NO | NO | `status` ASC |  |  |

---

## `seller.MemberCoupon`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `member_coupon_id` | `int` | NO | YES |  | NO |
| `coupon_id` | `int` | NO | NO |  | NO |
| `member_id` | `int` | NO | NO |  | NO |
| `is_used` | `bit` | NO | NO | ((0)) | NO |
| `used_at` | `datetime2(7)` | YES | NO |  | NO |
| `received_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |

### Computed Columns
無。

### Primary Key
- `pk_member_coupon`: `member_coupon_id`

### Foreign Keys
- `fk_member_coupon_coupon`: `coupon_id` → `seller.Coupon` (`coupon_id`)
- `fk_member_coupon_member`: `member_id` → `member.Member` (`member_id`)

### Unique Constraints
- `uq_member_coupon`: `coupon_id`, `member_id`
- `uq_member_coupon_id_member`: `member_coupon_id`, `member_id`

### Check Constraints
- `ck_member_coupon_used_state`: `[is_used]=(0) AND [used_at] IS NULL OR [is_used]=(1) AND [used_at] IS NOT NULL`

### Default Constraints
- `df_member_coupon_is_used` on `is_used`: `((0))`
- `df_member_coupon_received_at` on `received_at`: `(sysdatetime())`

### Indexes

| Name | Unique | Clustered | Key columns | Included columns | Filter |
| --- | ---: | ---: | --- | --- | --- |
| `ix_member_coupon_coupon_id` | NO | NO | `coupon_id` ASC |  |  |
| `ix_member_coupon_member_id` | NO | NO | `member_id` ASC |  |  |

---

## `seller.Seller`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `seller_id` | `int` | NO | YES |  | NO |
| `member_id` | `int` | NO | NO |  | NO |
| `store_name` | `nvarchar(100)` | NO | NO |  | NO |
| `store_description` | `nvarchar(500)` | YES | NO |  | NO |
| `store_logo_url` | `varchar(255)` | YES | NO |  | NO |
| `status` | `varchar(30)` | NO | NO |  | NO |
| `created_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `updated_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `service_start_time` | `time(7)` | YES | NO |  | NO |
| `service_end_time` | `time(7)` | YES | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `pk_seller`: `seller_id`

### Foreign Keys
- `fk_seller_member`: `member_id` → `member.Member` (`member_id`)

### Unique Constraints
- `uq_seller_member`: `member_id`

### Check Constraints
無。

### Default Constraints
- `df_seller_created_at` on `created_at`: `(sysdatetime())`
- `df_seller_updated_at` on `updated_at`: `(sysdatetime())`

### Indexes

| Name | Unique | Clustered | Key columns | Included columns | Filter |
| --- | ---: | ---: | --- | --- | --- |
| `uq_seller_store_name` | YES | NO | `store_name` ASC |  |  |

---

## `seller.SellerAiSalesAnalysis`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `analysis_id` | `int` | NO | YES |  | NO |
| `seller_id` | `int` | NO | NO |  | NO |
| `analysis_period_start` | `date` | NO | NO |  | NO |
| `analysis_period_end` | `date` | NO | NO |  | NO |
| `revenue_amount` | `decimal(12,2)` | NO | NO | ((0)) | NO |
| `order_count` | `int` | NO | NO | ((0)) | NO |
| `product_count` | `int` | NO | NO | ((0)) | NO |
| `used_coupon_count` | `int` | NO | NO | ((0)) | NO |
| `top_product_summary` | `nvarchar(500)` | YES | NO |  | NO |
| `coupon_summary` | `nvarchar(500)` | YES | NO |  | NO |
| `risk_summary` | `nvarchar(500)` | YES | NO |  | NO |
| `ai_summary` | `nvarchar(1000)` | NO | NO |  | NO |
| `ai_recommendation` | `nvarchar(1000)` | YES | NO |  | NO |
| `model_name` | `varchar(100)` | YES | NO |  | NO |
| `generated_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `created_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `period_type` | `varchar(20)` | YES | NO |  | NO |
| `period_year` | `int` | YES | NO |  | NO |
| `period_no` | `int` | YES | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `pk_seller_ai_sales_analysis`: `analysis_id`

### Foreign Keys
- `fk_seller_ai_sales_analysis_seller`: `seller_id` → `seller.Seller` (`seller_id`)

### Unique Constraints
- `uq_seller_ai_sales_analysis_period`: `seller_id`, `period_type`, `period_year`, `period_no`

### Check Constraints
- `ck_seller_ai_sales_analysis_order_count`: `[order_count]>=(0)`
- `ck_seller_ai_sales_analysis_period`: `[analysis_period_end]>=[analysis_period_start]`
- `ck_seller_ai_sales_analysis_period_type`: `[period_type]='YEARLY' OR [period_type]='QUARTERLY' OR [period_type]='MONTHLY'`
- `ck_seller_ai_sales_analysis_product_count`: `[product_count]>=(0)`
- `ck_seller_ai_sales_analysis_revenue`: `[revenue_amount]>=(0)`
- `ck_seller_ai_sales_analysis_used_coupon_count`: `[used_coupon_count]>=(0)`

### Default Constraints
- `df_seller_ai_sales_analysis_created_at` on `created_at`: `(sysdatetime())`
- `df_seller_ai_sales_analysis_generated_at` on `generated_at`: `(sysdatetime())`
- `df_seller_ai_sales_analysis_order_count` on `order_count`: `((0))`
- `df_seller_ai_sales_analysis_product_count` on `product_count`: `((0))`
- `df_seller_ai_sales_analysis_revenue` on `revenue_amount`: `((0))`
- `df_seller_ai_sales_analysis_used_coupon_count` on `used_coupon_count`: `((0))`

### Indexes

無。

---

## `seller.SellerApplication`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `application_id` | `int` | NO | YES |  | NO |
| `member_id` | `int` | NO | NO |  | NO |
| `store_name` | `nvarchar(100)` | NO | NO |  | NO |
| `store_description` | `nvarchar(500)` | YES | NO |  | NO |
| `store_logo_url` | `nvarchar(255)` | YES | NO |  | NO |
| `status` | `varchar(30)` | NO | NO | ('PENDING') | NO |
| `reject_reason` | `nvarchar(500)` | YES | NO |  | NO |
| `reviewed_by` | `int` | YES | NO |  | NO |
| `reviewed_at` | `datetime2(7)` | YES | NO |  | NO |
| `created_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `updated_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |

### Computed Columns
無。

### Primary Key
- ``: `application_id`

### Foreign Keys
- `fk_seller_application_member`: `member_id` → `member.Member` (`member_id`)
- `fk_seller_application_reviewer`: `reviewed_by` → `member.Member` (`member_id`)

### Unique Constraints
無。

### Check Constraints
- `ck_seller_application_review_consistency`: `[status]='PENDING' AND [reject_reason] IS NULL AND [reviewed_by] IS NULL AND [reviewed_at] IS NULL OR [status]='APPROVED' AND [reject_reason] IS NULL AND [reviewed_by] IS NOT NULL AND [reviewed_at] IS NOT NULL OR [status]='REJECTED' AND [reject_reason] IS NOT NULL AND [reviewed_by] IS NOT NULL AND [reviewed_at] IS NOT NULL`
- `ck_seller_application_status`: `[status]='REJECTED' OR [status]='APPROVED' OR [status]='PENDING'`

### Default Constraints
- `` on `created_at`: `(sysdatetime())`
- `` on `updated_at`: `(sysdatetime())`
- `df_seller_application_status` on `status`: `('PENDING')`

### Indexes

| Name | Unique | Clustered | Key columns | Included columns | Filter |
| --- | ---: | ---: | --- | --- | --- |
| `ix_seller_application_created_at` | NO | NO | `created_at` ASC |  |  |
| `ix_seller_application_member_id` | NO | NO | `member_id` ASC |  |  |
| `ix_seller_application_status` | NO | NO | `status` ASC |  |  |
| `uq_seller_application_member_pending` | YES | NO | `member_id` ASC |  | ([status]='PENDING') |

---

## `seller.withdrawal_request`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `withdrawal_id` | `int` | NO | YES |  | NO |
| `seller_id` | `int` | NO | NO |  | NO |
| `amount` | `decimal(12,2)` | NO | NO |  | NO |
| `status` | `nvarchar(20)` | NO | NO | (N'PROCESSING') | NO |
| `requested_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |

### Computed Columns
無。

### Primary Key
- `pk_seller_withdrawal_request`: `withdrawal_id`

### Foreign Keys
- `fk_seller_withdrawal_request_seller`: `seller_id` → `seller.Seller` (`seller_id`)

### Unique Constraints
無。

### Check Constraints
- `ck_seller_withdrawal_request_amount`: `[amount]>(0)`
- `ck_seller_withdrawal_request_status`: `[status]=N'REJECTED' OR [status]=N'PAID' OR [status]=N'PROCESSING'`

### Default Constraints
- `df_seller_withdrawal_request_requested_at` on `requested_at`: `(sysdatetime())`
- `df_seller_withdrawal_request_status` on `status`: `(N'PROCESSING')`

### Indexes

| Name | Unique | Clustered | Key columns | Included columns | Filter |
| --- | ---: | ---: | --- | --- | --- |
| `ix_seller_withdrawal_request_seller_status` | NO | NO | `seller_id` ASC, `status` ASC |  |  |

---

## `service.Demand`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `demand_id` | `int` | NO | NO |  | NO |
| `role_name` | `varchar(50)` | NO | NO |  | NO |
| `subtheme` | `nvarchar(50)` | YES | NO |  | NO |
| `demand` | `nvarchar(50)` | YES | NO |  | NO |
| `demand_enter` | `nvarchar(100)` | YES | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `pk_service_demand`: `demand_id`

### Foreign Keys
無。

### Unique Constraints
無。

### Check Constraints
- ``: `[role_name]='seller' OR [role_name]='customer'`

### Default Constraints
無。

### Indexes

無。

---

## `service.Reply`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `sys_reply_id` | `int` | NO | NO |  | NO |
| `role_name` | `varchar(50)` | NO | NO |  | NO |
| `demand` | `nvarchar(50)` | YES | NO |  | NO |
| `reply` | `nvarchar(100)` | YES | NO |  | NO |
| `reply_enter` | `nvarchar(100)` | YES | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `pk_service_reply`: `sys_reply_id`

### Foreign Keys
無。

### Unique Constraints
無。

### Check Constraints
- ``: `[role_name]='seller' OR [role_name]='customer'`

### Default Constraints
無。

### Indexes

無。

---

## `service.Role`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `service_role_id` | `int` | NO | NO |  | NO |
| `role_name` | `varchar(50)` | NO | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `pk_service_role`: `service_role_id`

### Foreign Keys
無。

### Unique Constraints
無。

### Check Constraints
- ``: `[role_name]='seller' OR [role_name]='customer'`

### Default Constraints
無。

### Indexes

無。

---

## `service.Subtheme`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `subtheme_id` | `int` | NO | NO |  | NO |
| `role_name` | `varchar(50)` | NO | NO |  | NO |
| `topic` | `nvarchar(50)` | YES | NO |  | NO |
| `subtheme` | `nvarchar(50)` | YES | NO |  | NO |
| `subtheme_enter` | `nvarchar(100)` | YES | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `pk_service_subtheme`: `subtheme_id`

### Foreign Keys
無。

### Unique Constraints
無。

### Check Constraints
- ``: `[role_name]='seller' OR [role_name]='customer'`

### Default Constraints
無。

### Indexes

無。

---

## `service.Topic`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `topic_id` | `char(1)` | NO | NO |  | NO |
| `role_name` | `varchar(50)` | NO | NO |  | NO |
| `topic` | `nvarchar(50)` | YES | NO |  | NO |
| `topic_enter` | `nvarchar(100)` | YES | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `pk_service_topic`: `topic_id`

### Foreign Keys
無。

### Unique Constraints
無。

### Check Constraints
- ``: `[role_name]='seller' OR [role_name]='customer'`

### Default Constraints
無。

### Indexes

無。

---

## `sysmsg.msg_function_sequence`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `prefix` | `varchar(2)` | NO | NO |  | NO |
| `current_value` | `int` | NO | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `PK_sysmsg_msg_function_sequence`: `prefix`

### Foreign Keys
無。

### Unique Constraints
無。

### Check Constraints
- `CK_sysmsg_msg_function_sequence_prefix`: `[prefix]='SC' OR [prefix]='AS' OR [prefix]='AC' OR [prefix]='OS' OR [prefix]='OC' OR [prefix]='OA'`
- `CK_sysmsg_msg_function_sequence_value`: `[current_value]>=(1) AND [current_value]<=(999)`

### Default Constraints
無。

### Indexes

無。

---

## `sysmsg.record`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `record_id` | `int` | NO | YES |  | NO |
| `send_id` | `int` | NO | NO |  | NO |
| `msg_function` | `varchar(6)` | NO | NO |  | NO |
| `msgfrom_seller_id` | `int` | NO | NO |  | NO |
| `msgto_member_id` | `int` | YES | NO |  | NO |
| `msgto_seller_id` | `int` | YES | NO |  | NO |
| `order_id` | `int` | YES | NO |  | NO |
| `order_status` | `nvarchar(30)` | YES | NO |  | NO |
| `member_inbox` | `computed` | YES | NO |  | YES |
| `seller_inbox` | `computed` | YES | NO |  | YES |
| `record_status` | `nvarchar(10)` | NO | NO | ('UNREAD') | NO |
| `record_created_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |

### Computed Columns
- `member_inbox`: `(case when [msgto_member_id] IS NULL then NULL when left([msg_function],(2))='OC' OR left([msg_function],(2))='OA' then 'SYSTEM_INBOX' when left([msg_function],(2))='AC' then 'ORDER_INBOX' when left([msg_function],(2))='SC' then 'SELLER_INBOX'  end)` (persisted: YES)
- `seller_inbox`: `(case when [msgto_seller_id] IS NULL then NULL when left([msg_function],(2))='OS' OR left([msg_function],(2))='OA' then 'SYSTEM_NOTICE' when left([msg_function],(2))='AS' AND [order_status]='CANCELLED' then 'CANCELLED_ORDER' when left([msg_function],(2))='AS' then 'NEW_ORDER'  end)` (persisted: YES)

### Primary Key
- `PK_sysmsg_record`: `record_id`

### Foreign Keys
- `FK_sysmsg_record_send_function`: `send_id`, `msg_function` → `sysmsg.send` (`send_id`, `msg_function`)

### Unique Constraints
無。

### Check Constraints
- `CK_record_msg_function`: `([msg_function]) collate Latin1_General_100_BIN2 like 'OA-[0-9][0-9][0-9]' AND [msg_function]<>'OA-000' OR ([msg_function]) collate Latin1_General_100_BIN2 like 'OC-[0-9][0-9][0-9]' AND [msg_function]<>'OC-000' OR ([msg_function]) collate Latin1_General_100_BIN2 like 'OS-[0-9][0-9][0-9]' AND [msg_function]<>'OS-000' OR ([msg_function]) collate Latin1_General_100_BIN2 like 'AC-[0-9][0-9][0-9]' AND [msg_function]<>'AC-000' OR ([msg_function]) collate Latin1_General_100_BIN2 like 'AS-[0-9][0-9][0-9]' AND [msg_function]<>'AS-000' OR ([msg_function]) collate Latin1_General_100_BIN2 like 'SC-[0-9][0-9][0-9]' AND [msg_function]<>'SC-000'`
- `CK_record_status`: `[record_status]='DELETE' OR [record_status]='READ' OR [record_status]='UNREAD'`
- `CK_sysmsg_record_exactly_one_recipient`: `[msgto_member_id] IS NOT NULL AND [msgto_seller_id] IS NULL OR [msgto_member_id] IS NULL AND [msgto_seller_id] IS NOT NULL`
- `CK_sysmsg_record_order_snapshot`: `[order_id] IS NULL AND [order_status] IS NULL OR [order_id] IS NOT NULL AND ([order_status]='CANCELLED' OR [order_status]='COMPLETED' OR [order_status]='DELIVERED' OR [order_status]='SHIPPED' OR [order_status]='PAID')`

### Default Constraints
- `DF_sysmsg_record_created_at` on `record_created_at`: `(sysdatetime())`
- `DF_sysmsg_record_status` on `record_status`: `('UNREAD')`

### Indexes

| Name | Unique | Clustered | Key columns | Included columns | Filter |
| --- | ---: | ---: | --- | --- | --- |
| `IX_sysmsg_record_member_category` | NO | NO | `msgto_member_id` ASC, `member_inbox` ASC, `record_status` ASC, `record_created_at` ASC, `record_id` ASC |  |  |
| `IX_sysmsg_record_msgfrom` | NO | NO | `msgfrom_seller_id` ASC, `msg_function` ASC, `record_status` ASC, `record_created_at` ASC |  |  |
| `IX_sysmsg_record_seller_category` | NO | NO | `msgto_seller_id` ASC, `seller_inbox` ASC, `record_status` ASC, `record_created_at` ASC, `record_id` ASC |  |  |
| `IX_sysmsg_record_send_id` | NO | NO | `send_id` ASC |  |  |
| `UX_sysmsg_record_id_send_id` | YES | NO | `record_id` ASC, `send_id` ASC |  |  |
| `UX_sysmsg_record_order_member_once` | YES | NO | `order_id` ASC, `order_status` ASC, `msgto_member_id` ASC |  | ([order_id] IS NOT NULL AND [msgto_member_id] IS NOT NULL) |
| `UX_sysmsg_record_order_seller_once` | YES | NO | `order_id` ASC, `order_status` ASC, `msgto_seller_id` ASC |  | ([order_id] IS NOT NULL AND [msgto_seller_id] IS NOT NULL) |

---

## `sysmsg.record_channel`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `record_channel_id` | `int` | NO | YES |  | NO |
| `send_id` | `int` | NO | NO |  | NO |
| `record_id` | `int` | NO | NO |  | NO |
| `channel_type` | `varchar(10)` | NO | NO |  | NO |
| `notification_type` | `varchar(20)` | NO | NO |  | NO |
| `sent_at` | `datetime2(7)` | YES | NO |  | NO |
| `provider_message_id` | `nvarchar(200)` | YES | NO |  | NO |
| `error_message` | `nvarchar(1000)` | YES | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `PK_sysmsg_record_channel`: `record_channel_id`

### Foreign Keys
- `FK_sysmsg_record_channel_record_send`: `record_id`, `send_id` → `sysmsg.record` (`record_id`, `send_id`)
- `FK_sysmsg_record_channel_send`: `send_id` → `sysmsg.send` (`send_id`)

### Unique Constraints
- `UX_sysmsg_record_channel_type`: `record_id`, `channel_type`

### Check Constraints
- `CK_sysmsg_record_channel_notification_type`: `[notification_type]='MARKETING' OR [notification_type]='ORDER'`
- `CK_sysmsg_record_channel_result`: `NOT ([sent_at] IS NOT NULL AND [error_message] IS NOT NULL) AND ([sent_at] IS NULL AND [provider_message_id] IS NULL OR [sent_at] IS NOT NULL AND len(ltrim(rtrim([provider_message_id])))>(0)) AND ([error_message] IS NULL OR len(ltrim(rtrim([error_message])))>(0))`
- `CK_sysmsg_record_channel_type`: `[channel_type]='LINE' OR [channel_type]='EMAIL'`

### Default Constraints
無。

### Indexes

| Name | Unique | Clustered | Key columns | Included columns | Filter |
| --- | ---: | ---: | --- | --- | --- |
| `IX_sysmsg_record_channel_pending` | NO | NO | `channel_type` ASC, `record_channel_id` ASC |  | ([sent_at] IS NULL AND [provider_message_id] IS NULL AND [error_message] IS NULL) |
| `IX_sysmsg_record_channel_send` | NO | NO | `send_id` ASC, `record_id` ASC, `channel_type` ASC |  |  |

---

## `sysmsg.send`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `send_id` | `int` | NO | YES |  | NO |
| `msgfrom_seller_id` | `int` | NO | NO |  | NO |
| `msg_function` | `varchar(6)` | NO | NO |  | NO |
| `msg_label` | `nvarchar(50)` | NO | NO |  | NO |
| `send_title` | `nvarchar(100)` | NO | NO |  | NO |
| `send_content` | `nvarchar(1000)` | NO | NO |  | NO |
| `send_upd_at` | `datetime2(7)` | NO | NO | (sysdatetime()) | NO |
| `send_status` | `nvarchar(10)` | NO | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `PK_sysmsg_send`: `send_id`

### Foreign Keys
無。

### Unique Constraints
無。

### Check Constraints
- ``: `len(ltrim(rtrim([msg_label])))>(0)`
- ``: `len(ltrim(rtrim([send_title])))>(0)`
- ``: `len(ltrim(rtrim([send_content])))>(0)`
- `CK_send_msg_function`: `([msg_function]) collate Latin1_General_100_BIN2 like 'OA-[0-9][0-9][0-9]' AND [msg_function]<>'OA-000' OR ([msg_function]) collate Latin1_General_100_BIN2 like 'OC-[0-9][0-9][0-9]' AND [msg_function]<>'OC-000' OR ([msg_function]) collate Latin1_General_100_BIN2 like 'OS-[0-9][0-9][0-9]' AND [msg_function]<>'OS-000' OR ([msg_function]) collate Latin1_General_100_BIN2 like 'AC-[0-9][0-9][0-9]' AND [msg_function]<>'AC-000' OR ([msg_function]) collate Latin1_General_100_BIN2 like 'AS-[0-9][0-9][0-9]' AND [msg_function]<>'AS-000' OR ([msg_function]) collate Latin1_General_100_BIN2 like 'SC-[0-9][0-9][0-9]' AND [msg_function]<>'SC-000'`
- `CK_send_status`: `[send_status]='DELETE' OR [send_status]='SAVE' OR [send_status]='SEND'`

### Default Constraints
- `DF_sysmsg_send_upd_at` on `send_upd_at`: `(sysdatetime())`

### Indexes

| Name | Unique | Clustered | Key columns | Included columns | Filter |
| --- | ---: | ---: | --- | --- | --- |
| `IX_sysmsg_send_owner` | NO | NO | `msgfrom_seller_id` ASC, `send_status` ASC, `send_upd_at` ASC, `send_id` ASC |  |  |
| `UX_sysmsg_send_id_msg_function` | YES | NO | `send_id` ASC, `msg_function` ASC |  |  |
| `UX_sysmsg_send_msg_function_save` | YES | NO | `msg_function` ASC |  | ([send_status]='SAVE') |

---

## `sysmsg.send_disorder`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `send_disorder_id` | `int` | NO | NO |  | NO |
| `order_id` | `int` | NO | NO |  | NO |
| `order_no` | `nvarchar(30)` | NO | NO |  | NO |
| `total_amount` | `decimal(12,2)` | NO | NO |  | NO |
| `payment_method_id` | `int` | YES | NO |  | NO |
| `method_name` | `nvarchar(50)` | YES | NO |  | NO |
| `cancel_reason` | `nvarchar(500)` | YES | NO |  | NO |
| `cancelled_at` | `datetime2(7)` | NO | NO |  | NO |
| `status` | `nvarchar(30)` | NO | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `PK_sysmsg_send_disorder`: `send_disorder_id`

### Foreign Keys
- `FK_send_disorder_send`: `send_disorder_id` → `sysmsg.send` (`send_id`)

### Unique Constraints
無。

### Check Constraints
- `CK_send_disorder_status`: `[status]='CANCELLED'`
- `CK_send_disorder_total_amount`: `[total_amount]>=(0)`

### Default Constraints
無。

### Indexes

無。

---

## `sysmsg.send_order`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `send_order_id` | `int` | NO | NO |  | NO |
| `order_id` | `int` | NO | NO |  | NO |
| `order_no` | `nvarchar(30)` | NO | NO |  | NO |
| `total_amount` | `decimal(12,2)` | NO | NO |  | NO |
| `payment_method_id` | `int` | YES | NO |  | NO |
| `method_name` | `nvarchar(50)` | YES | NO |  | NO |
| `created_at` | `datetime2(7)` | NO | NO |  | NO |
| `status` | `nvarchar(30)` | NO | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `PK_sysmsg_send_order`: `send_order_id`

### Foreign Keys
- `FK_send_order_send`: `send_order_id` → `sysmsg.send` (`send_id`)

### Unique Constraints
無。

### Check Constraints
- `CK_send_order_status`: `[status]='COMPLETED' OR [status]='DELIVERED' OR [status]='SHIPPED' OR [status]='PAID'`
- `CK_send_order_total_amount`: `[total_amount]>=(0)`

### Default Constraints
無。

### Indexes

無。

---

## `sysmsg.send_seller`

### Columns

| Column | SQL Type | Nullable | Identity | Default | Computed |
| --- | --- | ---: | ---: | --- | ---: |
| `send_seller_id` | `int` | NO | NO |  | NO |
| `order_no` | `nvarchar(30)` | YES | NO |  | NO |
| `img_one` | `varbinary` | YES | NO |  | NO |
| `img_two` | `varbinary` | YES | NO |  | NO |
| `img_three` | `varbinary` | YES | NO |  | NO |
| `send_remark` | `nvarchar(1000)` | YES | NO |  | NO |

### Computed Columns
無。

### Primary Key
- `PK_sysmsg_send_seller`: `send_seller_id`

### Foreign Keys
- `FK_send_seller_send`: `send_seller_id` → `sysmsg.send` (`send_id`)

### Unique Constraints
無。

### Check Constraints
無。

### Default Constraints
無。

### Indexes

無。
