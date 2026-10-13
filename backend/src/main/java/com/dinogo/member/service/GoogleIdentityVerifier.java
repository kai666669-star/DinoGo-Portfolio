package com.dinogo.member.service;

public interface GoogleIdentityVerifier {

    GoogleIdentity verify(String credential);
}
