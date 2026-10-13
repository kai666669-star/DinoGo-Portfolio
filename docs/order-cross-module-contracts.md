# 訂單模組跨模組整合需求

> 對象：B 商品目錄模組、E 賣家中心模組、D 訂單模組
>
> 目的：整理 D 訂單模組在賣家訂單權限、下單扣庫存及取消回補庫存時，需要其他模組提供的最小 Service contract。
>
> 本文件不修改資料表 ownership、DB schema 或既有業務分工。資料表結構仍以 `docs/database-schema.md` 為準。

---

## 一、整合需求摘要

| 整合項目 | 主要負責 | 需要協作 | 目的 |
| --- | --- | --- | --- |
| 賣家更新訂單狀態的 ownership 驗證 | D 訂單 | E 賣家 | 防止賣家操作其他賣家的訂單 |
| 建立訂單時條件式扣除庫存 | D 訂單流程 | B 商品 | 避免併發下單造成超賣 |
| 取消訂單時恢復庫存 | D 訂單流程 | B 商品 | 取消成功後正確回補 SKU 庫存 |
| 併發扣庫存測試 | D、B | B 商品 | 驗證最後一件商品只能被一張訂單取得 |
| 併發取消測試 | D 訂單 | B 僅提供回補操作 | 驗證同一訂單只能回補一次庫存 |

---

# 二、給 E 賣家中心模組的需求

## 2.1 背景

JWT Authentication 提供的是登入會員的 `memberId`，但訂單資料保存的是：

```text
sales.Orders.seller_id
```

D 模組不能接受前端傳入的 `sellerId` 作為授權依據，因此需要由 E 模組提供：

```text
JWT memberId → Seller → sellerId
```

## 2.2 希望 E 提供的最小 contract

建議由 E 模組提供 Service，而不是讓 D 直接存取 `SellerRepository`：

```java
public interface SellerIdentityService {

    Integer getActiveSellerIdByMemberId(Integer memberId);
}
```

如果現有架構習慣回傳 Entity，也可以提供：

```java
public Seller getActiveSellerByMemberId(Integer memberId);
```

D 實際只需要可信任的 `sellerId`。

## 2.3 Contract 行為

輸入：

```text
memberId：由 JWT Authentication 取得，不接受前端 request/query parameter
```

成功：

```text
回傳該會員所屬且可以操作訂單的 sellerId
```

失敗情況請由 E 明確區分：

| 情況 | 建議結果 |
| --- | --- |
| 會員沒有 Seller | 拋出 SellerNotFoundException 或等價例外 |
| Seller 已停權／不可營業 | 拋出 InactiveSellerException 或等價例外 |
| memberId 無效 | 拒絕操作，不回傳其他 Seller |

請 E 模組確認哪些 Seller status 可以操作訂單。

## 2.4 D 模組如何使用

```text
JWT memberId
→ SellerIdentityService 取得 sellerId
→ D 以 orderId + sellerId 查詢訂單
→ 驗證合法狀態轉換
→ 更新訂單狀態
```

D 的 Repository 預計使用：

```java
Optional<Order> findByOrderIdAndSellerId(
        Integer orderId,
        Integer sellerId);
```

找不到時應回 404，避免洩漏其他賣家的訂單是否存在。

## 2.5 賣家可操作的訂單狀態

建議賣家通用狀態 API 只允許：

```text
PAID → PROCESSING
PROCESSING → SHIPPED
```

不應由賣家通用狀態 API 處理：

| 狀態變更 | 正確負責流程 |
| --- | --- |
| `PENDING_PAYMENT → PAID` | Payment SUCCESS 流程 |
| `任何狀態 → CANCELLED` | 專用取消流程，必須處理庫存回補 |
| Shipment `AVAILABLE_FOR_PICKUP → DELIVERED`、Order `SHIPPED → COMPLETED` | 買家確認收貨流程；Shipment 仍為 `SHIPPED` 時不得確認 |

