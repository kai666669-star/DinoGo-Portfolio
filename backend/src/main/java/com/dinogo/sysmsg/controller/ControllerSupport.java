package com.dinogo.sysmsg.controller;

import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.dinogo.member.service.MemberSysmsgProviderService;
import com.dinogo.security.AuthenticatedMember;
import com.dinogo.sysmsg.dto.external.MemberAuthResponse;
import com.dinogo.sysmsg.service.ModuleDataMapper;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

/** Controller 的可信任登入身分來源；不讀取 Request body 內的登入 ID。 */
@Component
public class ControllerSupport {
    private final MemberSysmsgProviderService members; //client
    public ControllerSupport(MemberSysmsgProviderService members) { //client
        this.members = members;
    }
    public MemberAuthResponse currentMember() {
        return ModuleDataMapper.member(members.getProfile(currentPrincipal().memberId())); //client
    }
    public Integer memberId() { return currentMember().getMemberId(); }
    public Integer sellerId() {
        Integer id = currentMember().getSellerId();
        if (id == null) throw new SecurityException("目前登入會員不是商家");
        return id;
    }

    private AuthenticatedMember currentPrincipal() { //假設client
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedMember member)) {
            throw new AuthenticationCredentialsNotFoundException("尚未登入");
        }
        return member;
    }
}
