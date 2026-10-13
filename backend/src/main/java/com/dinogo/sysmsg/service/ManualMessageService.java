package com.dinogo.sysmsg.service;

import com.dinogo.sysmsg.dto.request.send.SellerCreateRequest;
import com.dinogo.sysmsg.dto.request.send.SysCreateRequest;
import com.dinogo.sysmsg.dto.response.SendResponse;

public interface ManualMessageService {
    SendResponse createSellerSend(SellerCreateRequest request, Integer loginMemberId);
    SendResponse createSystemSend(SysCreateRequest request, Integer loginMemberId);
}