## 2.6 E 模組不需要負責

- 不需要修改 `sales.Orders`。
- 不需要定義訂單狀態轉換。
- 不需要處理訂單取消或庫存回補。
- 不需要接受前端 sellerId 來協助 D 驗證。
- 不需要修改 D 模組的 Controller、Service 或 DTO。

## 2.7 驗收方式

- Seller 會員可以取得自己的 sellerId。
- 非 Seller 會員無法取得 sellerId。
- 停權或不可營業 Seller 無法取得可操作訂單的 sellerId。
- D 使用 Seller A 的 JWT 時，不能更新 Seller B 的訂單。

---

# 三、給 B 商品目錄模組的需求

## 3.1 背景

`catalog.ProductSku` 的 ownership 屬於 B 模組。D 在建立及取消訂單時需要安全地調整庫存：

```text
建立訂單 → 條件式扣除庫存
取消訂單 → 恢復庫存
```

為維持模組邊界，建議由 B 提供 Inventory Service；如果短期仍使用 Repository 方法，也請由 B 確認並維護其 contract。

## 3.2 希望 B 提供的最小 contract

建議 Service：

```java
public interface InventoryService {

    boolean deductStockIfAvailable(Integer skuId, Integer quantity);

    void restoreStock(Integer skuId, Integer quantity);
}
```

如果以 Repository contract 提供，建議為：

```java
int deductStockIfAvailable(Integer skuId, Integer quantity);

int restoreStock(Integer skuId, Integer quantity);
```

受影響筆數定義：

```text
1 = 成功
0 = SKU 不存在、SKU 停用或庫存不足
```

## 3.3 條件式扣庫存要求

扣庫存必須使用單一 atomic update，不可使用「先查庫存，再 setStock」：

```sql
UPDATE catalog.ProductSku
SET stock = stock - :quantity
WHERE sku_id = :skuId
  AND status = 1
  AND stock >= :quantity;
```

Spring Data JPA 參考：

```java
@Modifying(flushAutomatically = true, clearAutomatically = true)
@Query("""
        UPDATE ProductSku sku
        SET sku.stock = sku.stock - :quantity
        WHERE sku.skuId = :skuId
          AND sku.status = 1
          AND sku.stock >= :quantity
        """)
int deductStockIfAvailable(
        @Param("skuId") Integer skuId,
        @Param("quantity") Integer quantity);
```

必要條件：

- `quantity` 必須大於 0。
- SKU 必須存在且啟用。
- 庫存不得扣成負數。
- 同時搶最後一件庫存時只能有一個 request 成功。

## 3.4 恢復庫存要求

取消訂單使用：

```sql
UPDATE catalog.ProductSku
SET stock = stock + :quantity
WHERE sku_id = :skuId;
```

Spring Data JPA 參考：

```java
@Modifying(flushAutomatically = true, clearAutomatically = true)
@Query("""
        UPDATE ProductSku sku
        SET sku.stock = sku.stock + :quantity
        WHERE sku.skuId = :skuId
        """)
int restoreStock(
        @Param("skuId") Integer skuId,
        @Param("quantity") Integer quantity);
```

B 只需要保證回補操作正確且為 atomic update。

同一張訂單只能回補一次，由 D 透過訂單悲觀鎖與狀態驗證負責。

## 3.5 D 模組如何使用

建立訂單：

```text
查詢 SKU 與後端正式價格
→ 驗證所有商品屬於同一賣家
→ atomic deductStockIfAvailable
→ 任一 SKU 失敗則整張訂單 transaction rollback
```

取消訂單：

```text
悲觀鎖定訂單
→ `PENDING_PAYMENT` 訂單可由買家取消
→ 貨到付款訂單在 `PROCESSING`、付款仍為 `PENDING` 且 Shipment 尚未出貨（無 Shipment 或 `PREPARING`）時也可由買家取消
→ 已線上付款成功、已出貨及後續狀態禁止取消；退款不屬於此 MVP 流程
→ 逐筆 restoreStock
→ 訂單改為 CANCELLED
→ 同一 transaction commit
```

