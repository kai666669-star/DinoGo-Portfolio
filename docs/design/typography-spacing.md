# Usage, Typography, Spacing

## Color Usage

| 使用場景 | Default | Hover | Active | 文字 | 注意事項 |
|---|---|---|---|---|---|
| Primary Button | `--color-primary` | `--color-primary-hover` | `--color-primary-active` | `--color-surface` | focus 使用 `--shadow-focus` |
| Secondary Button | `--color-surface` | `--color-primary-soft` | `--color-secondary-200` | `--color-text` | 必須有 `--color-border` |
| Ghost Button | transparent | `--color-primary-soft` | `--color-primary-100` | `--color-primary-700` | 僅次要工具 |
| Header active item | `--color-primary-soft` | `--color-primary-100` | `--color-primary-200` | `--color-primary-800` | active 底色淡、文字深 |
| MemberNav active item | `--color-surface` | `--color-primary-soft` | `--color-primary-100` | `--color-primary-700` | 層級低於 PrimaryNav |
| SellerNav active item | `--color-primary-soft` | `--color-primary-100` | `--color-primary-200` | `--color-primary-800` | 不與 MemberNav 混用 |
| Card | `--color-surface` | `--color-surface-soft` | `--color-surface-soft` | `--color-text` | border 與 spacing 同等重要 |
| Input | `--color-surface` | `--color-border-strong` | `--color-primary` | `--color-text` | focus 顯示 ring |
| Badge | `--color-primary-soft` | `--color-primary-100` | `--color-primary-200` | `--color-primary-800` | warning 使用 tertiary/state token |
| Disabled | `--color-disabled-bg` | `--color-disabled-bg` | `--color-disabled-bg` | `--color-text-subtle` | 不只降低 opacity |
| Error | `--color-danger-soft` | `--color-danger-soft` | `--color-danger` | `--color-danger` | 需有錯誤文字與 border |
| Success | `--color-success-soft` | `--color-success-soft` | `--color-success` | `--color-success` | 完成狀態需有文字或 icon |
| Warning | `--color-warning-soft` | `--color-warning-soft` | `--color-warning` | `--color-warning` | coupon、待處理狀態 |

## Typography

| Token | Font family | Size | Weight | Line height | 用途 |
|---|---|---:|---:|---:|---|
| `--font-heading` | Noto Serif TC | - | 600–700 | - | brand、hero、重要 page title |
| `--font-body` | Noto Sans TC、system-ui | - | 400–700 | - | UI、表單、nav、button、table |
| `--font-size-xs` | body | 12px | 400 | 1.45 | caption、meta |
| `--font-size-sm` | body | 14px | 400/500 | 1.50 | 次要文字、help、nav label |
| `--font-size-base` | body | 16px | 400 | 1.55 | 一般內文、表單輸入 |
| `--font-size-md` | body | 18px | 500 | 1.45 | card title、section lead |
| `--font-size-lg` | body/heading | 22px | 600 | 1.35 | section title |
| `--font-size-xl` | heading | 28px | 700 | 1.25 | page title |
| `--font-size-2xl` | heading | 36px | 700 | 1.18 | hero、landing block |
| `--font-size-3xl` | heading | 44px | 700 | 1.12 | 品牌主視覺、dashboard title |
| `--line-height-base` | - | - | - | 1.50 | UI 內文 |
| `--line-height-heading` | - | - | - | 1.20 | 標題 |

## Spacing

| Token | Value | 用途 |
|---|---:|---|
| `--space-1` | 4px | icon gap、細部間距 |
| `--space-2` | 8px | row gap、badge padding |
| `--space-3` | 12px | control inner gap |
| `--space-4` | 16px | card padding、小區塊 |
| `--space-5` | 24px | section gap、表單群組 |
| `--space-6` | 32px | page 主要欄距 |
| `--space-7` | 48px | 大區塊分隔 |
| `--space-8` | 64px | landing/footer 垂直留白 |

## Radius

| Token | Value | 用途 |
|---|---:|---|
| `--radius-sm` | 4px | badge、table row、tag |
| `--radius-md` | 6px | button、input、compact card |
| `--radius-lg` | 8px | card、modal |
| `--radius-pill` | 999px | badge、avatar、chip |

## Shadow

| Token | Value | 用途 |
|---|---|---|
| `--shadow-soft` | `0 1px 2px rgba(26,31,46,.04)` | header、footer 輕層次 |
| `--shadow-card` | `0 4px 14px rgba(26,31,46,.06)` | 浮層、dropdown、重要卡片 |
| `--shadow-focus` | `0 0 0 3px rgba(124,144,130,.26)` | input/button focus |
