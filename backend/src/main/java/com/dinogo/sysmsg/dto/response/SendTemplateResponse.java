package com.dinogo.sysmsg.dto.response;

import com.dinogo.sysmsg.entity.SendStatus;

import java.time.LocalDateTime;

/**
 * 範本資料。
 *
 * SAVE 範本專用。
 */
public class SendTemplateResponse {

    private Integer sendId;

    private Integer msgfromSellerId;

    private String msgFunction;

    private String msgLabel;

    private String sendTitle;

    private String sendContent;

    private String sendRemark;

    private String imgOne;
    private String imgOnePublicId;
    private String imgTwo;
    private String imgTwoPublicId;
    private String imgThree;
    private String imgThreePublicId;

    private LocalDateTime sendUpdAt;

    private SendStatus sendStatus;

    public SendTemplateResponse() {
    }

    public Integer getSendId() {
        return sendId;
    }

    public void setSendId(Integer sendId) {
        this.sendId = sendId;
    }

    public Integer getMsgfromSellerId() {
        return msgfromSellerId;
    }

    public void setMsgfromSellerId(Integer msgfromSellerId) {
        this.msgfromSellerId = msgfromSellerId;
    }

    public String getMsgFunction() {
        return msgFunction;
    }

    public void setMsgFunction(String msgFunction) {
        this.msgFunction = msgFunction;
    }

    public String getMsgLabel() {
        return msgLabel;
    }

    public void setMsgLabel(String msgLabel) {
        this.msgLabel = msgLabel;
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

    public String getSendRemark() { return sendRemark; }
    public void setSendRemark(String sendRemark) { this.sendRemark = sendRemark; }
    public String getImgOne() { return imgOne; }
    public void setImgOne(String imgOne) { this.imgOne = imgOne; }
    public String getImgOnePublicId() { return imgOnePublicId; }
    public void setImgOnePublicId(String imgOnePublicId) { this.imgOnePublicId = imgOnePublicId; }
    public String getImgTwo() { return imgTwo; }
    public void setImgTwo(String imgTwo) { this.imgTwo = imgTwo; }
    public String getImgTwoPublicId() { return imgTwoPublicId; }
    public void setImgTwoPublicId(String imgTwoPublicId) { this.imgTwoPublicId = imgTwoPublicId; }
    public String getImgThree() { return imgThree; }
    public void setImgThree(String imgThree) { this.imgThree = imgThree; }
    public String getImgThreePublicId() { return imgThreePublicId; }
    public void setImgThreePublicId(String imgThreePublicId) { this.imgThreePublicId = imgThreePublicId; }

    public LocalDateTime getSendUpdAt() {
        return sendUpdAt;
    }

    public void setSendUpdAt(LocalDateTime sendUpdAt) {
        this.sendUpdAt = sendUpdAt;
    }

    public SendStatus getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(SendStatus sendStatus) {
        this.sendStatus = sendStatus;
    }
}
