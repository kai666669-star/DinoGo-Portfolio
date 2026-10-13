# DinoGo G-UI Design System

## 目的與來源

本規範將 `docs/design/references/` 的 G-UI Design System 圖片轉為 DinoGo 可實作的 CSS tokens、版面規則與共用元件邊界。圖片是色號與視覺規格的主要來源；若圖片文字與轉錄需求衝突，以圖片為準，並在 `color-tokens.md` 註記差異。

## 使用原則

- 顏色、字級、間距、圓角與陰影優先使用 `frontend/src/assets/styles/design-tokens.css` 的 CSS variables。
- UI 操作文字、表單、導覽、表格與按鈕使用 sans-serif；品牌標題、hero heading 與重要 page title 才使用 Noto Serif TC。
- Vue 3 頁面以 Bootstrap 5 utility/component class 為基礎，必要時補充原生 CSS/SCSS。
- 本規範不使用 Tailwind、React 或 shadcn component。
- Header、Footer、Layout、MemberNav 與 SellerNav 屬全站共用骨架；頁面不得重複實作。
- 現階段只規劃 layout shell 與導航；頁面專用區塊先留在 view，跨頁重複後才抽成 component。
- 互動元件必須提供 default、hover、focus-visible、active、disabled，以及適用時的 loading、error、empty 狀態。
- 小尺寸文字不可使用對比不足的色彩；詳見 `icon-resource-checklist.md` 的 accessibility checklist。

## 文件索引

- `color-tokens.md`：原始色階與語意色。
- `typography-spacing.md`：色彩使用、字型、字級、間距、圓角與陰影。
- `component-spec.md`：必要共用元件的責任、位置與狀態。
- `icon-resource-checklist.md`：icon 規格、資源索引與交付檢查。
- `page-implementation-rules.md`：Codex/Agent 實作限制。
- `router-branch-plan.md`：Layout route 與分支交付順序。

## 專案位置

以下路徑皆相對於 repository root `DinoGo/`：

- 前端程式：`frontend/src/`
- 設計 tokens：`frontend/src/assets/styles/design-tokens.css`
- 設計參考圖：`docs/design/references/`
