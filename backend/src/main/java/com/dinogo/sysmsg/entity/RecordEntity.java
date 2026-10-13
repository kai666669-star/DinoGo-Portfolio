package com.dinogo.sysmsg.entity;
import org.hibernate.annotations.Nationalized;
import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * ============================================================
 * sysmsg.record
 * ============================================================
 *
 * 【Spring Boot 控制】
 *
 * 1. 建立 Record 時透過 send_id 取得 msg_function
 * 2. 建立 Record 時確認 SendEntity 是否存在
 * 3. 建立 Record 時取得 msgfrom_seller_id
 * 4. 控制會員 / 商家收件人
 * 5. 控制一筆 Record 只能有一個收件人類型
 * 6. 新增 Record 時初始化 record_status = UNREAD
 * 7. 控制 READ / UNREAD 合法狀態轉換
 * 8. 控制 Record DELETE
 * 9. 控制 OA / OC / OS Record 刪除時硬刪除
 * 10. 控制其他 Record 軟刪除
 * 11. validateRecordOwner()
 *
 *
 * 【SQL Server 控制】
 *
 * 1. record_id IDENTITY
 * 2. INSERT 時 record_created_at DEFAULT SYSDATETIME()
 * 3. record_status DEFAULT 'UNREAD'
 * 4. record_status CHECK
 * 5. msg_function CHECK
 * 6. send_id FK
 * 7. 會員收件匣 INDEX
 * 8. 商家收件匣 INDEX
 * 9. 發送紀錄 INDEX
 * 10. msgto_member_id / msgto_seller_id 二選一 CHECK
 *
 *
 * 【責任分工】
 *
 * record_status：
 *     Spring Boot：
 *         INSERT 時主動設定 UNREAD。
 *         UPDATE 時控制 UNREAD → READ / DELETE。
 *
 *     SQL Server：
 *         DEFAULT 'UNREAD' 作為資料庫保護。
 *         CHECK 保證只能是 UNREAD / READ / DELETE。
 *
 *
 * record_created_at：
 *     Spring Boot：
 *         不產生、不修改。
 *
 *     SQL Server：
 *         INSERT 時使用 DEFAULT SYSDATETIME()。
 *
 *
 * 【重要】
 *
 * record_created_at 使用：
 *
 *     @Generated(event = EventType.INSERT)
 *
 * 告知 Hibernate：
 * 此欄位在 INSERT 時由資料庫產生。
 *
 * 因此 Hibernate 不應由 Java 程式主動產生建立時間。
 *
 * ============================================================
 */
@Entity
@Table(
    name = "record",
    schema = "sysmsg"
)
public class RecordEntity {


    // ============================================================
    // Primary Key
    // ============================================================

