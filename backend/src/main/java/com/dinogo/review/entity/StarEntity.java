package com.dinogo.review.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 完成訂單中的單一商品快照及其評論內容。
 *
 * <p>Star 在訂單 COMPLETED 時先建立；會員後續的「新增評論」與「修改評論」
 * 都是更新同一筆 Star，清除評論時也不刪除商品快照。</p>
 */
@Entity
@Table(
        name = "star",
        schema = "review",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_review_star_history_order_item",
                columnNames = {"history_id", "order_item_id"}))
@Getter
@NoArgsConstructor
public class StarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "star_id")
    private Integer id;

    // Spring Boot/JPA 維護物件關聯；SQL Server FK 保護資料完整性。
    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "history_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_review_star_history"))
    @JsonIgnore
    private HistoryEntity history;

    // 以下商品欄位由單體內 Order Provider 建立快照，避免商品日後變更影響歷史訂單。
    @Setter
    @Column(name = "order_item_id", nullable = false)
    private Integer orderItemId;

    @Setter
    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Setter
    //review-start，總共3次修改，第1次//
    // 對齊訂單模組 OrderItem.productName 的 100 字快照上限。
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Setter
    // 對齊訂單模組 OrderItem.productImageUrl 的 500 字上限。
    // 訂單商品可能沒有圖片；review 保存快照時允許 image_url 為 NULL。
    @Column(name = "image_url", nullable = true, length = 500)
    private String imageUrl;

    @Setter
    // 對齊訂單模組 OrderItem.unitPrice 的 decimal(12,2)。
    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;
    //review-end，總共3次修改，第1次//

    //review-start，總共3次修改，第2次//
    // 評論圖片只保存 Cloudinary HTTPS URL，實際圖片不寫入單體資料庫。
    @Setter
    @Column(name = "img_one", length = 500)
    private String imgOne;
    @Setter
    @Column(name = "img_one_public_id", length = 255)
    private String imgOnePublicId;

    @Setter
    @Column(name = "img_two", length = 500)
    private String imgTwo;
    @Setter
    @Column(name = "img_two_public_id", length = 255)
    private String imgTwoPublicId;

    @Setter
    @Column(name = "img_three", length = 500)
    private String imgThree;
    @Setter
    @Column(name = "img_three_public_id", length = 255)
    private String imgThreePublicId;
    //review-end，總共3次修改，第2次//

    @Setter
    @Column(name = "feedback", length = 500)
    private String feedback;

    //review-start，總共3次修改，第3次//
    /**
     * 功能：使用 Integer 保留「尚未評分」的 null，同時以 TINYINT JDBC 型別對齊 SQL Server。
     * 應用：完成訂單建立 Star 時不必先填星等，會員評論時才寫入 1～5。
     */
    @Setter
    @Min(1)
    @Max(5)
    // 明確覆寫 Integer 的預設 JDBC INTEGER，讓 ddl-auto=validate 預期 SQL Server tinyint。
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "five_star", columnDefinition = "tinyint")
    private Integer fiveStar;

    /**
     * 功能：讀取 SQL Server PERSISTED 計算欄位，不允許 Hibernate 寫入。
     * 應用：Offset 分頁依內容完整度 5→0、更新時間、starId 穩定排序。
     */
    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(
            name = "review_priority",
            insertable = false,
            updatable = false)
    private Integer reviewPriority;
    //review-end，總共3次修改，第3次//

    // 會員可能從不同裝置同時修改；JPA 樂觀鎖避免舊資料覆蓋新資料。
    // SQL Server DEFAULT 0 同時保護非 JPA INSERT。
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // INSERT 由 SQL Server DEFAULT 控制；UPDATE 由 ReviewService 明確設定。
    // insertable=false 防止 Hibernate 用 null 覆蓋資料庫預設值。
    @Setter
    @Generated(event = EventType.INSERT)
    @Column(name = "star_upd_at", nullable = false, insertable = false)
    private LocalDateTime starUpdAt;
}
