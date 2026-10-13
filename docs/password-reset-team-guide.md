# 忘記密碼功能：組員本機設定與測試指南

本文件供組員在本機啟動、用 Postman 驗證，以及手動測試「忘記密碼 → Email 連結 → 重設密碼」流程使用。API 契約以 [member-api.md](member-api.md) 為準；本文件只補充操作步驟與測試情境。

## 1. 本次更新內容

- 新增公開 API：
  - `POST /api/auth/password-reset-requests`：申請寄送重設連結。
  - `POST /api/auth/password-resets`：以連結 token 設定新密碼。
- 新增忘記密碼、設定新密碼前端頁面與登入頁連結。
- 重設 token 使用獨立 `PASSWORD_RESET_SECRET`，預設 15 分鐘有效。
- 成功重設後會遞增會員 `authVersion`，既有 JWT 與先前重設連結都會失效。
- 同一連結使用原子條件更新，只能成功一次。
- 申請端點以 IP 與正規化 Email 限流，預設每 60 秒一次。
- 不論帳號是否存在，或 SMTP 寄信是否失敗，申請端點都回傳相同 `202 Accepted`，避免帳號探測。

## 2. 本機需要新增的設定

請在**不提交 Git 的本機設定**或系統環境變數中加入下列項目。不要把實際密鑰、Gmail App Password 或真實帳號提交到 repository、Postman collection 或截圖。

| 變數 | 是否必填 | 本機建議值／說明 |
| --- | --- | --- |
| `PASSWORD_RESET_SECRET` | 是 | 與 `JWT_SECRET` 不同、至少 32 bytes 的隨機字串。未設定時後端不會啟動。 |
| `FRONTEND_BASE_URL` | 建議設定 | `http://localhost:5173`。寄信連結會導向 `${FRONTEND_BASE_URL}/reset-password?token=...`。 |
| `EMAIL_USERNAME` | 寄信測試必填 | 目前寄件帳號：`service.dinogo@gmail.com`。 |
| `EMAIL_PASSWORD` | 寄信測試必填 | Gmail App Password；不是 Google 帳號登入密碼。 |
| `EMAIL_FROM` | 選填 | 預設 `service.dinogo@gmail.com`。若改用其他地址，必須先在 Gmail 設定為可用的寄件別名（Send mail as）。 |
| `PASSWORD_RESET_EXPIRATION_MS` | 選填 | 預設 `900000`（15 分鐘）；測試過期可暫設 `1000`。 |
| `PASSWORD_RESET_RATE_LIMIT_WINDOW_MS` | 選填 | 預設 `60000`；本機重複測試可暫設較短值，例如 `2000`。 |
| `PASSWORD_RESET_RATE_LIMIT_MAX_KEYS` | 選填 | 預設 `10000`，一般本機不需設定。 |

既有資料庫、JWT、Google OAuth 設定仍照原有本機流程處理。請確認測試會員為 `ACTIVE` 狀態，且知道其原本密碼，才能完整驗證重設前後登入結果。

### 產生本機測試 secret

可自行產生至少 32 bytes 的隨機字串，例如在 PowerShell：

```powershell
[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Maximum 256 }))
```

只將輸出放進本機環境變數或 `application-local.properties`；不要貼到群組、issue、PR 或 Git。

## 3. 啟動前檢查

1. 後端設定完成後，在 `backend` 啟動 Spring Boot。
2. 在 `frontend` 啟動 Vue，確認網址與 `FRONTEND_BASE_URL` 相同，通常是 `http://localhost:5173`。
3. 準備一個有效、可收信的測試會員帳號；建議使用團隊測試帳號，不要使用真實個人帳號。
4. 若只測 API 而不測實際寄信，仍請提供專案既有的 `EMAIL_*` 設定，讓 application config 能正常載入；可使用測試值，但申請已知帳號時不會取得可用 token。

## 4. Postman 完整 API 測試

### 4.1 建立 Postman Environment

| Variable | Initial value | Current value |
| --- | --- | --- |
| `baseUrl` | `http://localhost:8080/api` | `http://localhost:8080/api` |
| `knownEmail` | 測試會員 Email | 測試會員 Email |
| `unknownEmail` | `not-exist@example.com` | `not-exist@example.com` |
| `oldPassword` | 測試會員原密碼 | 測試會員原密碼 |
| `newPassword` | 本次測試新密碼 | 本次測試新密碼 |
| `resetToken` | 留空 | 從 Email 連結複製 token 後填入 |

`baseUrl` 已包含 `/api`，以下 URL 可直接使用 `{{baseUrl}}/...`。

### 4.2 申請重設：已知帳號

```http
POST {{baseUrl}}/auth/password-reset-requests
Content-Type: application/json

{
  "email": "{{knownEmail}}"
}
```

預期：`202 Accepted`

```json
{
  "message": "若此 Email 已註冊，重設密碼說明已寄出。"
}
```

Postman Tests：

```javascript
pm.test('status is 202', () => pm.response.to.have.status(202))
pm.test('returns generic message', () => {
  pm.expect(pm.response.json().message).to.eql('若此 Email 已註冊，重設密碼說明已寄出。')
})
```

