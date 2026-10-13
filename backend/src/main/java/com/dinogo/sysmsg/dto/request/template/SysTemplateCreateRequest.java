package com.dinogo.sysmsg.dto.request.template;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 系統後台建立系統訊息範本。
 *
 * 允許：
 *
 * OA
 * OC
 * OS
 *
 * 不讓前端自行輸入完整 msg_function。
 *
 * Spring Boot 會：
 *
 * msg_function prefix
 *       +
 * 自動取號
 *
 * 例如：
 *
 * OA-001
 * OC-001
 * OS-001
 */
public class SysTemplateCreateRequest {

    /**
     * 只允許：
     *
     * OA
     * OC
     * OS
     */
    @NotNull
    @Pattern(regexp = "OA|OC|OS")
    private String msgType;

    @Size(max = 50)
    private String msgLabel;

    @NotBlank
    @Size(max = 100)
    private String sendTitle;

    @NotBlank
    @Size(max = 1000)
    private String sendContent;

    /*
     * Lombok：
     * @Getter
     * @Setter
     * @NoArgsConstructor
     */

    public SysTemplateCreateRequest() {
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
}
