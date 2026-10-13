# Review 公開商品評價 API

## 商品評價篩選與分頁

```http
GET /api/reviews/products/{productId}?page=1&rating=5&content=ALL
```

- 公開端點，不需 JWT。
- `page`：從 1 開始，每頁固定 10 筆。
- `rating`：選填，允許 `1`～`5`。
- `content`：`ALL`、`FEEDBACK` 或 `IMAGE`，預設 `ALL`。
- 排序依序為內容完整度、評價更新時間、`starId`，皆由新到舊。
- `reviewerDisplayName` 已由後端遮罩，不回傳完整 `memberId`。
- 即使篩選結果只有一頁，前端仍顯示目前頁碼及首／前／後／末頁控制；不可用的控制維持 disabled。
- `summary` 永遠彙總該商品的全部公開評價，不受目前列表篩選影響。

回應的 `content` 最多 10 筆；`currentPage`、`totalPages`、`totalElements` 對應目前篩選結果。`summary` 包含全部評價數、各星等數、附評論數、附圖片數及無條件捨去至小數一位的平均分數。

不合法的頁碼、星等或內容篩選回傳 `400 Bad Request`。

## 完成訂單單項產品評價

- `GET /api/reviews/orders/{orderId}/stars`：取得該會員訂單的所有 Star；前端以 `fiveStar > 0` 判斷是否已評價。
- `POST /api/reviews/stars/images`：`multipart/form-data`，欄位名稱為 `files`，一次最多三張；回傳 Cloudinary `secureUrl` 與 `publicId`。
- `PUT /api/reviews/stars/{starId}`：送出 1～5 星、內容及最多三組圖片 URL／publicId。
- `DELETE /api/reviews/stars/{starId}/content`：清空星等、內容及圖片參照，使 Star 回到未評價狀態。

以上端點皆需登入；後端從 JWT principal 驗證訂單與 Star 所有權，不接受前端提供 memberId。
