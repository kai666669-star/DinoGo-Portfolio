# 共用 Security 規範

> 本文件是 DinoGo 跨模組的 JWT、Spring Security 與前端認證整合規範，供人員與 Codex 在新增或調整 API 時使用。
>
> `SecurityConfig.java` 是目前實際生效的路由規則來源；各模組 API 文件才是各 endpoint URL、request／response 與業務權限的契約來源。本文件不重複維護完整路由白名單或任何 secret。

## 1. 何時必讀

遇到下列任一情況，先讀本文件與直接相關 API contract／程式：

- 新增或修改 API、HTTP method、公開性、登入或角色權限。
- 修改 `SecurityConfig`、JWT claim、JWT Filter、登入／登出或 401 處理。
- API 涉及會員、賣家、訂單、付款、購物車、地址、收藏、通知、評價、檔案上傳或管理功能。
- 前端新增會帶 JWT 的 API、受保護 route、Pinia auth state 或 Axios interceptor。

## 2. 責任邊界

| 層級 | 責任 |
| --- | --- |
| `SecurityConfig` | 決定路徑與 HTTP method 是否公開、需登入或需角色；未登入回 401、角色不足回 403。 |
| `JwtAuthenticationFilter` | 驗證 Bearer JWT、會員狀態與 auth version，建立 `SecurityContext`。 |
| Controller | 以 `@AuthenticationPrincipal AuthenticatedMember` 取得目前登入者，不從 client 提供的 member／buyer／seller ID 決定身分。 |
| Service | 驗證 resource ownership、訂單／評論資格、狀態轉換與其他業務規則。已登入不代表有權操作任意資料。 |
| 前端 | 透過共用 Axios instance 帶 JWT；router guard 與隱藏按鈕只能改善 UX，不可取代後端授權。 |

## 3. JWT 與登入狀態

- JWT 由 A 模組簽發，包含 `memberId`、小寫 `roles`、`authVersion`、subject、issued-at 與 expiration。
- Filter 將角色轉成 Spring Security `ROLE_*` authorities；`hasRole("SELLER")` 對應 JWT 的 `seller`。
- Filter 只會建立狀態為 `ACTIVE` 且 auth version 相符會員的 authentication。密碼或授權異動若需立即失效既有 token，必須使 auth version 變更。
- 不得記錄、回傳、提交或貼入文件：JWT secret、完整 token、密碼、API key、真實帳號資料。

## 4. API 授權規則

### 公開 API

只有已確認可匿名存取的 endpoint 才可設為 `permitAll()`。公開資料範圍與回傳 DTO 由該模組確認；不要因為是 `GET` 就自動公開。

### 需登入 API

個人資料、地址、購物車、收藏、結帳、訂單、付款、會員優惠券、通知與會員操作，預設需要 `.authenticated()`。缺少、格式錯誤、過期或已失效的 JWT 必須由 Security 層回 401。

### 角色 API

賣家與管理操作使用 `.hasRole(...)`。角色限制不取代 Service 的 ownership 驗證，例如賣家仍只能操作自己的商品或訂單。

### HTTP method 必須完整列舉

Security matcher 必須同時考慮實際 method。若商品寫入需 seller，POST、PUT、PATCH、DELETE 都必須受到保護；不可只補其中幾種，讓其他 method 落入 fallback。

目前 fallback 是 `.anyRequest().denyAll()`。新增 endpoint 必須先在 `SecurityConfig` 明確列出其公開、登入或角色規則；未列出的 route 會被拒絕。公開靜態資源也只能列出實際需要公開的子路徑，不可直接開放整個 `/uploads/**`。

## 5. 前端規則

- 所有 API 使用 `frontend/src/api/axios.js` 的 `api` instance；不得自行建立 interceptor、讀取另一個 token storage key 或手動散落 Bearer header。
- auth state 透過 `useAuthStore` 管理並儲存在既有 sessionStorage key；登入、登出與清除 session 必須呼叫 store action。
- 非 `/auth/` 請求收到 401 時，Axios 會清除 auth session 並導向 `/login`。403 是已登入但無權限，頁面應保留登入狀態並顯示適當權限訊息。
- 前端不可傳送或信任 `memberId`、`buyerId`、`sellerId` 作為目前登入者身分；這些值若為業務查詢條件，後端仍須以 principal 驗證。

## 6. 新增或修改 API checklist

在實作前，模組負責人必須提供 HTTP method、path、是否公開、需要角色及 resource ownership 規則。

1. 確認 API contract，並通知 A 與受影響模組所有 authorization／role 改動。
2. 對需保護的 path 在 `SecurityConfig` 增加 matcher；公開 API 也要明確檢視是否真的可匿名存取。
3. Controller 使用 `@AuthenticationPrincipal AuthenticatedMember`；Service 驗證 ownership 與業務資格。
4. 補 Security integration test：匿名／無效 JWT → 401、錯誤角色 → 403、合法角色可到達 controller，並驗證不應執行的 service 沒有被呼叫。
5. 前端使用共用 Axios，必要時加 router meta 作 UX 導向；不可只依前端按鈕或 route guard 保護功能。
6. 更新所屬模組 API contract；本文件只在共用規則改變時更新。

## 7. 交接與變更管理

- A 維護 JWT、Filter、SecurityConfig 與本文件；其他模組擁有自己的 endpoint 與業務授權契約。
- 模組新增 endpoint 前，向 A 提供上述 checklist 所需資訊；A 可協助整合共用 matcher 與測試，不接手該模組的 Service／資料 ownership。
- API URL、method、request／response、validation、authorization 或角色變更必須通知受影響組員，並在 PR 說明 401／403 行為與測試結果。
