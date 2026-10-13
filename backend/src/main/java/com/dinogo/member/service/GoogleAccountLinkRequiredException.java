package com.dinogo.member.service;

public class GoogleAccountLinkRequiredException extends RuntimeException {

    public GoogleAccountLinkRequiredException() {
        super("此 Email 已有密碼帳號，請輸入原密碼完成 Google 帳號綁定");
    }
}
