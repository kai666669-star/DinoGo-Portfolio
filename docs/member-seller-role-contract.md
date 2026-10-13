# 商家核准的會員權限 Contract

商家申請的建立、狀態轉換與 `seller.Seller` 資料由 E 模組負責；角色關聯與 JWT 失效由 A 的 `MemberService` 負責。

## A 提供的方法

```java
void grantSellerRole(Integer memberId)
void increaseAuthVersion(Integer memberId)
```

- `grantSellerRole` 會授予小寫角色名稱 `seller`，若角色已存在則不重複新增。
- `increaseAuthVersion` 會遞增 `member.Member.auth_version`。交易提交後，該會員既有 JWT 與資料庫版本不符，必須重新登入才會取得含 `seller` 的新 token。
- 兩個方法均使用 Spring 預設的 `@Transactional` propagation（`REQUIRED`），會加入 E 的外層交易。

## E 的核准流程

E 的核准 Service 必須使用 `@Transactional`，且以已驗證的管理員身分取得 `reviewedBy`：

```text
確認 SellerApplication 為 PENDING
→ 建立 seller.Seller
→ 將申請更新為 APPROVED，寫入 reviewed_by、reviewed_at
→ memberService.grantSellerRole(memberId)
→ memberService.increaseAuthVersion(memberId)
→ commit
```

任一步驟失敗時必須回滾。駁回流程不得授予 seller 角色，也不得變更 auth version。
