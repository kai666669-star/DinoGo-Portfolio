# DinoGo Review 模組架構

> 本文件是 Review 模組功能確立文件。前端需求若改變 Review 的 API、DTO、篩選、分頁、公開顯示或跨模組行為，必須同步更新本文件。

## 1. 模組目標

- 訂單轉為 `COMPLETED` 時建立 `review.history` 與 `review.star`，星等先留空。
- 訂單轉為 `CANCELLED` 時刪除 `review.history`，`review.star` 由 SQL Server `ON DELETE CASCADE` 清除。
- 會員由訂單模組查看訂單與商品細項，再由 Review 查詢、新增、修改及清除評論。
- 產品明細頁顯示所有已評分評論，採 Offset 分頁，每頁固定 10 筆。
- DinoGo 是單一 Spring Boot 應用；跨模組資料及驗證使用 Service／Provider Bean，並共用 Spring 交易邊界。

## 2. 分層與生命週期

- Controller：接收 HTTP request、執行 `@Valid`、從 `@AuthenticationPrincipal` 取得會員身分並回傳 DTO，不直接操作 Repository。
- ReviewService：管理交易、所有權、Provider DTO mapping、更新時間、唯一約束與樂觀鎖衝突。
- Repository：只負責資料存取、Offset 分頁及彙總，不放權限或跨模組邏輯。
- 不提供前端可呼叫的訂單生命週期 internal endpoint。
- 訂單事件使用 `ApplicationEventPublisher` 與 `@TransactionalEventListener(AFTER_COMMIT)`；定期對帳補建可能遺失的完成訂單評論。
- `review.history.order_id` 唯一；`review.star(history_id, order_item_id)` 複合唯一。

## 3. 資料模型

### review.history

- `history_id` PK。
- 保存 `seller_id`、`member_id`、`order_id`、`order_no`。
- 與 Star 一對多、LAZY 載入；建立使用 `CascadeType.PERSIST`，刪除交由資料庫 FK cascade。

### review.star

- `star_id` PK，`history_id` FK 指向 history。
- 保存訂單商品快照：`order_item_id`、`product_id`、`product_name`、`image_url`、`base_price`。
- 評論資料：`five_star`、`feedback`，以及最多三組 Cloudinary secureUrl／publicId。
- `five_star` 使用可為 null 的 Integer／SQL Server tinyint；null 代表尚未評分。
- `review_priority` 是 SQL Server PERSISTED 計算欄位，Java 設為不可寫入。
- 排序優先序：有文字與三圖為 5、文字與二圖為 4、文字與一圖為 3、只有文字為 2、只有圖片為 1、只有星等為 0。
- DB constraint 必須保護星等 1～5、未評分內容為 null、Cloudinary URL／publicId 成對。

## 4. API

### 會員操作

- `GET /api/reviews/orders/{orderId}`
- `GET /api/reviews/orders/{orderId}/stars`
- `POST /api/reviews/stars/images`：multipart 一次上傳目前新增的評論圖片，最多三張。
- `PUT /api/reviews/stars/{starId}`
- `DELETE /api/reviews/stars/{starId}/content`

### 產品頁

- `GET /api/reviews/products/{productId}?page=&rating=&content=`
  - `page` 從 1 開始，每頁固定 10 筆。
  - `rating` 選填，只允許 1～5。
  - `content` 為 `ALL`、`FEEDBACK` 或 `IMAGE`。
  - 使用 Offset 分頁，依 `review_priority DESC, star_upd_at DESC, star_id DESC` 穩定排序。
  - 回傳目前篩選的 `currentPage`、`totalPages`、`totalElements`，以及不受目前篩選影響的全商品 `summary`。
- `GET /api/reviews/products/{productId}/rating-summary`

### 商家頁

- `GET /api/reviews/sellers/{sellerId}/rating-summary`

## 5. 產品明細評價顯示

- 評價區位於商品大圖下方左側，約占頁面寬度 2/3；內容框比上方大圖寬。
- Tab 固定為「產品說明」與「商品評價」。
- 彙總區約一筆評論高度：左側顯示無條件捨去至小數一位的分數與主視覺綠色星星；小數部分使用較小字體，不顯示「全部星星平均數」文字。
- 彙總右側第一列顯示全部與 5～1 星數量；第二列顯示附評論、附圖片數量。
- 每筆依序顯示遮罩會員編號、評價時間、綠色星等、單行省略評論；右側最多三張圖片。
- `ProductReviewResponse` 不回傳原始 `memberId`，只回傳後端產生的 `reviewerDisplayName`，例如 `會員 1*****5`。
- 為產生遮罩名稱，產品公開查詢必須在同一查詢載入所需 history，避免逐筆 LAZY N+1。
- 點擊評論開啟詳閱浮層；右上角 X、點擊浮層外及 Esc 均可關閉。
- 有評價時一律顯示分頁列；只有一頁時也顯示目前頁碼，不可用按鈕 disabled。
- 分頁列使用 `<<`、`<`、頁碼、`…`、最後頁、`>`、`>>`。
- 不足 10 筆或最後一頁不足 10 筆時，分頁列緊跟最後一筆，不補空白評論卡。
- 評價白底框結束後到頁底綠色背景保留約三筆評論高度的緩衝空間。

## 6. 圖片策略

- Cloudinary secureUrl 與 publicId 成對保存，最多三張。
- 上傳使用 multipart/form-data；多張上傳部分失敗時反向補償刪除已成功資產。
- publicId 必須位於該會員的 Review 目錄下。

## 6.1 完成訂單的單項產品評價

- 完成訂單的每個 item 尾端顯示填滿高度的星形端蓋按鈕，不顯示「評價」文字。
- 按鈕狀態只依 Review 後端 `StarResponse.fiveStar > 0` 判斷，不以訂單前端暫存狀態推測。
- 未評價使用較強烈的主視覺綠色；已評價使用灰暗的主視覺綠色。
- 點擊端蓋在訂單詳情頁開啟評價浮層；浮層標題為「完成的訂單 · 單項產品 商品評價」。
- 浮層下拉清單包含該訂單所有 item，可直接切換評價對象並重新載入對應 Star。
- 表單顯示產品照片、名稱、簡易規格、五星選擇、評論內容及最多三張照片。
- 「送出」呼叫圖片上傳及 `PUT`；「清空」呼叫 `DELETE`；成功後立即更新訂單 item 端蓋狀態。
- 右上角 X、點擊浮層外及 Esc 都代表不做目前尚未送出的變更並關閉浮層。

## 7. 例外

- `ReviewNotFoundException` → 404。
- `ReviewForbiddenException` → 403。
- `ReviewConflictException` → 409。
- Provider 資料不完整的 `IllegalStateException` → 500。
- 樂觀鎖只使用 JPA `@Version`，不使用 expectedVersion。

## 8. 套件邊界

所有 Review 檔案統一位於 `com.dinogo.review.*`。Review 不直接查詢其他模組 Repository；會員、訂單與商家資料透過正式 Provider／Service contract 取得。
