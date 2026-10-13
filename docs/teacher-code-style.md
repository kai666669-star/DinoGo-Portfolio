# Teacher Code Style Reference

> 本文件整理自 `../teacher-reference/` 的實際老師專案，只記錄可重複使用的 pattern，不是 DinoGo 架構或資料庫設計的 Source of Truth。
>
> 使用順序：先讀本摘要；仍不足時，才依任務類型查看對應 reference。若老師範例與 DinoGo 現有 codebase 或正式文件衝突，以 DinoGo 規範為準。

## Reference Map

| 任務類型 | 優先 reference |
| --- | --- |
| Hibernate / JPA / DAO | `../teacher-reference/hibernate/proj-hibernate/` |
| Spring Boot / REST / AJAX | `../teacher-reference/spring-ajax/labboot-ajax-final/` |
| Vue / Axios / Router | `../teacher-reference/vue/labvue-front-final/` |
| Login / BCrypt / Filter / Interceptor / Security | `../teacher-reference/security/SecurityTL/`、`SecurityTL-Lab/` |

## Java / General

- 套件依職責分組。Spring 範例使用 `controller`、`domain`/`entity`、`dto`、`repository`/`dao`、`service`、`config`、`filter`、`interceptor`、`utils`；原生 Hibernate 範例則使用較扁平的 `dao`、`model`、`service`、`utils`。
- 類別與介面使用 PascalCase，常見後綴包含 `Controller`、`Service`、`Repository`、`DAO`、`DAOImpl`、`Bean`、`Response`、`Filter`、`Interceptor`、`Config`、`Utility`/`Util`。
- 方法與變數使用 camelCase；CRUD 方法偏向動詞命名，例如 `findById`、`find`、`create`、`modify`、`remove`、`save`、`deleteById`。
- Spring 元件主要採 constructor injection。欄位通常為 private；部分教材專案未加 `final`，DinoGo 可優先維持自身一致性。
- 縮排以 tab 與 4 spaces 混用，annotation spacing 也不完全一致。不要把這種格式差異當成規範；修改 DinoGo 時遵循現有 formatter，避免全域重排。
- 註解多用中文解釋教學步驟、SQL 對應或安全觀念；正式程式應保留能說明「為何」的註解，不需逐行翻譯程式行為。
- 教材中可見 `printStackTrace()`、`System.out`、手動 JSON 解析及示範帳密。這些是課堂示範，不應直接視為 DinoGo 的 production pattern。

## Hibernate

主要來源：`teacher-reference/hibernate/proj-hibernate/`

- DAO 以介面定義查詢與 CRUD contract，再由 `*DAOHibernate` 實作。
- DAO implementation 透過 constructor 接收 `SessionFactory`，集中由 `getSession()` 取得 current session。
- Entity/Bean 使用 `@Entity`、`@Table`、`@Id`、`@Column` 明確對應資料表與欄位，並提供無參數建構子、getter/setter、`toString()`。
- CRUD 使用 `Session.find`、`persist`、`merge`、`remove`；操作前會先檢查輸入與資料是否存在。
- 動態查詢使用 JPA Criteria API：`CriteriaBuilder`、`CriteriaQuery`、`Predicate`、排序與 `setFirstResult`/`setMaxResults` 分頁。
- Service 依賴 DAO，負責輸入解析、存在性檢查與流程組合，不直接建立 Session。
- 原生 Hibernate 測試由呼叫端取得 Session、`beginTransaction()`，最後 `commit()` 並關閉資源。Spring 版本則在 service 使用 `@Transactional` 管理交易。
- `HibernateUtil` 集中建立與關閉 `SessionFactory`。其中連線字串與示範帳密僅屬老師本機教材，不可複製進 DinoGo 或版本控制。

## Spring Boot / REST / AJAX

主要來源：`teacher-reference/spring-ajax/labboot-ajax-final/`

- 採 `Controller -> Service -> Repository` 分層；Controller 與 Service 以 constructor injection 取得依賴。
- REST Controller 使用 `@RestController`、類別層級 `@RequestMapping`，並以 `@GetMapping`、`@PostMapping`、`@PutMapping`、`@DeleteMapping` 對應 CRUD。
- 路徑參數使用 `@PathVariable`；JSON request 在此教材中常以 `@RequestBody String` 搭配 `JSONObject` 手動解析。DinoGo 若已有 typed request DTO，應優先沿用，不必模仿字串解析。
- 簡單資料存取使用 `JpaRepository`；複雜動態篩選則讓 repository 同時擴充自訂 DAO 介面，由 `*DAOImpl` 使用 `EntityManager` 與 Criteria API 實作。
- Service 使用 `@Service` 與 `@Transactional`，集中處理查詢條件轉換、CRUD、存在性判斷與資料更新。
- API 回應可使用 record DTO，例如以 `success`、`message`、`count`、`list` 組成一致 envelope；登入範例則直接組 JSON 字串。未觀察到全域統一的 response wrapper。
- 找不到資料時，列表回應傾向回傳空陣列而非 `null`；操作結果以 success flag 與中文 message 表達。
- 未觀察到 `@ControllerAdvice` 或 `@ExceptionHandler` 的集中例外處理；多數錯誤在 controller/service 內判斷或捕捉。因此不得宣稱老師已建立全域 exception-handling style。
- AJAX/JSON 串接以同源 REST endpoint 為主，搭配 CORS 設定與前端 Axios。

