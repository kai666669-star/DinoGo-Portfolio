# Router and Branch Plan

## Layout Route Strategy

使用 layout route 組合共用 shell，避免每個 view 重複 Header/Footer。以下是規劃，這一階段只建立文件，不直接修改 `frontend/src/router/index.js`。

### DefaultStorefrontLayout

組合 `UtilityBar`、`AppHeader`、`PrimaryNav`、`RouterView`、`AppFooter`；適用 `/`、`/products`、`/products/:id`、`/cart`、`/checkout`。

### AuthLayout

組合簡化 Header、`RouterView`、簡化 Footer；適用 `/login`、`/register`、`/forgot-password`。

### MemberLayout

組合 `UtilityBar`、`AppHeader`、`PrimaryNav`、`MemberNav`、`RouterView`、`AppFooter`；適用 `/member/*`。

### SellerLayout

組合 `SellerNav` 與 `RouterView`；可依設計稿決定是否加入 AppHeader，但不得混用 `MemberNav`；適用 `/seller/*`。

## Suggested Route Shape

```js
{
  path: '/member',
  component: MemberLayout,
  children: [
    { path: '', redirect: '/member/overview' },
    { path: 'overview', component: () => import('@/views/member/MemberOverviewView.vue') },
  ],
}
```

完整頁面與 route 應在各模組實作時補上；不可在本階段引用尚未存在的 view。

## Branch Delivery Order

1. `feature/design-system`：本文件、其他 design docs、reference 圖片、tokens、`main.js` import。
2. `feature/layout-shell`：UtilityBar、AppHeader、SearchBar、PrimaryNav、AppFooter、DefaultStorefrontLayout、AuthLayout。
3. `feature/member-layout`：MemberLayout、MemberNav、member route shell。
4. `feature/seller-layout`：SellerLayout、SellerNav、seller route shell。
5. 各模組頁面分支：從已整合的 `develop` 再開立。

## Branch Rules

- 頁面分支不可重寫全域 Header/Footer。
- 商品頁不可自行建立另一份 AppHeader。
- Seller 頁不可混用 MemberNav。
- Layout shell 合併後，頁面分支再從更新後的 `develop` 開發。
