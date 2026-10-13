package com.dinogo.sysmsg.service.impl;

import org.springframework.stereotype.Service;
import com.dinogo.sysmsg.entity.RecordEntity;
import com.dinogo.sysmsg.entity.SendEntity;
import com.dinogo.sysmsg.dto.external.MemberAuthResponse;
import com.dinogo.member.service.MemberSysmsgProviderService;
import com.dinogo.sysmsg.service.ModuleDataMapper;
import com.dinogo.sysmsg.service.MsgPermissionService;

@Service
public class MsgPermissionServiceImpl implements MsgPermissionService {
    private final MemberSysmsgProviderService members;
    public MsgPermissionServiceImpl(MemberSysmsgProviderService members) {
        this.members = members;
    }

    @Override public Integer validateSeller(Integer loginMemberId) {
        MemberAuthResponse me = current(loginMemberId);
        if (me.getSellerId() == null || !me.getRoleIds().contains(2))
            throw new SecurityException("目前登入會員不是商家");
        return me.getSellerId();
    }
    @Override public void validateSystemAdmin(Integer loginMemberId) {
        MemberAuthResponse me = current(loginMemberId);
        if (!me.getRoleIds().contains(3))
            throw new SecurityException("需要系統後台管理員權限");
    }
    @Override public void validateTemplateOwner(SendEntity send, Integer loginMemberId) {
        if (send == null) throw new IllegalArgumentException("Send 不可為 null");
        String prefix = send.getMsgFunction().substring(0, 2);
        if (!"SC".equals(prefix)) { validateSystemAdmin(loginMemberId); return; }
        if (!send.getMsgfromSellerId().equals(validateSeller(loginMemberId))) throw new SecurityException("不是此商家範本的擁有者");
    }
    @Override public void validateRecordOwner(RecordEntity record, Integer loginMemberId) {
        MemberAuthResponse me = current(loginMemberId);
        boolean memberOwner = record.getMsgtoMemberId() != null && record.getMsgtoMemberId().equals(me.getMemberId());
        boolean sellerOwner = record.getMsgtoSellerId() != null && record.getMsgtoSellerId().equals(me.getSellerId());
        if (!memberOwner && !sellerOwner) throw new SecurityException("不是此收件訊息的擁有者");
    }
    private MemberAuthResponse current(Integer assertedId) {
        MemberAuthResponse me = ModuleDataMapper.member(members.getProfile(assertedId));
        if (!me.isAuthenticated() || me.getMemberId() == null) throw new SecurityException("會員驗證失敗");
        if (assertedId != null && !assertedId.equals(me.getMemberId())) throw new SecurityException("登入身分與請求不一致");
        return me;
    }
}
