package com.dinogo.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

import com.dinogo.member.dto.LoginRequest;
import com.dinogo.member.dto.MemberApiErrorResponse;
import com.dinogo.member.dto.LoginResponse;
import com.dinogo.member.dto.GoogleLinkRequest;
import com.dinogo.member.dto.GoogleLoginRequest;
import com.dinogo.member.dto.PasswordResetRequest;
import com.dinogo.member.dto.ResetPasswordRequest;
import com.dinogo.member.service.GoogleAccountLinkRequiredException;
import com.dinogo.member.service.GoogleLoginService;
import com.dinogo.member.service.LoginService;
import com.dinogo.member.service.PasswordResetService;
import com.dinogo.member.service.PasswordResetRateLimitException;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Validated
@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);

    private final LoginService loginService;
    private final GoogleLoginService googleLoginService;
    private final PasswordResetService passwordResetService;

    public LoginController(
            LoginService loginService,
            GoogleLoginService googleLoginService,
            PasswordResetService passwordResetService) {
        this.loginService = loginService;
        this.googleLoginService = googleLoginService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(loginService.login(request));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MemberApiErrorResponse.from(
                            HttpStatus.UNAUTHORIZED,
                            "Email 或密碼錯誤"));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        try {
            return ResponseEntity.ok(googleLoginService.login(request));
        } catch (GoogleAccountLinkRequiredException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(MemberApiErrorResponse.from(HttpStatus.CONFLICT, exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MemberApiErrorResponse.from(HttpStatus.UNAUTHORIZED, exception.getMessage()));
        }
    }

    @PostMapping("/google/link")
    public ResponseEntity<?> linkGoogleAccount(@Valid @RequestBody GoogleLinkRequest request) {
        try {
            return ResponseEntity.ok(googleLoginService.link(request));
        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(MemberApiErrorResponse.from(HttpStatus.CONFLICT, exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MemberApiErrorResponse.from(HttpStatus.UNAUTHORIZED, exception.getMessage()));
        }
    }

    @PostMapping("/password-reset-requests")
    public ResponseEntity<?> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {
        try {
            passwordResetService.requestPasswordReset(request, httpRequest.getRemoteAddr());
            return passwordResetAccepted();
        } catch (PasswordResetRateLimitException exception) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(MemberApiErrorResponse.from(
                            HttpStatus.TOO_MANY_REQUESTS,
                            "請稍後再申請重設密碼。"));
        } catch (MailException exception) {
            LOGGER.warn("Password reset email delivery failed: {}", exception.getClass().getSimpleName());
            return passwordResetAccepted();
        }
    }

    private ResponseEntity<java.util.Map<String, String>> passwordResetAccepted() {
        return ResponseEntity.accepted().body(java.util.Map.of(
                "message", "若此 Email 已註冊，重設密碼說明已寄出。"));
    }

    @PostMapping("/password-resets")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            passwordResetService.resetPassword(request);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(MemberApiErrorResponse.from(HttpStatus.BAD_REQUEST, exception.getMessage()));
        }
    }
}
