package com.dinogo.sysmsg.service;


import com.dinogo.sysmsg.entity.RecordEntity;
import com.dinogo.sysmsg.entity.SendEntity;

public interface MsgPermissionService {

    Integer validateSeller(
            Integer loginMemberId
    );

    void validateSystemAdmin(
            Integer loginMemberId
    );

    void validateTemplateOwner(
            SendEntity send,
            Integer loginMemberId
    );

    void validateRecordOwner(
            RecordEntity record,
            Integer loginMemberId
    );
}