    /**
     * ------------------------------------------------------------
     * record_id
     * ------------------------------------------------------------
     *
     * SQL Server：
     *
     *     IDENTITY(1,1)
     *
     * Spring Boot / JPA：
     *
     *     GenerationType.IDENTITY
     *
     * 不由 Java 自行產生。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
        name = "record_id",
        nullable = false
    )
    private Integer recordId;


    // ============================================================
    // Send FK
    // ============================================================

    /**
     * ------------------------------------------------------------
     * send_id
     * ------------------------------------------------------------
     *
     * DB：
     *
     *     sysmsg.record.send_id
     *              ↓
     *     sysmsg.send.send_id
     *
     *
     * JPA：
     *
     *     RecordEntity
     *          ↓
     *     SendEntity
     *
     *
     * 使用 LAZY：
     *
     * 查收件匣列表時，
     * 不一定需要立即取得完整 SendEntity。
     *
     *
     * 注意：
     *
     * 不使用 CascadeType.ALL。
     *
     * Record 被刪除時，
     * 不應該連帶刪除 sysmsg.send。
     *
     * 符合規則：
     *
     *     刪除收件匣訊息
     *     不影響 sysmsg.send 母訊息。
     */
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    )
    @JoinColumns({
        @JoinColumn(
            name = "send_id",
            referencedColumnName = "send_id",
            nullable = false
        ),
        @JoinColumn(
            name = "msg_function",
            referencedColumnName = "msg_function",
            nullable = false
        )
    })
    private SendEntity send;


    // ============================================================
    // Message Information
    // ============================================================

    /**
     * ------------------------------------------------------------
     * msg_function
     * ------------------------------------------------------------
     *
     * 例如：
     *
     *     OA-001
     *     OC-001
     *     OS-001
     *     AC-001
     *     AS-001
     *     SC-001
     *
     *
     * Record 建立時：
     *
     *     send_id
     *          ↓
     *     SendEntity
     *          ↓
     *     send.getMsgFunction()
     *          ↓
     *     RecordEntity.msgFunction
     *
     *
     * 【Spring Boot 控制】
     *
     * 不接受前端自行決定 Record 的 msg_function。
     *
     * RecordService.createRecords()
     * 應以 SendEntity 的 msgFunction 為唯一來源。
     *
     *
     * 【SQL Server 控制】
     *
     * CHECK：
     *
     *     OA-001 ~
     *     OC-001 ~
     *     OS-001 ~
     *     AC-001 ~
     *     AS-001 ~
     *     SC-001 ~
     *
     * 且不能為 xxx-000。
     */
    @Column(
        name = "msg_function",
        length = 6,
        nullable = false,
        insertable = false,
        updatable = false
    )
    private String msgFunction;


    /**
     * ------------------------------------------------------------
     * msgfrom_seller_id
     * ------------------------------------------------------------
     *
     * 訊息發送者 seller_id。
     *
     *
     * 來源：
     *
     *     SendEntity.msgfromSellerId
     *
     *
     * 建立 Record 時：
     *
     *     send.getMsgfromSellerId()
     *              ↓
     *     RecordEntity.msgfromSellerId
     *
     *
     * 不建議前端傳入。
     *
     *
     * 例如：
     *
     * 系統後台：
     *
     *     seller_id = 1
     *
     * SC 商家訊息：
     *
     *     使用登入商家的 seller_id。
     */
    @Column(
        name = "msgfrom_seller_id",
        nullable = false,
        updatable = false
    )
    private Integer msgfromSellerId;


    // ============================================================
    // Receiver
    // ============================================================

    /**
     * ------------------------------------------------------------
     * msgto_member_id
     * ------------------------------------------------------------
     *
     * 會員收件人。
     *
     * 與 msgto_seller_id 必須二選一。
     *
     *
     * 合法：
     *
     *     msgto_member_id = 5
     *     msgto_seller_id = NULL
     *
     *
     * 不合法：
     *
     *     兩個都有值
     *
     * 或：
     *
     *     兩個都是 NULL
     */
    @Column(
        name = "msgto_member_id"
    )
    private Integer msgtoMemberId;


    /**
     * ------------------------------------------------------------
     * msgto_seller_id
     * ------------------------------------------------------------
     *
     * 商家收件人。
     *
     * 與 msgto_member_id 必須二選一。
     *
     *
     * 合法：
     *
     *     msgto_member_id = NULL
     *     msgto_seller_id = 5
     */
    @Column(
        name = "msgto_seller_id"
    )
    private Integer msgtoSellerId;

    /** 訂單通知的冪等快照；一般訊息兩欄皆為 null。 */
    @Column(name = "order_id")
    private Integer orderId;

    @Nationalized
    @Column(name = "order_status", length = 30)
    private String orderStatus;

    /** SQL Server PERSISTED 計算欄位，僅供查詢與回傳使用。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "member_inbox", insertable = false, updatable = false)
    private MemberInbox memberInbox;

    /** SQL Server PERSISTED 計算欄位，僅供查詢與回傳使用。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "seller_inbox", insertable = false, updatable = false)
    private SellerInbox sellerInbox;


    // ============================================================
    // Record Status
    // ============================================================

    /**
     * ------------------------------------------------------------
     * record_status
     * ------------------------------------------------------------
     *
     * Java Enum：
     *
     *     UNREAD
     *     READ
     *     DELETE
     *
     *
     * 【Spring Boot 主控】
     *
     * INSERT：
     *
     *     recordStatus = UNREAD
     *
     * 由 @PrePersist 自動設定。
     *
     *
     * UPDATE：
     *
     * RecordService 控制合法狀態：
     *
     *     UNREAD → READ
     *
     *     UNREAD → DELETE
     *
     *     READ → DELETE
     *
     *
     * 不應允許：
     *
     *     DELETE → UNREAD
     *     DELETE → READ
     *
     *
     * 【SQL Server 保護】
     *
     * DEFAULT：
     *
     *     'UNREAD'
     *
     * CHECK：
     *
     *     UNREAD
     *     READ
     *     DELETE
     *
     *
     * SQL Server DEFAULT 主要作為：
     *
     *     非 JPA INSERT
     *     DB 直接 INSERT
     *
     * 時的最後保護。
     */
    @Nationalized
    @Enumerated(EnumType.STRING)
    @Column(
        name = "record_status",
        length = 10,
        nullable = false
    )
    private RecordStatus recordStatus;


    // ============================================================
    // Created Time
    // ============================================================

    /**
     * ------------------------------------------------------------
     * record_created_at
     * ------------------------------------------------------------
     *
     * Record 建立時間。
     *
     *
     * 【SQL Server 控制 INSERT】
     *
     * DB：
     *
     *     DEFAULT SYSDATETIME()
     *
     *
     * Spring Boot 不使用：
     *
     *     LocalDateTime.now()
     *
     * 來產生此欄位。
     *
     *
     * @Generated(event = EventType.INSERT)
     *
     * 告訴 Hibernate：
     *
     *     此欄位的 INSERT 值由資料庫產生。
     *
     *
     * updatable = false：
     *
     *     建立後永遠不 UPDATE。
     *
     *
     * 與 SendEntity.sendUpdAt 不同：
     *
     * send_upd_at：
     *
     *     INSERT → SQL Server
     *     UPDATE → Spring Boot
     *
     * record_created_at：
     *
     *     INSERT → SQL Server
     *     UPDATE → 永不修改
     */
    @Generated(event = EventType.INSERT)
    @Column(
        name = "record_created_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime recordCreatedAt;


    // ============================================================
    // Constructor
    // ============================================================

    /**
     * JPA 必須保留無參數 Constructor。
     *
     * protected：
     *
     *     JPA 可以使用，
     *     一般程式不鼓勵直接建立空 Entity。
     *
     *
     * Lombok：
     *
     *     @NoArgsConstructor(access = AccessLevel.PROTECTED)
     */
    protected RecordEntity() {
    }


    /**
     * ------------------------------------------------------------
     * 建立 Record 使用 Constructor
     * ------------------------------------------------------------
     *
     * 呼叫端只提供：
     *
     *     SendEntity
     *     member 收件人
     *     seller 收件人
     *
     *
     * 不提供：
     *
     *     recordId
     *     msgFunction
     *     msgfromSellerId
     *     recordStatus
     *     recordCreatedAt
     *
     *
     * 原因：
     *
     * recordId：
     *     SQL Server IDENTITY
     *
     * msgFunction：
     *     SendEntity 提供
     *
     * msgfromSellerId：
     *     SendEntity 提供
     *
     * recordStatus：
     *     @PrePersist → UNREAD
     *
     * recordCreatedAt：
     *     SQL Server DEFAULT SYSDATETIME()
     */
    public RecordEntity(
        SendEntity send,
        Integer msgtoMemberId,
        Integer msgtoSellerId
    ) {

        this.send = send;

        /*
         * 先同步 SendEntity 資料。
         *
         * @PrePersist 還會再做一次，
         * 確保 persist 當下資料一致。
         */
        if (send != null) {

            this.msgFunction =
                send.getMsgFunction();

            this.msgfromSellerId =
                send.getMsgfromSellerId();
        }

        this.msgtoMemberId =
            msgtoMemberId;

        this.msgtoSellerId =
            msgtoSellerId;
    }


    // ============================================================
    // Entity Lifecycle
    // ============================================================

    /**
     * ------------------------------------------------------------
     * INSERT 前處理
     * ------------------------------------------------------------
     *
     * Spring Boot 先做必要業務檢查。
     *
     * SQL Server CHECK / FK
     * 仍然作為最後 DB 保護層。
     */
    @PrePersist
    protected void prePersist() {


        // ========================================================
        // 1. SendEntity 必須存在
        // ========================================================

        if (this.send == null) {

            throw new IllegalStateException(
                "Record 必須關聯 SendEntity"
            );
        }

        // Record 只能代表已發送的實際訊息；SAVE 是範本，不能進入收件匣。
        if (this.send.getSendStatus() != SendStatus.SEND) {
            throw new IllegalStateException(
                "Record 只能指向 send_status=SEND，不能指向 SAVE 或 DELETE"
            );
        }


        // ========================================================
        // 2. msg_function 強制從 SendEntity 取得
        // ========================================================

        this.msgFunction =
            this.send.getMsgFunction();


        if (
            this.msgFunction == null
            || this.msgFunction.trim().isEmpty()
        ) {

            throw new IllegalStateException(
                "Record 對應的 SendEntity 必須具有 msgFunction"
            );
        }


        // ========================================================
        // 3. msgfrom_seller_id 強制從 SendEntity 取得
        // ========================================================

        this.msgfromSellerId =
            this.send.getMsgfromSellerId();


        if (this.msgfromSellerId == null) {

            throw new IllegalStateException(
                "Record 對應的 SendEntity 必須具有 msgfromSellerId"
            );
        }


        // ========================================================
        // 4. 收件人必須二選一
        // ========================================================

        boolean hasMember =
            this.msgtoMemberId != null;

        boolean hasSeller =
            this.msgtoSellerId != null;


        /*
         * XOR 規則：
         *
         * member | seller | 結果
         * -----------------------
         * false  | false  | 錯
         * true   | false  | 對
         * false  | true   | 對
         * true   | true   | 錯
         *
         *
         * hasMember == hasSeller
         *
         * 表示：
         *     同時 true
         * 或
         *     同時 false
         * 都是不合法。
         */
        if (hasMember == hasSeller) {

            throw new IllegalStateException(
                "Record 收件人必須且只能設定 member_id 或 seller_id 其中一個"
            );
        }

        if ((this.orderId == null) != (this.orderStatus == null)) {
            throw new IllegalStateException(
                "訂單通知的 orderId 與 orderStatus 必須同時有值或同時為空"
            );
        }


        // ========================================================
        // 5. 新 Record 一律初始化 UNREAD
        // ========================================================

        /*
         * 【Spring Boot 主控】
         * 新建立的 Record 一律是 UNREAD。
         *
         * SQL Server 同時保留：
         *     DEFAULT 'UNREAD'
         * 作為 DB 最後保護。
         */
        this.recordStatus = RecordStatus.UNREAD;
        

    }


    // ============================================================
    // Business Helper
    // ============================================================

    /**
     * ------------------------------------------------------------
     * 是否為會員收件匣
     * ------------------------------------------------------------
     */
    public boolean isMemberInbox() {

        return this.msgtoMemberId != null
            && this.msgtoSellerId == null;
    }


    /**
     * ------------------------------------------------------------
     * 是否為商家收件匣
     * ------------------------------------------------------------
     */
    public boolean isSellerInbox() {

        return this.msgtoMemberId == null
            && this.msgtoSellerId != null;
    }


    /**
     * ------------------------------------------------------------
     * 是否未讀
     * ------------------------------------------------------------
     */
    public boolean isUnread() {

        return this.recordStatus ==
            RecordStatus.UNREAD;
    }


    /**
     * ------------------------------------------------------------
     * 是否已讀
     * ------------------------------------------------------------
     */
    public boolean isRead() {

        return this.recordStatus ==
            RecordStatus.READ;
    }


    /**
     * ------------------------------------------------------------
     * 是否已軟刪除
     * ------------------------------------------------------------
     */
    public boolean isDeleted() {

        return this.recordStatus ==
            RecordStatus.DELETE;
    }


    // ============================================================
    // Getter / Setter
    // ============================================================

    /*
     *
     * 如果之後使用 Lombok：
     *     @Getter
     *     @Setter
     *
     *
     *
     * 但實務上：
     *
     * msgFunction
     * msgfromSellerId
     * recordCreatedAt
     *
     * 建議不要讓 Controller 任意修改。
     * 真正的業務限制仍應由 Service 控制。
     */


    public Integer getRecordId() {

        return recordId;
    }


    public void setRecordId(
        Integer recordId
    ) {

        this.recordId = recordId;
    }


    public SendEntity getSend() {

        return send;
    }


    /**
     * ------------------------------------------------------------
     * setSend
     * ------------------------------------------------------------
     *
     * 一旦設定新的 SendEntity，
     * 同步更新：
     *
     *     msgFunction
     *     msgfromSellerId
     *
     *
     * 注意：
     *
     * 已存在的 Record
     * 原則上不應任意更換 send。
     *
     * 真正限制應由 RecordService 控制。
     */
    public void setSend(
        SendEntity send
    ) {

        this.send = send;

        if (send != null) {

            this.msgFunction =
                send.getMsgFunction();

            this.msgfromSellerId =
                send.getMsgfromSellerId();
        }
    }


    public String getMsgFunction() {

        return msgFunction;
    }


    /**
     * 保留 setter 提供 JPA / mapping 彈性。
     *
     * 但建立 Record 時：
     *
     *     msgFunction
     *
     * 必須以 SendEntity 為準。
     *
     * Controller 不應直接接受此欄位。
     */
    public void setMsgFunction(
        String msgFunction
    ) {

        this.msgFunction = msgFunction;
    }


    public Integer getMsgfromSellerId() {

        return msgfromSellerId;
    }


    /**
     * 保留 setter。
     *
     * 真正建立 Record 時應由：
     *
     *     SendEntity.msgfromSellerId
     *
     * 自動帶入。
     */
    public void setMsgfromSellerId(
        Integer msgfromSellerId
    ) {

        this.msgfromSellerId =
            msgfromSellerId;
    }


    public Integer getMsgtoMemberId() {

        return msgtoMemberId;
    }


    public void setMsgtoMemberId(
        Integer msgtoMemberId
    ) {

        this.msgtoMemberId =
            msgtoMemberId;
    }


    public Integer getMsgtoSellerId() {

        return msgtoSellerId;
    }


    public void setMsgtoSellerId(
        Integer msgtoSellerId
    ) {

        this.msgtoSellerId =
            msgtoSellerId;
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public MemberInbox getMemberInbox() {
        return memberInbox;
    }

    public SellerInbox getSellerInbox() {
        return sellerInbox;
    }


    public RecordStatus getRecordStatus() {

        return recordStatus;
    }


    /**
     * ------------------------------------------------------------
     * recordStatus setter
     * ------------------------------------------------------------
     *
     * Entity 本身保留 setter。
     *
     * 但是實際系統中：
     *
     * Controller 不應直接自由指定狀態。
     *
     *
     * 應由 RecordService 控制：
     *
     *     UNREAD → READ
     *     UNREAD → DELETE
     *     READ   → DELETE
     *
     *
     * DELETE 不允許恢復。
     */
    public void setRecordStatus(
        RecordStatus recordStatus
    ) {

        this.recordStatus =
            recordStatus;
    }


    public LocalDateTime getRecordCreatedAt() {

        return recordCreatedAt;
    }


    /**
     * ------------------------------------------------------------
     * setRecordCreatedAt
     * ------------------------------------------------------------
     *
     * 一般業務程式不應呼叫。
     *
     * 此欄位 INSERT 時由：
     *
     *     SQL Server
     *     DEFAULT SYSDATETIME()
     *
     * 產生。
     *
     *
     * 保留 setter：
     *
     *     JPA
     *     DTO mapping
     *     測試
     *
     * 使用彈性。
     */
    public void setRecordCreatedAt(
        LocalDateTime recordCreatedAt
    ) {

        this.recordCreatedAt =
            recordCreatedAt;
    }
}
