# Git Task 操作與交付規範

---

# 一、Git 分支規則

建議分支架構：

```
main
└── develop
    ├── feature/member-auth
    ├── feature/product-search
    ├── feature/cart-checkout
    ├── feature/order-payment
    ├── feature/seller-center
    └── feature/chat-service
```

## 分支用途

### main

- 只放可展示、可交付、相對穩定的版本
- 不可直接在 main 開發
- 不可直接 Push 未測試程式
- 只有整合完成並測試通過後，才由組長或指定人員合併

### develop

- 六人功能整合分支
- 各功能分支完成後，先合併到 develop
- develop 測試通過後，再合併到 main
- 原則上也不直接在 develop 開發

### feature 分支

每位組員在自己的功能分支開發。

範例：

```
feature/member-auth
feature/product-search
feature/cart-checkout
feature/order-payment
feature/seller-center
feature/chat-service
```

若功能較大，可以再細分：

```
feature/product-search-api
feature/product-detail-page
feature/cart-api
feature/checkout-page
```

---

# 二、每個 Task 開始或接續前

每個 Task 開始或因 session 切換而接續前，都要先確認 Git 與遠端最新狀態。一個 Task 可以跨多個 session，不以日期為界。

## 1. 確認目前所在資料夾

在 VS Code Terminal 輸入：

```bash
git status
```

確認目前位於 DinoGo Repository，且沒有未處理的錯誤。

---

## 2. 確認目前分支

```bash
git branch
```

目前分支前面會有 `*`。

例如：

```
* feature/product-search
  develop
  main
```

確認自己不是在 `main` 或 `develop` 直接開發。

---

## 3. 儲存或處理未完成的 Task 修改

如果 `git status` 顯示還有未提交內容，不要直接 Pull。

可以選擇：

### 方法 A：先 Commit

```bash
git add .
git commit -m "wip: save unfinished product search work"
```

`wip` 代表 Work In Progress，只建議用在自己的 feature 分支。

### 方法 B：暫存修改

```bash
git stash
```

同步完成後恢復：

```bash
git stash pop
```

---

## 4. 更新遠端分支資訊

```bash
git fetch origin
```

這個指令只會更新遠端資訊，不會直接修改目前檔案。

---

## 5. 先更新 develop

切換到 develop：

```bash
git switch develop
```

更新 develop：

```bash
git pull origin develop
```

---

## 6. 回到自己的 feature 分支

例如：

```bash
git switch feature/product-search
```

將最新 develop 合併進自己的分支：

```bash
git merge develop
```

如果沒有衝突，就可以開始開發。

---

# 三、開發中的 Commit 規範

## 1. 不要累積一整天才 Commit

建議每完成一個小功能就 Commit 一次。

例如：

- 完成 Entity
- 完成 Repository
- 完成 API
- 完成前端頁面
- 完成 API 串接
- 修正一個 Bug

不要把完全不同的修改全部放在同一個 Commit。

---

## 2. Commit 前先檢查修改內容

```bash
git status
```

查看哪些檔案被修改。

也可以使用：

```bash
git diff
```

查看尚未加入暫存區的修改。

---

## 3. 加入暫存區

全部加入：

```bash
git add .
```

只加入指定檔案：

```bash
git add frontend/src/views/ProductList.vue
```

建議先確認修改內容，再執行 `git add .`。

---

## 4. Commit Message 格式

建議格式：

```
<type>: <簡短描述>
```

常用類型：

| Type | 用途 |
| --- | --- |
| feat | 新增功能 |
| fix | 修正 Bug |
| refactor | 重構，不改變功能 |
| docs | 修改文件 |
| style | 格式、排版、CSS 調整 |
| test | 新增或修改測試 |
| chore | 設定、依賴或其他維護工作 |
| wip | 尚未完成的暫存版本 |

範例：

```
feat: add product search api
feat: create product detail page
feat: implement cart quantity update
fix: resolve duplicate email registration
fix: correct order total calculation
refactor: simplify product service
docs: update backend setup guide
style: adjust seller dashboard layout
chore: update frontend dependencies
```

不建議：

```
update
test
123
修改
完成
new
```

因為之後看不出修改內容。

---

# 四、Task 開發中的 Push 規範

## 1. Push 前先確認目前分支

