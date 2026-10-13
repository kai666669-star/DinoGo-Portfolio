# Color Tokens

以下色階依 G-UI Color Tokens reference 建立。`--color-primary-50`、`--color-success-bg`、`--color-warning` 的圖片讀值與原始文字需求曾有差異，本文件採圖片讀值。

## Primary / Sage Green

| Token | Hex | 用途 |
|---|---|---|
| `--color-primary-50` | `#F2F3EF` | very light sage surface |
| `--color-primary-100` | `#E4E8DF` | active item 淡背景、focus surface |
| `--color-primary-200` | `#CED8C8` | soft border、pale background |
| `--color-primary-300` | `#B4C2AD` | secondary sage |
| `--color-primary-400` | `#97A88F` | muted sage |
| `--color-primary-500` | `#7C9082` | primary default |
| `--color-primary-600` | `#657A6D` | primary hover |
| `--color-primary-700` | `#526559` | selected、active route |
| `--color-primary-800` | `#3F4E45` | dark section |
| `--color-primary-900` | `#2E3A33` | footer、dark header |
| `--color-primary-950` | `#18201A` | deepest text、dark background |

## Secondary / Warm Neutral

| Token | Hex |
|---|---|
| `--color-secondary-50` | `#FAFAF8` |
| `--color-secondary-100` | `#F4F2ED` |
| `--color-secondary-200` | `#E8E6E1` |
| `--color-secondary-300` | `#D8D3CA` |
| `--color-secondary-400` | `#C3BCB0` |
| `--color-secondary-500` | `#AA9F91` |
| `--color-secondary-600` | `#8E8276` |
| `--color-secondary-700` | `#72685F` |
| `--color-secondary-800` | `#554C45` |
| `--color-secondary-900` | `#362F2A` |
| `--color-secondary-950` | `#211B17` |

## Tertiary / Earth Accent

| Token | Hex |
|---|---|
| `--color-tertiary-50` | `#F7F5E7` |
| `--color-tertiary-100` | `#EEEBD0` |
| `--color-tertiary-200` | `#DDD292` |
| `--color-tertiary-300` | `#CEBF67` |
| `--color-tertiary-400` | `#BEAB3B` |
| `--color-tertiary-500` | `#A6902C` |
| `--color-tertiary-600` | `#897621` |
| `--color-tertiary-700` | `#726723` |
| `--color-tertiary-800` | `#5C541D` |
| `--color-tertiary-900` | `#4C4118` |
| `--color-tertiary-950` | `#2B260D` |

## Surface / Background

| Token | Hex | 用途 |
|---|---|---|
| `--color-surface` | `#FFFFFF` | card、section、input |
| `--color-surface-soft` | `#FAFAF8` | subtle surface |
| `--color-bg` | `#F8F7F4` | body background |
| `--color-bg-muted` | `#F4F2ED` | muted block background |
| `--color-surface-400` | `#EDEBE6` | neutral surface scale |
| `--color-border` | `#E8E6E1` | card、input、divider |
| `--color-border-strong` | `#D8D3CA` | table header、focus boundary |
| `--color-surface-700` | `#BEB8AD` | neutral surface scale |
| `--color-surface-800` | `#8F877D` | neutral surface scale |
| `--color-surface-900` | `#5F5750` | dark neutral surface |
| `--color-surface-950` | `#2A2622` | deepest neutral surface |

## Text / Ink Gray

| Token | Hex | 用途 |
|---|---|---|
| `--color-text-50` | `#F7F8F7` | light text surface |
| `--color-text-100` | `#EEF0EE` | light text surface |
| `--color-text-200` | `#D5D8D5` | disabled light |
| `--color-text-300` | `#B8BDB9` | disabled |
| `--color-text-subtle` | `#8A8F88` | caption、meta |
| `--color-text-muted` | `#66706A` | secondary text |
| `--color-text-600` | `#4E5853` | muted strong |
| `--color-text-700` | `#38423D` | body strong |
| `--color-text-800` | `#2A332F` | heading support |
| `--color-text` | `#1A1F2E` | main text |
| `--color-text-950` | `#0F141D` | strongest text |

## State Colors

| Token | Hex | 用途 |
|---|---|---|
| `--color-success-soft` | `#EDF3EE` | success pale background |
| `--color-success-bg` | `#DDE9DF` | success background |
| `--color-success` | `#5F7B68` | success text、icon |
| `--color-warning-soft` | `#F7F1E5` | warning pale background |
| `--color-warning` | `#9A7B42` | warning、coupon、pending |
| `--color-danger-soft` | `#FFF0EE` | error pale background |
| `--color-danger` | `#C73E3A` | delete、error |
| `--color-info-soft` | `#EFF3F7` | info pale background |
| `--color-info` | `#64748B` | info text |
| `--color-disabled-bg` | `#E8E6E1` | disabled background |
| `--color-disabled` | `#D5D3CE` | disabled border、text |

## Semantic Tokens

| Token | Value | 用途 |
|---|---|---|
| `--color-primary` | `var(--color-primary-500)` | primary button default |
| `--color-primary-hover` | `var(--color-primary-600)` | hover |
| `--color-primary-active` | `var(--color-primary-700)` | pressed、selected、current route |
| `--color-primary-soft` | `var(--color-primary-100)` | active item、focus surface |
| `--color-primary-notice` | `#BFC9BB` | review notice、process step badge |
| `--color-text` | `#1A1F2E` | 主要文字 |
| `--color-text-muted` | `#66706A` | 次要文字、說明 |
| `--color-text-subtle` | `#8A8F88` | caption、meta |

> 注意：`--color-text`、`--color-text-muted`、`--color-text-subtle` 是已定義的語意文字 token；不可在 CSS 中再次自我引用。其餘語意 token 依表格指向對應色階，不另行發明 900 色階。
