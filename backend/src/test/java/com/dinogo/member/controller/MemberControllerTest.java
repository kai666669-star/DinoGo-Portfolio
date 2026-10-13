package com.dinogo.member.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.dinogo.member.dto.MemberResponse;
import com.dinogo.member.dto.MemberApiErrorResponse;
import com.dinogo.member.dto.ChangePasswordRequest;
import com.dinogo.member.dto.MemberUpdateRequest;
import com.dinogo.member.service.MemberService;
import com.dinogo.security.AuthenticatedMember;

@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

        @Mock
        private MemberService memberService;

        @InjectMocks
        private MemberController memberController;

        // 模擬 JWT 驗證後的登入者：
        // memberId = 1，email = user@example.com
        private final AuthenticatedMember authenticatedMember = new AuthenticatedMember(1, "user@example.com");

        @Test
        void getProfileReturnsAuthenticatedMember() {
                MemberResponse response = new MemberResponse(
                                1,
                                "user@example.com",
                                "王",
                                "小明",
                                null,
                                "0912345678",
                                "ACTIVE");

                // MemberController 現在使用 memberId 查詢會員
                when(memberService.getProfile(1)).thenReturn(response);

                ResponseEntity<?> result = memberController.getProfile(authenticatedMember);

                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(result.getBody()).isEqualTo(response);

                // 確認 Controller 傳入的是 JWT 的 memberId
                verify(memberService).getProfile(1);
        }

        @Test
        void updateProfileReturnsUpdatedMember() {
                MemberUpdateRequest request = new MemberUpdateRequest(
                                "林",
                                "小美",
                                LocalDate.of(1998, 2, 3),
                                "0987654321");

                MemberResponse response = new MemberResponse(
                                1,
                                "user@example.com",
                                "林",
                                "小美",
                                request.birthDate(),
                                request.phone(),
                                "ACTIVE");

                // 更新會員資料時也使用 memberId
                when(memberService.updateProfile(1, request))
                                .thenReturn(response);

                ResponseEntity<?> result = memberController.updateProfile(
                                authenticatedMember,
                                request);

                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(result.getBody()).isEqualTo(response);

                // 確認只更新 JWT 對應的會員
                verify(memberService).updateProfile(1, request);
        }

        @Test
        void getProfileReturnsNotFoundWhenMemberDoesNotExist() {
                when(memberService.getProfile(1))
                                .thenThrow(new IllegalArgumentException("Member not found"));

                ResponseEntity<?> result = memberController.getProfile(authenticatedMember);

                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(result.getBody()).isEqualTo(MemberApiErrorResponse.from(
                                HttpStatus.NOT_FOUND,
                                "Member not found"));
        }

        @Test
        void updateProfileReturnsNotFoundWhenMemberDoesNotExist() {
                MemberUpdateRequest request = new MemberUpdateRequest(
                                "林",
                                "小美",
                                null,
                                null);

                when(memberService.updateProfile(1, request))
                                .thenThrow(new IllegalArgumentException("Member not found"));

                ResponseEntity<?> result = memberController.updateProfile(
                                authenticatedMember,
                                request);

                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                assertThat(result.getBody()).isEqualTo(MemberApiErrorResponse.from(
                                HttpStatus.NOT_FOUND,
                                "Member not found"));
        }

        @Test
        void changePasswordUsesAuthenticatedMemberAndReturnsNoContent() {
                ChangePasswordRequest request = new ChangePasswordRequest(
                                "current-password", "new-password", "new-password");

                ResponseEntity<?> result = memberController.changePassword(authenticatedMember, request);

                assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
                verify(memberService).changePassword(1, request);
        }
}
