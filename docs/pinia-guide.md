# Pinia 與 JWT 前端整合指南

本文件說明 DinoGo 導入 Pinia 後的架構規則、手動測試方式，以及組員新增頁面、API 或 Store 時的注意事項。

---

## 一、這次修改的目的

- 用 Pinia 集中管理登入狀態與購物車狀態。
- 用共用 Axios instance 統一附加 JWT，不讓每個頁面各自讀 token。
- buyer 身分由後端從 JWT 的 `AuthenticatedMember` 決定，不由前端傳入或信任。
- Cart 與 Checkout 頁面必須登入後才能使用。

---

## 二、相關檔案與責任

| 檔案 | 責任 | 組員注意事項 |
|---|---|---|
| `frontend/src/stores/auth.js` | Pinia 登入狀態、token、member | 登入／登出請呼叫 store，不要直接改 storage |
| `frontend/src/stores/cart.js` | 購物車資料、數量、總額、API 操作 | 商品單價欄位是後端的 `price`，不是 `unitPrice` |
| `frontend/src/stores/index.js` | 共用 Pinia instance | `main.js`、Router guard 使用同一個 instance |
| `frontend/src/api/axios.js` | 統一加 Bearer token、處理 401 | 新頁面直接 import `api`，不要再加 interceptor |
| `frontend/src/utils/auth-session.js` | 讀取／清除 auth session | 不要自行混用 `localStorage['token']` |
| `frontend/src/views/CartView.vue` | 顯示購物車、勾選商品、導向結帳 | API 操作透過 `cartStore` |
| `frontend/src/views/CheckoutView.vue` | 預覽與建立訂單 | 不要從 localStorage 取得 buyerId |
| `frontend/src/router/index.js` | 路由登入限制 | Cart、Checkout 要保留 `requiresAuth: true` |
| `backend/.../SecurityConfig.java` | API 層級權限 | `/api/checkout/**` 應設為 authenticated |

---

## 三、啟動

後端：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

前端另開 Terminal：

```powershell
cd frontend
npm install
npm run dev
```

通常使用 `http://localhost:5173`。

---

## 四、需要 Postman 嗎？

不一定。瀏覽器可驗證完整登入流程；Postman 適合單獨驗證後端 API、JWT Filter 與 API contract。

| 目的 | 工具 |
| --- | --- |
| 登入、路由、Pinia、畫面同步 | 瀏覽器與 DevTools |
| JWT 是否通過 Filter | Postman |
| API request／response | Postman |
| 編譯檢查 | `npm run build` |

---

## 五、瀏覽器手動測試

### 登入與保存

1. 開啟 `/login`，輸入有效帳密並登入。
2. 確認導向 `/member/overview` 或原本的 redirect URL。
3. 確認 UtilityBar 顯示「登出」。
4. DevTools → Application → Session Storage，確認有 `auth` key。
5. `auth` 物件應包含 `token` 與 `member`。

目前不是 `sessionStorage['token']`，而是 persistedstate 的 `sessionStorage['auth']`。

### JWT 自動帶入

1. 開 DevTools → Network。
2. 進入會員資料或購物車頁。
3. 點開 `/api/member/profile` 或 `/api/cart`。
4. Request Headers 應有 `Authorization: Bearer <JWT>`。

所有 API 都必須使用 `frontend/src/api/axios.js` 匯出的 `api` instance。

### 路由、登出與 401

1. 清除 Session Storage 的 `auth`，直接開 `/member/profile`。
2. 應被導向 `/login`。
3. 登入後按「登出」，應回到 `/login` 且 `auth` 被清除。
4. 讓 API 回傳 401 時，Axios 應清除 `auth` 並導向 `/login`。

### 購物車與結帳完整流程

1. 登入後確認可以進入 `/cart`。
2. 確認 Cart API request 的 Authorization 是 `Bearer <token>`。
3. 修改數量、刪除商品，確認畫面與 Header 數量同步。
4. 勾選商品進入 Checkout，確認 `/checkout/preview` 成功。
5. 建立訂單，確認 request 不含 buyerId，後端仍能以登入者建立訂單。
6. 登出後直接開 `/cart` 或 `/checkout`，應被導回登入頁。
7. 用另一個帳號確認看不到前一個帳號的購物車與訂單。

### 會員資料同步

修改會員姓名或電話並儲存，確認 auth store 的 member 同步更新。

進入 `/cart`，確認取得資料、修改數量、刪除商品後畫面更新；重新進入時仍以後端資料為準。依目前決議，Header 不會主動初始化購物車，等組員版本 pull 後再檢查 badge。

---

## 六、Postman JWT 測試