```bash
git branch
```

確認不是 main 或 develop。

---

## 2. Push 自己的 feature 分支

第一次 Push：

```bash
git push -u origin feature/product-search
```

之後：

```bash
git push
```

---

## 3. Task 階段完成時 Push

即使功能尚未全部完成，只要程式沒有明顯破壞，也應該在自己的 feature 分支留下可接續的進度。

建議時機：

- 中途完成一個階段時 Push
- Task 暫停或切換 session 前 Push
- 重大修改前先 Push 備份

---

## 4. 不可直接 Push 到 main

禁止：

```bash
git push origin main
```

除非是組長或被指定負責發布的人員，而且已經完成測試。

---

# 五、Task 完成／暫停交付規範

Task 完成、暫停或準備切換 session 時，依 Task 狀態完成下列適用事項。

## 1. 確認程式可以啟動

### Backend

確認 Spring Boot 可以啟動，沒有編譯錯誤。

```bash
mvn spring-boot:run
```

或使用 IDE 啟動。

### Frontend

確認 Vue 可以啟動。

```bash
npm run dev
```

必要時也執行：

```bash
npm run build
```

---

## 2. 完成基本測試

依負責內容測試：

- API 是否可成功呼叫
- 頁面是否可正常顯示
- 資料是否正確寫入資料庫
- 錯誤輸入是否有適當回應
- 是否影響其他既有功能

---

## 3. Commit 已完成的 Task 修改

```bash
git add .
git commit -m "feat: complete product list pagination"
```

---

## 4. Push 到自己的遠端分支

```bash
git push
```

---

## 5. 確認 GitHub 有更新

到 GitHub 切換到自己的分支，確認：

- 最新 Commit 已出現
- 修改檔案已出現
- 分支名稱正確
- 沒有誤 Push 到 main

---

## 6. 回報 Task 狀態

建議使用以下格式：

```
Task：
負責模組：
Git 分支：

已完成：

目前修改／Commit：

測試：

未完成／Blocker：

需要其他組員協助：

下一步：
```

---

# 六、功能完成後的 Pull Request 規範

功能完成後，不要直接把自己的 feature 分支強制合併到 main。

流程：

```
feature 分支
↓
Pull Request
↓
develop
↓
整合測試
↓
main
```

## 1. 建立 Pull Request

GitHub 上選擇：

```
base: develop
compare: feature/你的分支
```

例如：

```
base: develop
compare: feature/product-search
```

---

## 2. Pull Request 標題

建議格式：

```
feat: complete product search module
```

---

## 3. Pull Request 說明

```markdown
## 完成內容

-完成商品關鍵字搜尋
-完成分類與品牌篩選
-完成價格與銷量排序
-完成每頁 12 / 24 筆切換

## API

-GET /products
-GET /categories
-GET /brands

## 測試方式

1.啟動 Backend
2.啟動 Frontend
3.進入商品列表頁
4.測試搜尋與篩選

## 注意事項

-需要先建立測試商品資料
-API 回傳格式有新增 brandName 欄位
```

---

## 4. 合併前檢查

- [ ]  程式可啟動
- [ ]  功能已測試
- [ ]  沒有上傳密碼或帳號
- [ ]  沒有上傳 `.env`
- [ ]  沒有上傳 `application-local.properties`
- [ ]  沒有上傳 `node_modules`
- [ ]  沒有上傳 `target`
- [ ]  API 修改已通知其他組員
- [ ]  沒有大量無關格式修改

---

# 七、合併與更新規範

## 1. Feature 合併到 Develop 後

自己的分支要更新最新 develop：

```bash
git switch develop
git pull origin develop
git switch feature/product-search
git merge develop
```

---

## 2. 合併後不要立刻刪本機分支

確認：

- Pull Request 已成功合併
- develop 功能正常
- GitHub 上程式完整

再刪除。

刪除本機分支：

```bash
git branch -d feature/product-search
```

刪除遠端分支：

```bash
git push origin --delete feature/product-search
```

若後續還要繼續開發，可以保留原分支，或建立下一個更明確的新分支。

---

# 八、Git 衝突處理規範

## 發生衝突時不要做的事

