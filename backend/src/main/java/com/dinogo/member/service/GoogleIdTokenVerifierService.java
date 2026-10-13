package com.dinogo.member.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Component
public class GoogleIdTokenVerifierService implements GoogleIdentityVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenVerifierService(@Value("${google.oauth.client-id}") String clientId)
            throws GeneralSecurityException, IOException {
        if (!StringUtils.hasText(clientId)) {
            throw new IllegalStateException("GOOGLE_CLIENT_ID is not configured");
        }
        verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(List.of(clientId))
                .build();
    }

    @Override
    public GoogleIdentity verify(String credential) {
        try {
            GoogleIdToken token = verifier.verify(credential);
            if (token == null) {
                throw new IllegalArgumentException("Google 登入憑證無效");
            }

            GoogleIdToken.Payload payload = token.getPayload();
            if (!Boolean.TRUE.equals(payload.getEmailVerified())
                    || !StringUtils.hasText(payload.getEmail())
                    || !StringUtils.hasText(payload.getSubject())) {
                throw new IllegalArgumentException("Google 帳號尚未驗證 Email");
            }

            return new GoogleIdentity(
                    payload.getSubject(),
                    payload.getEmail(),
                    valueOf(payload.get("given_name")),
                    valueOf(payload.get("family_name")));
        } catch (IOException | GeneralSecurityException exception) {
            throw new IllegalArgumentException("Google 登入憑證無效", exception);
        }
    }

    private String valueOf(Object value) {
        return value == null ? null : value.toString();
    }
}