### 4.3 申請重設：不存在帳號

```http
POST {{baseUrl}}/auth/password-reset-requests
Content-Type: application/json

{
  "email": "{{unknownEmail}}"
}
```

預期：同樣是 `202`，且 body 與已知帳號相同；不得因帳號不存在而回傳 404、400 或不同訊息。

### 4.4 輸入驗證

```http
POST {{baseUrl}}/auth/password-reset-requests
Content-Type: application/json

{
  "email": "not-an-email"
}
```

預期：`400 Bad Request`，回傳既有欄位驗證錯誤格式。

### 4.5 限流

同一 Email 或同一 IP 在預設 60 秒內再次送出申請：

```http
POST {{baseUrl}}/auth/password-reset-requests
Content-Type: application/json

{
  "email": "{{knownEmail}}"
}
```

預期：第二次為 `429 Too Many Requests`，訊息為 `請稍後再申請重設密碼。`。等待限流時間結束後可再次申請。

### 4.6 從 Email 取得 token

確認信件中的連結格式：

```text
http://localhost:5173/reset-password?token=<token>
```

複製 `token=` 後面的完整內容，填入 Postman Environment 的 `resetToken`。不要把 token 貼到群組訊息、PR、commit 或測試截圖。

### 4.7 設定新密碼

```http
POST {{baseUrl}}/auth/password-resets
Content-Type: application/json

{
  "token": "{{resetToken}}",
  "newPassword": "{{newPassword}}",
  "confirmNewPassword": "{{newPassword}}"
}
```

預期：`204 No Content`，response body 為空。

Postman Tests：

```javascript
pm.test('status is 204', () => pm.response.to.have.status(204))
pm.test('response body is empty', () => pm.expect(pm.response.text()).to.eql(''))
```

### 4.8 驗證登入與單次使用

以既有登入 API 驗證：

```http
POST {{baseUrl}}/auth/login
Content-Type: application/json

{
  "email": "{{knownEmail}}",
  "password": "{{newPassword}}"
}
```

預期：`200 OK`，取得新的 JWT。

接著重送 4.7 的同一 `resetToken`：預期 `400 Bad Request`，訊息為 `重設連結無效或已過期`。再用 `oldPassword` 登入：預期 `401 Unauthorized`。

### 4.9 其他負向案例

| 情境 | 預期 |
| --- | --- |
| `newPassword` 與 `confirmNewPassword` 不同 | `400`，訊息 `新密碼與確認密碼不一致`。 |
| 密碼少於 8、未同時包含英文與數字，或超過 72 個 UTF-8 位元組 | `400` 欄位驗證錯誤。 |
| token 缺失、亂填或已過期 | `400`，訊息 `重設連結無效或已過期`。 |
| 帳號重設後，使用重設前取得的 JWT 呼叫受保護 API | `401`。 |

## 5. 前端手動測試

1. 開啟登入頁，點選「忘記密碼？」。
2. 輸入已知 Email，送出後應顯示通用成功訊息；輸入不存在 Email 也應顯示相同訊息。
3. 開啟信件連結，應進入 `/reset-password?token=...` 頁面。
4. 確認新密碼至少 8 字元、包含英文與數字、最多 72 個 UTF-8 位元組，以及確認密碼不一致提示。
5. 成功更新後，頁面顯示成功訊息並在約 1.5 秒後回到登入頁。
6. 用新密碼登入成功；舊密碼登入失敗。
7. 回到同一封 Email 再送出一次，應顯示連結無效或已過期。
8. 以沒有 token 的 `/reset-password` 直接開頁，送出時應顯示連結無效提示。

## 6. 需要知道的限制與注意事項

- 本機限流是單一後端節點的記憶體實作；未來若多節點部署，需改用共享的 rate-limit store（例如 Redis）。
- 目前直接使用 `HttpServletRequest.getRemoteAddr()` 作為 IP；若部署於 Nginx、Cloudflare 等反向代理後方，需在部署設定可信任的 forwarded headers，否則可能只取得代理 IP。這不是本機測試的必要設定。
- Gmail 需使用 App Password，並確認帳號允許 SMTP 寄送。SMTP 問題時 API 仍會回傳通用 202，請從後端日誌查看 `Password reset email delivery failed` 的錯誤類型。
- 安全設計刻意不告知帳號是否存在，也不在 API 回應中回傳 token；測試 token 只能從測試信箱取得。
- 密碼重設信是帳戶安全通知，不受 F 模組的訂單／行銷通知偏好設定影響；本次沒有修改 F 模組。
- 若改動 URL、request／response、驗證規則或權限，必須同步更新 [member-api.md](member-api.md) 並通知使用此流程的組員。

## 7. 問題回報時請附上

- 使用的 commit hash、後端／前端啟動方式與環境（本機即可，不要附密鑰）。
- 呼叫的 API、HTTP status、去除 token 與個資後的 response body。
- 後端錯誤類型與時間；不要貼 Email、密碼、JWT、reset token、App Password 或 DB 連線字串。
