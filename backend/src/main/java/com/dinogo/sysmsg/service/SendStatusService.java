package com.dinogo.sysmsg.service;

import com.dinogo.sysmsg.dto.request.record.SendStatusUpdateRequest;
import com.dinogo.sysmsg.dto.response.SendResponse;

public interface SendStatusService {
    SendResponse changeSendStatus(Integer sendId, SendStatusUpdateRequest request, Integer loginMemberId);
    void deleteSend(Integer sendId, Integer loginMemberId);
}
