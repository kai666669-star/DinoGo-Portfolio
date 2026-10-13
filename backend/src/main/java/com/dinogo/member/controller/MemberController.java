package com.dinogo.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

import com.dinogo.member.dto.ChangePasswordRequest;
import com.dinogo.member.dto.DeactivateAccountRequest;
import com.dinogo.member.dto.MemberApiErrorResponse;
import com.dinogo.member.dto.MemberUpdateRequest;
import com.dinogo.member.service.MemberService;
import com.dinogo.member.service.MemberAccountService;
import com.dinogo.security.AuthenticatedMember;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/member")
public class MemberController {

    private final MemberService memberService;
    private final MemberAccountService memberAccountService;

    public MemberController(MemberService memberService, MemberAccountService memberAccountService) {
        this.memberService = memberService;
        this.memberAccountService = memberAccountService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            @AuthenticationPrincipal AuthenticatedMember member) {
        try {
            // member.memberId() 是登入者的會員 ID
            return ResponseEntity.ok(
                    memberService.getProfile(member.memberId()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(MemberApiErrorResponse.from(HttpStatus.NOT_FOUND, exception.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody MemberUpdateRequest request) {
        try {
            // 只能修改 JWT 對應的會員
            return ResponseEntity.ok(
                    memberService.updateProfile(member.memberId(), request));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(MemberApiErrorResponse.from(HttpStatus.NOT_FOUND, exception.getMessage()));
        }
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody ChangePasswordRequest request) {
        try {
            memberService.changePassword(member.memberId(), request);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(MemberApiErrorResponse.from(HttpStatus.NOT_FOUND, exception.getMessage()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(MemberApiErrorResponse.from(HttpStatus.BAD_REQUEST, exception.getMessage()));
        }
    }

    @PostMapping("/account/deactivate")
    public ResponseEntity<?> deactivateAccount(@AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody DeactivateAccountRequest request) {
        try {
            memberAccountService.deactivate(member.memberId(), request.currentPassword());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(MemberApiErrorResponse.from(HttpStatus.BAD_REQUEST, exception.getMessage()));
        }
    }
}
