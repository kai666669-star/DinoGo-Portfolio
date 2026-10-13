package com.dinogo.sysmsg.entity;
import org.hibernate.annotations.Nationalized;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * ============================================================
 * sysmsg.send_seller
 * ============================================================
 *
 * JOINED：
 *
 * SendEntity
 *     ↓
 * SendSellerEntity
 *
 * 用於 SC 商家 → 會員訊息。
 *
 * 也可以存放商家訊息專屬欄位。
 *
 * ============================================================

 */
@Entity
@PrimaryKeyJoinColumn(name = "send_seller_id")
@Table(
    name = "send_seller",
    schema = "sysmsg"
)
public class SendSellerEntity extends SendEntity {

    /**
     * SC 實際發送時保存 order 模組提供的訂單編號快照。
     * SAVE 範本尚未選擇訂單，因此維持 NULL。
     */
    @Nationalized
    @Column(name = "order_no", length = 30)
    private String orderNo;

    //sysmsg-start，總共1次修改，第1次//
    // 圖片本體上傳 Cloudinary，訊息資料只保存 HTTPS URL。
    @Column(
        name = "img_one", length = 500
    )
    private String imgOne;
    @Column(name = "img_one_public_id", length = 255)
    private String imgOnePublicId;

    @Column(
        name = "img_two", length = 500
    )
    private String imgTwo;
    @Column(name = "img_two_public_id", length = 255)
    private String imgTwoPublicId;


    @Column(
        name = "img_three", length = 500
    )
    private String imgThree;
    @Column(name = "img_three_public_id", length = 255)
    private String imgThreePublicId;
    //sysmsg-end，總共1次修改，第1次//
    
    @Nationalized
    @Column(
        name = "send_remark",
        length = 1000
    )
    private String sendRemark;


    // ============================================================
    // Constructor
    // ============================================================

    /**
     * Lombok： @NoArgsConstructor
     */
    protected SendSellerEntity() {
        super();
    }

    public SendSellerEntity(
        Integer msgfromSellerId,
        String msgFunction,
        String msgLabel,
        String sendTitle,
        String sendContent,
        SendStatus sendStatus,
        String orderNo,
        String imgOne,
        String imgTwo,
        String imgThree,
        String sendRemark
    ) {

        super(
            msgfromSellerId,
            msgFunction,
            msgLabel,
            sendTitle,
            sendContent,
            sendStatus
        );

        this.orderNo = orderNo;
        this.imgOne = imgOne;
        this.imgTwo = imgTwo;
        this.imgThree = imgThree;
        this.sendRemark = sendRemark;
    }


    // ============================================================
    // Getter / Setter
    // ============================================================

    /*
     * Lombok：
     *
     *     @Getter
     *     @Setter
     */

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getImgOne() {
        return imgOne;
    }

    public void setImgOne(String imgOne) {
        this.imgOne = imgOne;
    }
    public String getImgOnePublicId() { return imgOnePublicId; }
    public void setImgOnePublicId(String value) { this.imgOnePublicId = value; }

    public String getImgTwo() {
        return imgTwo;
    }

    public void setImgTwo(String imgTwo) {
        this.imgTwo = imgTwo;
    }
    public String getImgTwoPublicId() { return imgTwoPublicId; }
    public void setImgTwoPublicId(String value) { this.imgTwoPublicId = value; }

    public String getImgThree() {
        return imgThree;
    }

    public void setImgThree(String imgThree) {
        this.imgThree = imgThree;
    }
    public String getImgThreePublicId() { return imgThreePublicId; }
    public void setImgThreePublicId(String value) { this.imgThreePublicId = value; }

    public String getSendRemark() {
        return sendRemark;
    }

    public void setSendRemark(String sendRemark) {
        this.sendRemark = sendRemark;
    }
}

