package com.dinogo.member.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dinogo.config.SecurityConfig;
import com.dinogo.member.dto.MemberResponse;
import com.dinogo.member.service.MemberService;
import com.dinogo.member.service.MemberAccountService;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.security.AuthenticatedMember;
import com.dinogo.security.JwtAuthenticationFilter;
import com.dinogo.security.JwtTokenUtil;

@WebMvcTest(MemberController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class MemberSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private MemberAccountService memberAccountService;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void profileEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/member/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/member/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUpdateJson()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/member/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPasswordJson()))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(memberService);
    }

    @Test
    void authenticatedMemberIdIsUsedForProfileLookup() throws Exception {
        when(memberService.getProfile(7)).thenReturn(
                new MemberResponse(7, "user@example.com", "王", "小明", null, null, "ACTIVE"));

        mockMvc.perform(get("/api/member/profile")
                        .with(authentication(authenticationForMember(7))))
                .andExpect(status().isOk());

        verify(memberService).getProfile(7);
    }

    @ParameterizedTest(name = "profile update rejects {0}")
    @MethodSource("invalidProfileUpdates")
    void updateProfileRejectsInvalidRequest(String scenario, String requestBody, String fieldName) throws Exception {
        mockMvc.perform(put("/api/member/profile")
                        .with(authentication(authenticationForMember(1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("輸入資料驗證失敗"))
                .andExpect(jsonPath("$.fieldErrors." + fieldName).isNotEmpty());

        verifyNoInteractions(memberService);
    }

    @ParameterizedTest(name = "password change rejects {0}")
    @MethodSource("invalidPasswordChanges")
    void changePasswordRejectsInvalidRequest(String scenario, String requestBody, String fieldName) throws Exception {
        mockMvc.perform(put("/api/member/password")
                        .with(authentication(authenticationForMember(1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors." + fieldName).isNotEmpty());

        verifyNoInteractions(memberService);
    }

    private static Stream<Arguments> invalidProfileUpdates() {
        return Stream.of(
                Arguments.of("blank last name", updateJson("", "小明"), "lastName"),
                Arguments.of("blank first name", updateJson("王", ""), "firstName"),
                Arguments.of("overlong first name", updateJson("王", "名".repeat(51)), "firstName"));
    }

    private static Stream<Arguments> invalidPasswordChanges() {
        return Stream.of(
                Arguments.of("blank current password", passwordJson("", "new-password1", "new-password1"), "currentPassword"),
                Arguments.of("short new password", passwordJson("current-password", "short", "short"), "newPassword"),
                Arguments.of("new password without English", passwordJson("current-password", "12345678", "12345678"), "newPassword"));
    }

    private static String validUpdateJson() {
        return updateJson("王", "小明");
    }

    private static String validPasswordJson() {
        return passwordJson("current-password", "new-password1", "new-password1");
    }

    private static String updateJson(String lastName, String firstName) {
        return """
                {
                  "lastName": "%s",
                  "firstName": "%s"
                }
                """.formatted(lastName, firstName);
    }

    private static String passwordJson(String currentPassword, String newPassword, String confirmNewPassword) {
        return """
                {
                  "currentPassword": "%s",
                  "newPassword": "%s",
                  "confirmNewPassword": "%s"
                }
                """.formatted(currentPassword, newPassword, confirmNewPassword);
    }

    private UsernamePasswordAuthenticationToken authenticationForMember(Integer memberId) {
        AuthenticatedMember principal = new AuthenticatedMember(memberId, "user@example.com");
        return new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
    }
}
