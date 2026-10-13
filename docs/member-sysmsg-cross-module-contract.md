# A → F：會員與系統通知整合規格

## 目的與範圍

本文件定義 A（會員）提供給 F（系統通知／評價／客服）的會員查詢與 Email 通知偏好 contract。

- 適用於同一個 DinoGo Spring Boot application 內的 Java 模組整合。
- F 應直接 constructor injection `MemberSysmsgProviderService`；**不要**自行建立 HTTP `MemberClient`、不要讀取 `member.*` 資料表，也不要複製會員／角色資料。
- Provider 只提供會員資料與有效狀態判斷；F 仍負責訊息、收件狀態與寄送紀錄。
- 公開會員 API 的完整規格仍以 [member-api.md](member-api.md) 為準。

## 前置條件

部署或啟動含此整合的版本前，必須先套用：

```text
database/migrations/V004__add_member_email_notification_preferences.sql
```

Migration 會在 `member.Member` 新增下列欄位：

| 欄位 | 型別 | 預設值 | 用途 |
| --- | --- | --- | --- |
| `email_order_notifications` | `bit NOT NULL` | `1` | 是否接收訂單相關 Email。 |
| `email_marketing_notifications` | `bit NOT NULL` | `0` | 是否接收行銷相關 Email。 |

## 注入方式

```java
import com.dinogo.member.service.MemberSysmsgProviderService;

@Service
public class MessageService {
    private final MemberSysmsgProviderService memberProvider;

    public MessageService(MemberSysmsgProviderService memberProvider) {
        this.memberProvider = memberProvider;
    }
}
```

## 回傳資料

所有方法回傳 `com.dinogo.member.dto.MemberSysmsgResponse`：

```java
import java.util.List;

public record MemberSysmsgResponse(
        Integer memberId,
        Integer sellerId,
        boolean authenticated,
        String email,
        String role,
        List<Integer> roleIds,
        Boolean emailOrderNotifications,
        Boolean emailMarketingNotifications) {
}
```

| 欄位 | 說明 |
| --- | --- |
| `memberId` | A 模組的會員 ID；F 的收件者／評價對象應以此 ID 關聯。 |
| `sellerId` | 該會員對應的賣家 ID；非賣家時為 `null`。 |
| `authenticated` | 僅 `getProfile` 回傳 `true`；其餘查詢為 `false`。不可把此欄當作 Spring Security 授權依據。 |
| `email` | 用於寄送 Email 的會員信箱。 |
| `role` | 目前會員的角色名稱；僅供顯示或訊息路由參考。 |
| `roleIds` | 完整角色 ID 清單：`1` 一般會員、`2` 商家會員、`3` 管理員。含 `2` 時必定也含 `1`，因商家同時是一般會員。授權或收件匣分流應以此欄位判斷，不可依固定會員／商家 ID 判斷。 |
| `emailOrderNotifications` | 訂單 Email 偏好；`true` 寄送、`false` 不寄送、`null` 視為未驗證且預設寄送。 |
| `emailMarketingNotifications` | 行銷 Email 偏好。 |

## Provider 方法

| 方法 | 使用情境 | 規則 |
| --- | --- | --- |
| `getProfile(Integer memberId)` | 目前登入會員的個人通知設定 | 呼叫端必須先從已驗證 JWT 取得 `memberId`；不可使用前端任意傳入的 ID。回傳 `authenticated = true`。 |
| `getMember(Integer memberId)` | 單一收件者查核、評價會員存在性 | 僅回傳 `ACTIVE` 會員；回傳 `authenticated = false`。 |
| `getAllMembers()` | OA／平台廣播的有效會員清單 | 僅包含 `ACTIVE` 會員；已批次載入角色與賣家對照。 |

若會員不存在或狀態非 `ACTIVE`，`getProfile` 與 `getMember` 會拋出 `IllegalArgumentException("Member not found: <memberId>")`。F 應捕捉此例外並停止建立該會員的收件紀錄；不要把它當作可寄送的會員。

## Email 收件篩選規則

F 在建立 Email 寄送工作前，必須套用下列規則：

| Email 類型 | 必要條件 |
| --- | --- |
| 訂單成立、付款、出貨、配送等訂單通知 | `emailOrderNotifications != false` |
| 優惠活動、新品、推薦商品等行銷通知 | `emailMarketingNotifications == true` |

- `emailMarketingNotifications` 預設為 `false`；只有明確為 `true` 才可寄送行銷 Email。
- 本規格只限制 Email。站內訊息、客服通知等其他通道若要套用偏好，需由 F 另訂 contract，不可自行把 Email 偏好延伸為所有通道的封鎖規則。
- `getAllMembers()` 只負責篩選有效會員，不會替 F 依 Email 類型過濾；F 必須在取得清單後依上述偏好篩選。

## 邊界與變更通知

- A 擁有 `member.Member`、通知偏好欄位與 `MemberSysmsgProviderService`；F 不可直接修改它們。
- F 擁有訊息模板、訊息內容、寄送佇列與收件狀態。
- 若 A 變更 DTO 欄位、方法簽名、會員有效狀態判斷或偏好語意，必須先通知 F 並更新本文件。
- 若 F 需要新的會員資料，先提出 contract 需求；不要直接新增 SQL 查詢或跨模組修改 A 的 Entity。
