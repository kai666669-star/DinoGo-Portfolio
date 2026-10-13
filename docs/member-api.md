# 會員與帳號 API

本文件以目前後端實作為準，供前端與其他模組串接使用。

## 共通規則

- Base URL：`/api`
- Request body 使用 `Content-Type: application/json`。
- `/auth/**` 為公開 API；其餘本文件中的 API 都需要登入。
- 受保護 API 必須帶入：`Authorization: Bearer <token>`。
- `memberId` 一律從 JWT 取得，request body 與 URL 不接受前端指定的會員 ID。
- JWT 另有 `roles` claim，內容為 `buyer`、`seller`、`admin` 等小寫角色名稱陳列。
- Token 預設有效期為 1 小時，可由後端 `jwt.expiration-ms` 設定覆寫。成功變更密碼後，所有既有 token 會立即失效。
- 未登入、JWT 缺少、格式錯誤或過期時，Security 層回傳 HTTP 401；已登入但角色不足時回傳 HTTP 403。這兩種 Security 回應目前可能為純文字，前端應以 HTTP status 判斷。

## 共用回應格式

### 會員資料 `MemberResponse`

```json
{
  "memberId": 1,
  "email": "member@example.com",
  "lastName": "王",
  "firstName": "小明",
  "birthDate": "2000-01-01",
  "phone": "0912345678",
  "emailOrderNotifications": true,
  "emailMarketingNotifications": false,
  "status": "ACTIVE",
  "createdAt": "2026-08-17T10:00:00",
  "updatedAt": "2026-08-17T10:00:00"
}
```

`birthDate`、`phone`、`createdAt`、`updatedAt` 可能為 `null`；日期使用 ISO-8601 格式。`emailOrderNotifications` 預設為 `true`，`emailMarketingNotifications` 預設為 `false`。

### 地址資料 `AddressResponse`

```json
{
  "addressId": 1,
  "receiverName": "王小明",
  "receiverPhone": "0912345678",
  "postalCode": "100",
  "city": "台北市",
  "district": "中正區",
  "detailAddress": "忠孝西路一段 1 號",
  "isDefault": true
}
```

### A 模組業務與驗證錯誤