登入：

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "password": "your-password"
}
```

複製 response 的 `token`，再測試：

```http
GET http://localhost:8080/api/member/profile
Authorization: Bearer <JWT>
```

也可測試 `GET /api/cart`、`GET /api/orders/member`。有效 token 應成功；沒有 token、格式錯誤或過期 token 應回傳 401。後端資料範圍應由 JWT 的 `memberId` 決定，不應相信前端傳來的其他 memberId。

---

## 七、Pinia 使用規則

### 登入

```js
const authStore = useAuthStore()
await authStore.signIn(loginForm)
authStore.signOut()
```

不要在頁面中使用：

```js
localStorage.getItem('token')
localStorage.getItem('member')
```

### 購物車

```js
const cartStore = useCartStore()
const { cart, totalQuantity, totalAmount } = storeToRefs(cartStore)

await cartStore.fetchCart()
await cartStore.updateQuantity(item, quantity)
await cartStore.removeItem(item)
```

不要在 `CartView.vue` 重新呼叫 `/cart`、`/cart/items/...`；否則共用購物車狀態可能與 Header badge 不同步。

---

## 八、組員新增頁面或 API

順序：

1. 在 `frontend/src/views/` 新增頁面。
2. 在 `frontend/src/api/` 新增 API module。
3. API 使用共用 instance：

```js
import api from './axios'
const { data } = await api.get('/feature/items')
```

4. 在 `frontend/src/router/index.js` 加 route。
5. 需要登入的 route 加 `meta: { requiresAuth: true }`。
6. 只有跨頁共用狀態才新增 `frontend/src/stores/<feature>.js`。
7. 執行 `npm run build`。

不要自行使用 `import axios from 'axios'` 或 `localStorage.getItem('token')`，否則會繞過 JWT interceptor 或使用錯誤儲存方式。

其他規則：

- API 一律使用 `@/api/axios` 的 `api`，不要在頁面新增 request interceptor。
- 不要把 token 放到新的 storage key。
- 後端需要目前登入者時，從 `@AuthenticationPrincipal AuthenticatedMember` 取得。
- 不要信任前端傳入的 `buyerId`、`memberId` 或 `sellerId`。
- 若新增 API 需要權限，`SecurityConfig.java` 也要加入對應的 `.authenticated()` matcher。
- 若新增 API 回傳商品價格，前端欄位請與後端契約確認；目前購物車單價欄位是 `price`。

適合放 Pinia：登入會員、購物車、收藏、通知數量、跨頁草稿。單頁表單、loading、錯誤訊息與 modal 留在頁面。

---

## 九、組員新增 Store

沿用 setup store 與 `ref`／action／getter。template 使用 state 或 getter 時，用 `storeToRefs` 保持反應式。不要為 auth 或 cart 再新增另一套 localStorage／sessionStorage 規則。

---

## 十、組員新增或修改 JWT Filter

一般新增需要登入的 API，不需要修改 JWT Filter。現有流程是：

```text
Axios Authorization header
→ JwtAuthenticationFilter
→ SecurityContext
→ @AuthenticationPrincipal AuthenticatedMember
→ Controller
```

只有 JWT claim、token 過期規則、角色權限、Bearer 解析規則或受保護路徑改變時，才檢查這些檔案：

```text
backend/src/main/java/com/dinogo/security/JwtTokenUtil.java
backend/src/main/java/com/dinogo/security/JwtAuthenticationFilter.java
backend/src/main/java/com/dinogo/config/SecurityConfig.java
```

Controller 優先使用 `@AuthenticationPrincipal AuthenticatedMember member`，不要用 request parameter 或 body 的 memberId 決定登入者資料範圍。

新增或調整受保護路徑時，依 [security-rules.md](security-rules.md) 確認 HTTP method、公開性、角色、ownership 與 Security integration test；不可只在前端隱藏按鈕。

目前 JWT 的 `roles` claim 已由 Filter 轉成 `ROLE_*` authorities，且 `SecurityConfig` 已使用 `hasRole("SELLER")` 與 `hasRole("ADMIN")`。新增角色或調整既有角色時，仍必須同步檢查 JWT claim、Filter、SecurityConfig、前端 route meta 與後端 security tests。

---

## 十一、Pull 組員版本後

執行以下檢查：

```powershell
git fetch origin
git status
git diff
cd frontend
npm install
npm run build
```

特別檢查共用檔案：

```text
frontend/src/api/axios.js
frontend/src/router/index.js
frontend/src/main.js
frontend/src/stores/index.js
frontend/src/stores/auth.js
frontend/package.json
frontend/package-lock.json
backend/src/main/java/com/dinogo/config/SecurityConfig.java
backend/src/main/java/com/dinogo/security/JwtAuthenticationFilter.java
```

最後至少手測登入、受保護路由、會員 API、購物車 API、登出與失效 JWT。
