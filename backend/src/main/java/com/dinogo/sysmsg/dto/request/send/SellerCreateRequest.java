package com.dinogo.sysmsg.dto.request.send;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 商家直接建立 SC 實際訊息。
 *
 * 不建立 SAVE。
 *
 * 直接：
 *
 * SendEntity
 *     +
 * SendSellerEntity
 *
 * send_status = SEND
 *
 * 必須驗證 Order。
 */
public class SellerCreateRequest {

    /**
     * 商家選擇的訂單。
     */
    @NotNull
    private Integer orderId;

    @NotBlank
    @Size(max = 100)
    private String sendTitle;

    @NotBlank
    @Size(max = 1000)
    private String sendContent;

    @Size(max = 1000)
    private String sendRemark;

    //sysmsg-start，總共1次修改，第1次//
    @Size(max = 500) private String imgOne;
    @Size(max = 255) private String imgOnePublicId;
    @Size(max = 500) private String imgTwo;
    @Size(max = 255) private String imgTwoPublicId;
    @Size(max = 500) private String imgThree;
    @Size(max = 255) private String imgThreePublicId;
    //sysmsg-end，總共1次修改，第1次//

    public SellerCreateRequest() {
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getSendTitle() {
        return sendTitle;
    }

    public void setSendTitle(String sendTitle) {
        this.sendTitle = sendTitle;
    }

    public String getSendContent() {
        return sendContent;
    }

    public void setSendContent(String sendContent) {
        this.sendContent = sendContent;
    }

    public String getSendRemark() {
        return sendRemark;
    }

    public void setSendRemark(String sendRemark) {
        this.sendRemark = sendRemark;
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
}