## 3.6 B 模組不需要負責

- 不需要判斷訂單是否可以取消。
- 不需要判斷同一張訂單是否已回補過。
- 不需要更新 `sales.Orders` 或 `sales.OrderItem`。
- 不需要處理付款與訂單狀態。
- 不需要接受訂單 ID 才能調整庫存。

## 3.7 併發扣庫存驗收測試

前置資料：

```text
一筆啟用中的 SKU
stock = 1
```

測試：

```text
兩個獨立 transaction 同時各扣 1
```

預期：

```text
一個回傳成功
一個回傳失敗
最終 stock = 0
stock 不得為 -1
```

此測試需使用實際 SQL Server 或能重現資料庫鎖行為的整合測試環境，不能只用 Mockito。

---

# 四、D 訂單模組自行負責的部分

以下不需要 B 或 E 代為實作：

## 4.1 訂單權限與狀態

- 從 JWT Authentication 取得 `memberId`。
- 買家只能查詢與取消自己的訂單。
- 以 E 提供的 sellerId 驗證賣家 ownership。
- 統一驗證訂單合法狀態轉換。
- 通用狀態 API 禁止手動設為 `PAID` 或 `CANCELLED`。

## 4.2 Transaction 一致性

- 建立訂單、建立明細及扣庫存使用同一個 `@Transactional`。
- 取消訂單、恢復庫存及寫入取消欄位使用同一個 `@Transactional`。
- 任一明細失敗時整筆 rollback。

## 4.3 取消訂單併發控制

D 使用訂單悲觀寫鎖：

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
Optional<Order> findForCancellation(Integer orderId, Integer buyerId);
```

併發取消驗收：

```text
訂單初始狀態 = PENDING_PAYMENT
SKU 初始庫存 = 3
訂單明細 quantity = 2

兩個 transaction 同時取消同一張訂單
→ 一個成功
→ 一個因訂單已 CANCELLED 而失敗
→ 最終訂單狀態 = CANCELLED
→ 最終庫存 = 5
→ 庫存不得變成 7
```

---

# 五、跨模組 API／Contract 變更通知

若 B 或 E 提供的 contract 有以下變更，請通知 D：

- Service class 或 method 名稱。
- request／return type。
- 例外類型。
- Seller 可操作狀態判定。
- SKU 啟用狀態定義。
- 庫存方法的回傳值語意。
- transaction propagation。
- Repository bulk update 是否會清除 persistence context。

若需要修改資料表、欄位、PK／FK 或正式 relationship，必須先依團隊流程更新 migration 與 `docs/database-schema.md`；本文件本身不構成 DB schema 變更批准。

---

# 六、可直接傳給組員的簡短訊息

## 給 B 商品模組

```text
D 訂單模組在建立及取消訂單時需要安全調整 ProductSku 庫存。
請協助提供或確認以下 contract：

1. deductStockIfAvailable(skuId, quantity)
   - 使用單一 atomic UPDATE
   - 僅在 status=1 且 stock>=quantity 時扣除
   - 成功回傳 1，失敗回傳 0

2. restoreStock(skuId, quantity)
   - 使用 atomic UPDATE 回補庫存
   - 成功回傳 1，SKU 不存在回傳 0

D 會負責訂單 transaction、合法取消判斷及防止重複回補。
也請協助確認 stock=1 時兩個 transaction 同時扣 1，只能有一個成功。
詳細說明：docs/order-cross-module-contracts.md
```

## 給 E 賣家中心模組

```text
D 訂單模組需要依 JWT memberId 驗證賣家只能操作自己的訂單。
請協助提供 memberId → active sellerId 的 Service contract，例如：

getActiveSellerIdByMemberId(memberId)