- 不要隨便刪除整個檔案
- 不要直接選 Accept All
- 不要使用 `git push --force`
- 不要重複按 Undo Commit
- 不要在不理解狀態時執行 `reset --hard`
- 不要重新初始化 `git init`

---

## 正確處理方式

先查看：

```bash
git status
```

VS Code 會標出衝突檔案。

衝突內容通常會顯示：

```
接受目前變更:
自己的內容

接受來源變更:
其他分支內容
```

必須人工確認：

- 要保留自己的內容
- 要保留對方內容
- 或將兩邊內容整合

修改完成後：

```bash
git add 衝突檔案
git commit -m "merge: resolve conflict with develop"
```

再 Push：

```bash
git push
```

如果不確定怎麼處理，先把 `git status` 與衝突畫面貼到群組，不要自行亂按。

---

# 九、禁止上傳的內容

以下檔案不得提交到 GitHub：

```
node_modules/
dist/
target/
.env
.env.*
application-local.properties
application-local.yml
*.log
.idea/
.vscode/（除非團隊決定共享設定）
```

也不得上傳：

- 資料庫密碼
- JWT Secret
- API Key
- Email 密碼
- 個人 Access Token
- 真實會員個資
- 教室電腦連線密碼

提交前確認：

```bash
git status
```

若敏感資料已被加入暫存區：

```bash
git restore --staged 檔案名稱
```

---

# 十、資料庫更新規範

資料庫結構不能只在個人電腦手動修改，必須同步留下 SQL 檔案。

建議目錄：

```
database/
├── migrations/
├── seed/
└── README.md
```

Migration 命名範例：

```
V001__create_member_tables.sql
V002__create_product_tables.sql
V003__add_product_status.sql
V004__create_cart_tables.sql
```

每次資料庫欄位異動，必須：

1. 新增 SQL Migration
2. Commit 到 Git
3. 在群組通知其他人
4. 說明新增、修改或刪除哪些欄位
5. 說明是否影響 API

禁止只在 SSMS 修改，卻沒有留下 SQL 紀錄。

---

# 十一、API 修改規範

API 若有以下修改，必須通知會使用該 API 的組員：

- URL 修改
- HTTP Method 修改
- Request 欄位修改
- Response 欄位修改
- 資料型態修改
- 欄位名稱修改
- 驗證規則修改
- 權限修改

通知格式：

```
API 更新通知

負責模組：
API：
修改日期：

修改前：
修改後：

影響人員：
需要配合修改：
```

---

# 十二、Task Git 標準流程

## Task 開始或接續

```bash
git status
git branch
git fetch origin
git switch develop
git pull origin develop
git switch feature/自己的分支
git merge develop
```

## 開發中

```bash
git status
git add .
git commit -m "feat: 完成的功能"
git push
```

## Task 完成、暫停或切換 session

```bash
git status
# 依 Task 狀態執行必要的 commit / push
```

整理 Task handoff：已完成、目前修改、測試、Git 狀態、未完成／blocker 與下一步。若已 push，再到 GitHub 確認最新 Commit 已出現。

---

# 十三、Task handoff 檢查表

每位組員在 Task 完成、暫停或切換 session 前確認：

- [ ]  Task 是在自己的 feature 分支開發
- [ ]  Task 開始或接續前已確認遠端狀態
- [ ]  已完成範圍的程式可以正常啟動
- [ ]  已完成範圍有基本測試
- [ ]  已完成範圍已視需要 Commit
- [ ]  Commit Message 清楚
- [ ]  已視需要 Push 到 GitHub
- [ ]  已 Push 時，GitHub 可看到最新 Commit
- [ ]  沒有 Push 到錯誤分支
- [ ]  沒有上傳帳號、密碼或金鑰
- [ ]  資料庫修改有附 SQL
- [ ]  API 修改有通知相關組員
- [ ]  已回報 Task 狀態
- [ ]  已寫下下一步

---

# 十四、發生問題時的處理順序

發生 Git 問題時，先停止操作並依序執行：

```bash
git status
git branch
git remote -v
git log --oneline --all --graph -10
```

將結果貼到群組或交給熟悉 Git 的組員判斷。

在尚未確認問題前，禁止執行：

```bash
git reset --hard
git push --force
git init
rm -rf .git
```

這些操作可能造成程式或 Git 歷史遺失。
