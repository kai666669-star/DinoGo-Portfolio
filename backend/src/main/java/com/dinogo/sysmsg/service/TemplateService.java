package com.dinogo.sysmsg.service;

import com.dinogo.sysmsg.dto.request.template.SellerTemplateApplyRequest;
import com.dinogo.sysmsg.dto.request.template.SellerTemplateCreateRequest;
import com.dinogo.sysmsg.dto.request.template.SendTemplateUpdateRequest;
import com.dinogo.sysmsg.dto.request.template.SysTemplateApplyRequest;
import com.dinogo.sysmsg.dto.request.template.SysTemplateCreateRequest;
import com.dinogo.sysmsg.dto.response.SendResponse;
import com.dinogo.sysmsg.dto.response.SendTemplateResponse;
import com.dinogo.sysmsg.dto.response.OffsetPageResponse;

public interface TemplateService {
    SendTemplateResponse createSellerTemplate(SellerTemplateCreateRequest request, Integer loginMemberId);
    SendTemplateResponse createSystemTemplate(SysTemplateCreateRequest request, Integer loginMemberId);
    SendTemplateResponse updateTemplate(Integer sendId, SendTemplateUpdateRequest request, Integer loginMemberId);
    SendResponse applySystemTemplate(SysTemplateApplyRequest request, Integer loginMemberId);
    SendResponse applySellerTemplate(SellerTemplateApplyRequest request, Integer loginMemberId);
    OffsetPageResponse<SendTemplateResponse> findSystemTemplates(Integer loginMemberId, Integer page);
    OffsetPageResponse<SendTemplateResponse> findSellerTemplates(Integer loginMemberId, Integer page);
}
