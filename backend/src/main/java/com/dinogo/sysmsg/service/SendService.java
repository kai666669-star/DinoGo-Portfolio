package com.dinogo.sysmsg.service;


import java.util.List;

import com.dinogo.sysmsg.dto.request.record.SendStatusUpdateRequest;
import com.dinogo.sysmsg.dto.request.send.SellerCreateRequest;
import com.dinogo.sysmsg.dto.request.send.SysCreateRequest;
import com.dinogo.sysmsg.dto.request.template.SellerTemplateApplyRequest;
import com.dinogo.sysmsg.dto.request.template.SellerTemplateCreateRequest;
import com.dinogo.sysmsg.dto.request.template.SendTemplateUpdateRequest;
import com.dinogo.sysmsg.dto.request.template.SysTemplateApplyRequest;
import com.dinogo.sysmsg.dto.request.template.SysTemplateCreateRequest;
import com.dinogo.sysmsg.dto.response.SendResponse;
import com.dinogo.sysmsg.dto.response.SendTemplateResponse;
import com.dinogo.sysmsg.dto.response.OffsetPageResponse;

/**
 * 相容用聚合介面。新 Controller 應直接依賴職責較小的介面。
 */
public interface SendService extends TemplateService, ManualMessageService, SendStatusService {

    /**
     * 商家建立 SC SAVE 範本。
     */
    SendTemplateResponse createSellerTemplate(
            SellerTemplateCreateRequest request,
            Integer loginMemberId
    );

    /**
     * 系統後台建立 OA / OC / OS SAVE 範本。
     */
    SendTemplateResponse createSystemTemplate(
            SysTemplateCreateRequest request,
            Integer loginMemberId
    );

    /**
     * 修改 SAVE 範本。
     */
    SendTemplateResponse updateTemplate(
            Integer sendId,
            SendTemplateUpdateRequest request,
            Integer loginMemberId
    );

    /**
     * 系統範本套用。
     */
    SendResponse applySystemTemplate(
            SysTemplateApplyRequest request,
            Integer loginMemberId
    );

    /**
     * 商家 SC 範本套用。
     */
    SendResponse applySellerTemplate(
            SellerTemplateApplyRequest request,
            Integer loginMemberId
    );

    /**
     * 商家直接建立 SC SEND。
     */
    SendResponse createSellerSend(
            SellerCreateRequest request,
            Integer loginMemberId
    );

    /**
     * 系統直接建立 OA / OC / OS SEND。
     */
    SendResponse createSystemSend(
            SysCreateRequest request,
            Integer loginMemberId
    );

    /**
     * 修改 SEND 狀態。
     */
    SendResponse changeSendStatus(
            Integer sendId,
            SendStatusUpdateRequest request,
            Integer loginMemberId
    );

    /**
     * DELETE Send。
     */
    void deleteSend(
            Integer sendId,
            Integer loginMemberId
    );

    /** 系統管理員依 OA / OC / OS 訊息類型查詢系統範本，不使用 sellerId。 */
    OffsetPageResponse<SendTemplateResponse> findSystemTemplates(Integer loginMemberId, Integer page);

    /** 依登入會員對應的 sellerId 查詢該商家的 SC 範本。 */
    OffsetPageResponse<SendTemplateResponse> findSellerTemplates(Integer loginMemberId, Integer page);

    /** 目前登入商家的寄件匣，不包含 SAVE 範本與 DELETE。 */
    OffsetPageResponse<SendResponse> findSellerOutbox(Integer loginMemberId, Integer page);
}
