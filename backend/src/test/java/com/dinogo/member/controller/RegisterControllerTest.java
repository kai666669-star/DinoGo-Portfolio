package com.dinogo.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.dinogo.member.dto.MemberResponse;
import com.dinogo.member.dto.MemberApiErrorResponse;
import com.dinogo.member.dto.RegisterRequest;
import com.dinogo.member.dto.RegisterResponse;
import com.dinogo.member.service.MemberService;

@ExtendWith(MockitoExtension.class)
class RegisterControllerTest {

    @Mock
    private MemberService memberService;

    @InjectMocks
    private RegisterController registerController;

    @Test
    void registerReturnsCreatedResponse() {
        RegisterRequest request = new RegisterRequest(
                "user@example.com", "password123", "password123",
                "王", "小明", null, null
        );
        RegisterResponse response = new RegisterResponse(
                new MemberResponse(1, "user@example.com", "王", "小明", null, null, "ACTIVE")
        );
        when(memberService.register(request)).thenReturn(response);

        ResponseEntity<?> result = registerController.register(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(response);
        verify(memberService).register(request);
    }

    @Test
    void registerReturnsConflictWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "user@example.com", "password123", "password123",
                "王", "小明", null, null
        );
        when(memberService.register(request)).thenThrow(new IllegalArgumentException("Email 已被註冊"));

        ResponseEntity<?> result = registerController.register(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(result.getBody()).isEqualTo(MemberApiErrorResponse.from(
                HttpStatus.CONFLICT,
                "Email 已被註冊"));
    }

    @Test
    void registerReturnsBadRequestWhenPasswordsDoNotMatch() {
        RegisterRequest request = new RegisterRequest(
                "user@example.com", "password123", "different-password",
                "王", "小明", null, null
        );
        when(memberService.register(request)).thenThrow(new IllegalArgumentException("密碼與確認密碼不一致"));

        ResponseEntity<?> result = registerController.register(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(result.getBody()).isEqualTo(MemberApiErrorResponse.from(
                HttpStatus.BAD_REQUEST,
                "密碼與確認密碼不一致"));
    }
}