## Vue

主要來源：`teacher-reference/vue/labvue-front-final/`

- 實際使用 Vue 3、Vite 與 Composition API，單檔元件採 `<script setup>`。
- 頁面放在 `src/views/`，可重用 UI 放在 `src/components/`；安全性頁面另放 `views/secure/`，一般功能頁放 `views/pages/`。
- reactive state 主要使用 `ref`，初始化資料使用 `onMounted`；props 用 `defineProps`，父子元件以 props、`v-model` 與自訂 event 溝通。
- Router 使用獨立 `src/router/router.js`，以 `createRouter`、`createWebHistory` 和具名 routes 設定，並包含 403 與 catch-all 404 頁面。
- Axios 封裝在 `src/plugins/axios.js`，`baseURL` 由 `import.meta.env.VITE_API_URL` 取得；response interceptor 統一處理 403 導向。
- API 呼叫集中在 view 的具名函式中，混合使用 `async/await` 與 Promise chain；CRUD 對應 POST/GET/PUT/DELETE，錯誤以 `try/catch` 或 `.catch()` 處理。
- Store 使用 Pinia setup store，state 以 `ref` 定義，action 以函式更新；登入 email/token 透過 persisted-state 存在 `sessionStorage`。
- 畫面使用 Bootstrap class，互動提示使用 SweetAlert2；列表頁示範搜尋、分頁、modal CRUD 與 API 串接。
- 教材會在個別 request 手動加入 Bearer token。若 DinoGo 已有 request interceptor，應沿用既有集中式做法。

## Security

主要來源：`teacher-reference/security/SecurityTL/`、`SecurityTL-Lab/`；JWT 補充來源為 `spring-ajax/labboot-ajax-final/`。

- Login 範例包含 session-based authentication：controller 驗證帳密後寫入 session，`HandlerInterceptor` 檢查登入狀態，未登入時導回登入頁。
- `LoginInterceptor` 實作 `HandlerInterceptor.preHandle`，保存原 request target，檢查 session attribute；由 `WebMvcConfigurer.addInterceptors` 註冊並設定 include/exclude path。
- `NoCacheFilter` 類型的 servlet filter 用於 response header 等橫切關注點；Filter、Interceptor 與 MVC config 分放獨立 package。
- BCrypt 確實存在於 `SecurityTL` 與 `SecurityTL-Lab`：以 `BCrypt.gensalt(cost)`、`BCrypt.hashpw` 產生雜湊，登入驗證用 `BCrypt.checkpw`。教材說明 production cost 需在安全性與效能間取捨，不應硬搬示範迴圈或明文測試密碼。
- `SecurityTL` 使用 Spring Security Crypto，但未觀察到完整的 Spring Security filter-chain authentication/authorization 架構；主要仍是 MVC interceptor、session 與安全主題示範。
- JWT 確實出現在 Spring AJAX reference，而非兩個 SecurityTL 專案：utility 負責建立/驗證 token，`OncePerRequestFilter` 從 `Authorization: Bearer ...` 取出 token 並驗證，前端登入後保存 token 並在受保護 API 帶入。
- 未觀察到以 Spring Security `SecurityFilterChain`、`Authentication`、`GrantedAuthority` 建立的完整 role-based authorization；不可把一般網路範例補成老師 style。
- 老師 reference 內含本機示範 credentials、key/certificate 與加解密素材；僅能在 repo 外唯讀參考，不得複製到 DinoGo 文件、設定或 Git。

## 使用原則

1. 一般任務先讀本文件，不重新掃描所有 teacher reference。
2. 本文件不足時，只打開 Reference Map 中與任務直接相關的專案。
3. 只萃取結構、命名、分層與可重用實作方式，不複製完整 class 或教材 business logic。
4. DB/Entity mapping 先遵守 `database-schema.md`；分工與時程遵守 `team-plan.md`；Git 操作遵守 `git-workflow.md`。
5. 安全性實作須符合 DinoGo 正式要求；教材中的示範帳密、金鑰、舊式做法與不完整 security stack 不能直接進正式程式。