請同時明確定義：
1. 會員沒有 Seller 時的例外
2. Seller 停權／不可營業時的例外
3. 哪些 Seller status 可以操作訂單

D 不會接受前端傳入 sellerId，會使用此 contract 後再以 orderId + sellerId 查詢訂單。
詳細說明：docs/order-cross-module-contracts.md
```


---

# 七、buyerId 契約問題與處理方式

> 來源：feature/pinia merge review

## 7.1 背景

`CheckoutView.vue` 建立訂單時，前端是否應傳送 `buyerId` 進 request body，是 merge `feature/pinia` 後必須優先處理的契約問題。

## 7.2 解法選項

### 解法 A：前端繼續傳 buyerId

前端從 `localStorage['member']` 讀出 `memberId`，放進建立訂單 request。

優點：

- 不需要立刻修改後端 DTO。
- 舊版前後端較容易暫時相容。

缺點：

- localStorage 可以被使用者自行修改，buyerId 不可信。
- 若後端直接採用 request 的 buyerId，可能造成以他人身分下單或存取資料的安全問題。
- JWT 已經包含登入身分，重複傳送是多餘且容易產生不一致。

只適合短期過渡，而且後端必須忽略 request buyerId，不能拿它當真正身分。

### 解法 B：後端移除 buyerId，從 JWT 決定（推薦）

前端只送地址、運費、備註與商品；後端 Controller 透過：

```java
@AuthenticationPrincipal AuthenticatedMember member
```

再使用 `member.memberId()` 呼叫 service。

優點：

- 身分來源單一且可信。
- 前端 request 較簡潔。
- 防止使用者竄改 buyerId。
- 與目前 `OrderController` 的寫法一致。

缺點：

- 需要修改共用的 `CreateOrderRequest.java`。
- 前後端必須一起部署；只更新其中一邊會出現 400 或反序列化問題。

推薦修改：移除 `CreateOrderRequest` record 中的 `buyerId` 欄位，以及它的 `@NotNull`、`@Positive` 驗證；保留 `OrderController` 的 `member.memberId()` 流程。

### 解法 C：DTO 暫時保留 buyerId，但後端以 JWT 為準

保留欄位以相容舊前端，但 service 永遠使用 Controller 傳入的 `member.memberId()`，不使用 `request.buyerId`。

優點：

- 舊版前端仍可送 request。
- 可以分階段更新前後端。

缺點：

- DTO 仍會讓人誤以為 buyerId 是必要資料。
- 仍有多餘欄位與驗證，容易造成維護誤用。

若組員目前不能同步修改後端，才使用此方案作為短期過渡；長期仍應回到解法 B。

## 7.3 推薦修改順序

1. D 組確認 `CreateOrderRequest` 改為不接收 buyerId。
2. 後端移除 DTO 的 buyerId 欄位與驗證。
3. 後端 `OrderController` 保留 `member.memberId()`，不要改回 request.buyerId。
4. `SecurityConfig.java` 增加：

   ```java
   .requestMatchers("/api/checkout/**").authenticated()
   ```

5. 前端 `CheckoutView.vue` 保持不送 buyerId，並只使用共用 `api`。
6. 前端新頁面直接 `import api from '@/api/axios'`；不要在頁面新增 JWT interceptor。
7. 測試登入、購物車、結帳預覽與建立訂單。

## 7.4 Merge review 結論（feature/pinia）

- `CartView.vue` 已使用 Pinia store，並使用後端回傳的 `price`，方向正確。
- `CheckoutView.vue` 已移除頁面自己的 JWT interceptor，方向正確。
- `router/index.js` 的 Cart、Checkout 已加登入限制，方向正確。
- 後端 `CreateOrderRequest.java` 目前仍要求 buyerId，與前端目前 request 不一致；這是 merge 後必須先處理的契約問題。
- `SecurityConfig.java` 目前尚未明確保護 `/api/checkout/**`，建議補上。
