# Icon Usage, Resource Index, Delivery Checklist

## Icon Usage

| 使用場景 | 建議 icon | Stroke/fill | Size | Color token |
|---|---|---|---:|---|
| Header 收藏 | heart / bookmark | stroke 1.75、no fill | 20–22px | `--color-text-muted` / active `--color-primary` |
| Header 通知 | bell | stroke 1.75、no fill | 20–22px | `--color-text-muted` / badge `--color-danger` |
| Header 購物車 | shopping-cart | stroke 1.75、no fill | 20–22px | `--color-text-muted` / badge `--color-primary` |
| Header 會員中心 | user-circle | stroke 1.75、no fill | 20–22px | `--color-text-muted` |
| PrimaryNav 全部分類 | grid / menu | stroke 1.75 | 18–20px | `--color-primary-800` |
| MemberNav 總覽 | layout-dashboard | stroke 1.75 | 18px | `--color-text-muted` / active `--color-primary` |
| MemberNav 訂單 | package / receipt | stroke 1.75 | 18px | `--color-text-muted` |
| MemberNav 收藏 | heart | stroke 1.75 | 18px | `--color-text-muted` |
| MemberNav 優惠券 | ticket | stroke 1.75 | 18px | `--color-text-muted` / `--color-warning` |
| MemberNav 訊息 | message-circle | stroke 1.75 | 18px | `--color-text-muted` |
| 帳戶設定 | settings / user-cog | stroke 1.75 | 18px | `--color-text-muted` |
| 商家中心 | store / layout-panel-left | stroke 1.75 | 18–20px | `--color-text-muted` / active `--color-primary` |
| 表單錯誤 | alert-circle | stroke 1.75 | 16–18px | `--color-danger` |
| 成功狀態 | check-circle | stroke 1.75 | 18–24px | `--color-success` |
| 空狀態 | inbox / search | stroke 1.5 | 40–56px | `--color-text-subtle` |
| Loading | loader / refresh-cw | stroke 1.75 | 18–24px | `--color-primary` |
| 右箭頭/dropdown | chevron-right / chevron-down | stroke 1.75 | 16–18px | `--color-text-subtle`、hover `--color-primary` |

Icon 必須保留至少 40px hit area（Header 購物車與 icon button 特別重要），不可用 badge 取代收藏 icon；所有 icon 的 stroke、尺寸與 token 顏色需一致。

## Resource Index

| 類別 | 資源 | URL | 使用限制 |
|---|---|---|---|
| 字型 | Noto Serif TC | https://fonts.google.com/noto/specimen/Noto+Serif+TC | 品牌/hero/page title；不可大量用於 UI |
| 色調參考 | Sage Garden | https://21st.dev/@serafimcloud/themes/sage-garden | 僅方向參考，不直接導入 Tailwind/shadcn tokens |
| UI Skills | UI UX Pro Max Skill | https://github.com/nextlevelbuilder/ui-ux-pro-max-skill | 只作 UI/UX 與 accessibility 檢查 |
| Icons | The SVG | https://thesvg.org/ | 需確認授權與線性風格 |
| Icons | Ant Design Icons | https://antdv.com/components/icon-cn | 僅結構參考，不導入 React |
| UI reference | Uiverse | https://uiverse.io/ | 僅互動靈感，不導入 Tailwind/React |

## Delivery Checklist

- [ ] 每個 CSS token 都有圖片色號對照與用途。
- [ ] heading、body、label、caption 與中文可讀性已確認。
- [ ] hover、active、focus-visible、disabled、error、empty 狀態已規劃。
- [ ] icon size、stroke/fill、color token 一致。
- [ ] RWD 檢查 375px、768px、1024px、1440px。
- [ ] accessibility 檢查 focus、keyboard navigation、form error、empty state。
- [ ] 不依賴 Tailwind、React、shadcn。
- [ ] 若要使用 Bootstrap 5，先確認 dependency 已由專案決定安裝。

## 對比備註

`--color-primary-500` 白字對比約 3.40:1，`--color-text-subtle` 在 `--color-bg` 上約 3.08:1；兩者不適合一般小尺寸正文。實作時需改用較深 token 或僅用於大型文字/裝飾用途，不修改原始 token。