註冊、登入、會員資料與地址 API 的 Controller 回傳下列 JSON 格式：

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "輸入資料驗證失敗",
  "fieldErrors": {
    "email": "must be a well-formed email address"
  }
}
```

- `fieldErrors` 只在 request 欄位驗證失敗時帶入欄位訊息；其他情況為空物件 `{}`。
- JSON 無法解析時，`message` 為 `請求內容格式錯誤`。
- URL 中的 `addressId` 小於等於 0 時，`message` 為 `請求參數驗證失敗`。
- 缺少、格式錯誤或過期的 JWT，以及角色不足，皆由 Security 層處理；前端應以 HTTP status 判斷登入失效（401）或權限不足（403），不應假設它是上述 JSON 格式。

## 認證 API

### 註冊

`POST /api/auth/register`

Request：

```json
{
  "email": "member@example.com",
  "password": "password123",
  "confirmPassword": "password123",
  "lastName": "王",
  "firstName": "小明",
  "birthDate": "2000-01-01",
  "phone": "0912345678"
}
```

| 欄位 | 必填 | 規則 |
| --- | --- | --- |
| `email` | 是 | Email 格式，最長 100 字 |
| `password` | 是 | 至少 8 字元、須包含英文與數字、最多 72 個 UTF-8 位元組 |
| `confirmPassword` | 是 | 至少 8 字元、須包含英文與數字、最多 72 個 UTF-8 位元組，且必須與 `password` 相同 |
| `lastName` | 是 | 最長 50 字 |
| `firstName` | 是 | 最長 50 字 |
| `birthDate` | 否 | `YYYY-MM-DD` |
| `phone` | 否 | 最長 20 字 |

成功：`200 OK`

```json
{
  "member": {
    "memberId": 1,
    "email": "member@example.com",
    "lastName": "王",
    "firstName": "小明",
    "birthDate": "2000-01-01",
    "phone": "0912345678",
    "emailOrderNotifications": true,
    "emailMarketingNotifications": false,
    "status": "ACTIVE",
    "createdAt": "2026-08-17T10:00:00",
    "updatedAt": "2026-08-17T10:00:00"
  }
}
```

失敗：

| Status | `message` | 時機 |
| --- | --- | --- |
| 400 | `輸入資料驗證失敗` | 欄位不符合格式或長度 |
| 400 | `密碼與確認密碼不一致` | 兩個密碼不同 |
| 400 | `請求內容格式錯誤` | JSON 格式錯誤 |
| 409 | `Email 已被註冊` | Email 已存在 |

### 登入

`POST /api/auth/login`

Request：

```json
{
  "email": "member@example.com",
  "password": "password123"
}
```

| 欄位 | 必填 | 規則 |
| --- | --- | --- |
| `email` | 是 | Email 格式，最長 100 字 |
| `password` | 是 | 不可空白 |

成功：`200 OK`

```json
{
  "token": "<jwt>",
  "roles": ["buyer"],
  "sellerId": null,
  "member": {
    "memberId": 1,
    "email": "member@example.com",
    "lastName": "王",
    "firstName": "小明",
    "birthDate": "2000-01-01",
    "phone": "0912345678",
    "emailOrderNotifications": true,
    "emailMarketingNotifications": false,
    "status": "ACTIVE",
    "createdAt": "2026-08-17T10:00:00",
    "updatedAt": "2026-08-17T10:00:00"
  }
}
```

失敗：

| Status | `message` | 時機 |
| --- | --- | --- |
| 400 | `輸入資料驗證失敗` | Request 欄位不合法 |
| 400 | `請求內容格式錯誤` | JSON 格式錯誤 |
| 401 | `Email 或密碼錯誤` | Email 不存在、密碼錯誤或帳號不是 `ACTIVE` |

登入成功後，將 `token` 儲存並加到後續請求的 `Authorization` header；`roles` 可供前端導覽與路由判斷使用，但後端授權仍只信任 JWT claim。`sellerId` 僅在目前會員有 `ACTIVE` 商家資料時回傳其 ID，否則為 `null`；它可用於賣家中心流程，但後端仍必須由 JWT principal 驗證商家身分與資料 ownership。不要把密碼或完整 token 寫入 log。

### Google 登入與帳號綁定

前端使用 Google Identity Services 取得 ID Token 後，將 token 放入 `credential` 欄位送至後端。後端會驗證簽章、issuer、有效期限與 OAuth Web Client 的 audience；不接受前端提供的 Email 或 Google user ID。

`credential` 必填，最長 10,000 字元。

`POST /api/auth/google`

```json
{
  "credential": "<Google ID Token>"
}
```

成功時回傳格式與一般登入相同的 `LoginResponse`。首次 Google 登入且 Email 不存在時，系統建立預設 `buyer` 會員與 `member.MemberOAuthAccount` 對照；既有 Google 對照帳號則直接登入。

| Status | `message` | 時機 |
| --- | --- | --- |
| 401 | `Google 登入憑證無效` | token 無效、過期或驗證失敗 |
| 401 | `Google 帳號尚未驗證 Email` | Google 未宣告 email 已驗證 |
| 401 | `Google 帳號無法登入` | 已綁定會員不是 `ACTIVE` |
| 409 | `此 Email 已有密碼帳號，請輸入原密碼完成 Google 帳號綁定` | Email 已有一般帳號、尚未綁定 Google |

為避免以同 Email 自動合併而造成帳號接管，既有密碼帳號必須由本人輸入原密碼完成綁定：

`POST /api/auth/google/link`

```json
{
  "credential": "<Google ID Token>",
  "password": "existing-password"
}
```

成功時同樣回傳 `LoginResponse`。失敗時為 401（帳密或憑證不正確）或 409（Google 帳號已綁定其他會員）。ID Token 僅可用於本次驗證，不得存入前端 storage、資料庫或日誌。

### 忘記密碼與重設密碼

這兩個 API 都是公開 API。密碼重設信屬帳戶安全通知，不受訂單／行銷 Email 偏好設定影響。

`POST /api/auth/password-reset-requests`

```json
{
  "email": "member@example.com"
}
```

`email` 必填、須符合 Email 格式且最長 100 字。成功一律回傳 `202 Accepted` 與下列訊息，不論帳號是否存在或是否可重設，避免透過 API 探測帳號：

```json
{
  "message": "若此 Email 已註冊，重設密碼說明已寄出。"
}
```

重設連結有效期限預設為 15 分鐘，由 `PASSWORD_RESET_EXPIRATION_MS` 覆寫；`PASSWORD_RESET_SECRET` 必須為與 JWT secret 分離、至少 32 bytes 的部署密鑰。不得將連結 token 寫入 log、前端 storage 或資料庫。

每個 IP 與正規化 Email 預設每 60 秒只能申請一次，可用 `PASSWORD_RESET_RATE_LIMIT_WINDOW_MS` 覆寫；記憶體中的限流 key 預設上限為 10,000，可用 `PASSWORD_RESET_RATE_LIMIT_MAX_KEYS` 覆寫。超限時回傳 `429 Too Many Requests`。

`POST /api/auth/password-resets`

```json
{
  "token": "<password-reset-token>",
  "newPassword": "new-password",
  "confirmNewPassword": "new-password"
}
```

| 欄位 | 必填 | 規則 |
| --- | --- | --- |
| `token` | 是 | Email 重設連結提供的 token。 |
| `newPassword` | 是 | 至少 8 字元、須包含英文與數字、最多 72 個 UTF-8 位元組。 |
| `confirmNewPassword` | 是 | 至少 8 字元、須包含英文與數字、最多 72 個 UTF-8 位元組，且必須與 `newPassword` 相同。 |

成功：`204 No Content`。成功後該會員所有既有 JWT 及所有先前重設連結均立即失效。

失敗：`400`；密碼不一致時訊息為 `新密碼與確認密碼不一致`，token 無效、過期、帳號已停用或已因其他密碼變更失效時統一回傳 `重設連結無效或已過期`。

## 會員資料 API

以下 API 都需要 `Authorization: Bearer <token>`。

### 註銷目前帳號

`POST /api/member/account/deactivate`

```json
{ "currentPassword": "current-password" }
```

成功回傳 `204 No Content`，帳號狀態改為 `DEACTIVATED`，並立即使所有 JWT 失效。商家會員或有 `PENDING_PAYMENT`、`PAID`、`PROCESSING`、`SHIPPED` 買家訂單時回傳 `400`，不得自行註銷。

## 管理員會員 API

以下 API 需 `admin` 角色。

- `GET /api/admin/members?status=ACTIVE|SUSPENDED|DEACTIVATED&keyword=`：會員清單。
- `POST /api/admin/members/{memberId}/suspend`：body `{ "reason": "..." }`，停權帳號並使其既有 JWT 失效。
- `POST /api/admin/members/{memberId}/restore`：恢復已停權帳號。

帳號狀態僅接受 `ACTIVE`、`SUSPENDED`、`DEACTIVATED`。管理員不可停權或恢復自己的帳號；`DEACTIVATED` 帳號不可由此 API 恢復。

### 取得目前會員資料

`GET /api/member/profile`

成功：`200 OK`，body 為 `MemberResponse`。

失敗：

| Status | `message` | 時機 |
| --- | --- | --- |
| 401 | 依 Security 層回應 | 未登入或 token 無效 |
| 404 | `Member not found` | JWT 對應會員不存在 |

### 修改目前會員資料

`PUT /api/member/profile`

Request：

```json
{
  "lastName": "王",
  "firstName": "小明",
  "birthDate": "2000-01-01",
  "phone": "0912345678",
  "emailOrderNotifications": true,
  "emailMarketingNotifications": false
}
```

| 欄位 | 必填 | 規則 |
| --- | --- | --- |
| `lastName` | 是 | 最長 50 字 |
| `firstName` | 是 | 最長 50 字 |
| `birthDate` | 否 | `YYYY-MM-DD` |
| `phone` | 否 | 最長 20 字 |
| `emailOrderNotifications` | 否 | `true` 時接收訂單狀態 Email；省略時維持既有設定 |
| `emailMarketingNotifications` | 否 | `true` 時接收行銷 Email；省略時維持既有設定 |

成功：`200 OK`，body 為更新後的 `MemberResponse`。

失敗：400 欄位驗證、401 未登入或 token 無效、404 JWT 對應會員不存在。

### 變更密碼

`PUT /api/member/password`

Request：

```json
{
  "currentPassword": "current-password",
  "newPassword": "new-password",
  "confirmNewPassword": "new-password"
}
```

| 欄位 | 必填 | 規則 |
| --- | --- | --- |
| `currentPassword` | 是 | 不可空白 |
| `newPassword` | 是 | 至少 8 字元、須包含英文與數字、最多 72 個 UTF-8 位元組，且不可與目前密碼相同 |
| `confirmNewPassword` | 是 | 至少 8 字元、須包含英文與數字、最多 72 個 UTF-8 位元組，且必須與 `newPassword` 相同 |

成功：`204 No Content`。成功後系統會讓該會員全部裝置上的既有 JWT 立即失效，前端應清除目前保存的 token 並導回登入頁。

失敗：400 欄位驗證、`目前密碼錯誤` 或 `新密碼與確認密碼不一致`；401 未登入或 token 無效；404 JWT 對應會員不存在。

## 收件地址 API

以下 API 都需要 `Authorization: Bearer <token>`。

地址建立與修改共用下列 Request：

```json
{
  "receiverName": "王小明",
  "receiverPhone": "0912345678",
  "postalCode": "100",
  "city": "台北市",
  "district": "中正區",
  "detailAddress": "忠孝西路一段 1 號",
  "isDefault": true
}
```

| 欄位 | 必填 | 規則 |
| --- | --- | --- |
| `receiverName` | 是 | 最長 100 字 |
| `receiverPhone` | 是 | 最長 20 字 |
| `postalCode` | 否 | 最長 10 字 |
| `city` | 是 | 最長 10 字 |
| `district` | 是 | 最長 10 字 |
| `detailAddress` | 是 | 最長 255 字 |
| `isDefault` | 否 | `true`、`false` 或省略 |

規則：新增第一筆地址時會自動成為預設地址；更新時省略 `isDefault` 代表維持原設定。

### 取得目前會員的地址清單

`GET /api/addresses`

成功：`200 OK`，回傳 `AddressResponse` 陣列，預設地址排在第一筆。

### 取得單筆地址

`GET /api/addresses/{addressId}`

成功：`200 OK`，body 為 `AddressResponse`。

### 新增地址

`POST /api/addresses`

body 使用上述地址 Request。

成功：`201 Created`，body 為建立後的 `AddressResponse`。

### 修改地址

`PUT /api/addresses/{addressId}`

body 使用上述地址 Request。

成功：`200 OK`，body 為更新後的 `AddressResponse`。

### 刪除地址

`DELETE /api/addresses/{addressId}`

成功：`204 No Content`，沒有 response body。

地址 API 的共同失敗情況：

| Status | `message` | 時機 |
| --- | --- | --- |
| 400 | `輸入資料驗證失敗` | 建立或修改的 body 欄位不合法 |
| 400 | `請求內容格式錯誤` | JSON 格式錯誤 |
| 400 | `請求參數驗證失敗` | `addressId` 小於等於 0 |
| 401 | 依 Security 層回應 | 未登入或 token 無效 |
| 404 | `Address not found` | 地址不存在或不屬於目前登入會員 |
| 409 | `此地址已被訂單使用，無法刪除` | 刪除的地址已被訂單引用 |

## 跨模組串接注意事項

- C（購物車／結帳）可用 `GET /api/addresses` 取得登入會員地址；建立訂單時仍應由 D 的訂單 API 定義其 request 欄位，勿直接使用 A 的 Entity。
- E（賣家）與 F（聊天室／客服）需要目前登入會員時，應由 JWT 的 `memberId` 取得身分，不接受前端自行指定其他會員 ID。
- JWT 同時提供 `memberId` 與 `roles` claim。各模組可依 `roles` 顯示導覽項目，但後端必須依 Spring Security authorities 執行實際授權。
- Google 登入成功後的 JWT 格式、`memberId` 與 `roles` claim 均與密碼登入相同；C（購物車）、D（訂單）、E（賣家）與 F（聊天室／客服）不需修改既有受保護 API 串接，但應各自 smoke test 登入後的會員／賣家流程。
