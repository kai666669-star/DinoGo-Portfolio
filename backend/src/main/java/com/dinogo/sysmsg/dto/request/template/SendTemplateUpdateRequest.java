package com.dinogo.sysmsg.dto.request.template;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改 SAVE 範本。
 *
 * send_id 從 URL / PathVariable 取得，
 * 不需要重複放在 Request Body。
 *
 * 例如：
 *
 * PUT /api/sysmsg/templates/{sendId}
 */
public class SendTemplateUpdateRequest {

    @Size(max = 2)
    private String msgType;

    @Size(max = 50)
    private String msgLabel;

    @NotBlank
    @Size(max = 100)
    private String sendTitle;

    @NotBlank
    @Size(max = 1000)
    private String sendContent;

    @Size(max = 1000)
    private String sendRemark;

    //sysmsg-start，總共1次修改，第1次//
    @Size(max = 500)
    private String imgOne;
    @Size(max = 255)
    private String imgOnePublicId;

    @Size(max = 500)
    private String imgTwo;
    @Size(max = 255)
    private String imgTwoPublicId;

    @Size(max = 500)
    private String imgThree;
    @Size(max = 255)
    private String imgThreePublicId;
    //sysmsg-end，總共1次修改，第1次//

    /*
     * Lombok：
     * @Getter
     * @Setter
     * @NoArgsConstructor
     */

    public SendTemplateUpdateRequest() {
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
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

