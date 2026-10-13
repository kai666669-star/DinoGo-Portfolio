package com.dinogo.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dinogo.member.dto.MemberApiErrorResponse;
import com.dinogo.member.dto.RegisterRequest;
import com.dinogo.member.dto.RegisterResponse;
import com.dinogo.member.service.MemberService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/auth")
public class RegisterController {

    private final MemberService memberService;

    public RegisterController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            RegisterResponse response = memberService.register(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException exception) {
            HttpStatus status = "Email 已被註冊".equals(exception.getMessage())
                    ? HttpStatus.CONFLICT
                    : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status)
                    .body(MemberApiErrorResponse.from(status, exception.getMessage()));
        }
    }
}
