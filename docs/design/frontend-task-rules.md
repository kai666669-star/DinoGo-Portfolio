# Frontend Task Rules

> 此文件是前端／設計的唯一閱讀路由。只在 Vue、UI、Router、Layout、樣式、Axios、Pinia 或前端 API 串接任務時讀取；後端、資料庫、Git、文件與純 API 任務不需載入。

## 最小載入路由

| 任務類型 | 必讀 | 視需要再讀 |
| --- | --- | --- |
| Vue 頁面／元件／樣式 | `page-implementation-rules.md` | `design-system.md`、`color-tokens.md`、`typography-spacing.md`、`component-spec.md` |
| Router／Layout／導覽 | `router-branch-plan.md` | `component-spec.md`、`page-implementation-rules.md` |
| Axios／Pinia／前端 API 串接 | 本文件的「前端基線」 | Vue teacher reference；不需讀設計文件，除非同時修改 UI |
| 前端 build／除錯 | 本文件的「前端基線」 | 與錯誤直接相關的檔案或文件 |

不要一次載入整個 `docs/design/`、設計參考圖或所有 teacher reference。

## 前端基線

預設：

- Vue 3、Composition API、`<script setup>`、Vite、Bootstrap 5、Axios
- 維持既有架構；不因個別任務改用 Options API 或引入大型 UI framework。
- 不大規模重構 UI、不修改無關組員頁面、不新增不必要 dependency。
- 共用 UI 才抽成 components；頁面級元件放 views。

## Router 規範

Router 統一放在 `frontend/src/router/index.js`，建議維持以下結構：

1. 集中 import `vue-router`、Layout 與必要 route 元件。
2. 宣告獨立的 `routes` 陣列，再呼叫 `createRouter`，最後 `export default router`。
3. 每一筆 route 設定清楚的 `path`、具名 `name` 與 `component`。
4. 父子頁面使用 `children` 巢狀路由，父 Layout 負責 `RouterView`。
5. 導頁優先使用具名 route，避免散落硬編碼 URL。
6. 404 catch-all `/:pathMatch(.*)*` 放在 routes 陣列最後。
7. Layout、共用 shell 與 route guard 留在 Router／Layout 層；view 不重複建立 Header、Footer 或導覽 shell。
8. 頁面 route 預設 dynamic import；只有 Layout 或確實需要立即載入的共用元件使用靜態 import。

新增或調整 route 前，確認 `/member/*`、`/seller/*` 等 Layout 邊界、登入權限、404 fallback 與 route name。若同時改 UI，依「最小載入路由」讀取 `docs/design/` 的相關文件。
