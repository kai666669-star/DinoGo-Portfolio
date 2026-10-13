# Page Implementation Rules

## 實作前必讀

- `docs/design/design-system.md`
- `docs/design/color-tokens.md`
- `docs/design/typography-spacing.md`
- `docs/design/component-spec.md`
- `docs/design/icon-resource-checklist.md`
- `docs/design/router-branch-plan.md`

## Rules

- 不任意 hardcode 顏色、字級、spacing、radius 或 shadow；優先使用 `frontend/src/assets/styles/design-tokens.css`。
- 優先使用既有 Bootstrap 5 class；目前已列於 `frontend/package.json`。新增其他 UI library 仍須由任務明確授權。
- 不使用 Tailwind、React、shadcn component。
- 不在每個 view 重複寫 Header、Footer 或 Layout。
- `MemberNav` 只在 `MemberLayout` 使用；`SellerNav` 只在 `SellerLayout` 使用。
- UI 先使用靜態資料，除非任務明確要求，不串 API。
- 設計稿沒有的功能不得自行新增。
- 只有重複出現或跨頁共用時才抽 component；單頁專用區塊保留在 view。
- 所有可操作元素必須可 keyboard 操作並具 `:focus-visible` 狀態。
- disabled 不只降低 opacity；需使用 disabled background/border/text tokens。
- router/layout 規劃文件不代表本階段要建立所有 view 或 route。
