package com.dinogo.member.controller;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dinogo.config.SecurityConfig;
import com.dinogo.member.service.AddressService;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.security.AuthenticatedMember;
import com.dinogo.security.JwtAuthenticationFilter;
import com.dinogo.security.JwtTokenUtil;

@WebMvcTest(AddressController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class AddressSecurityTest {

    // MockMvc 驗證實際 SecurityFilterChain 與 validation。
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddressService addressService;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private MemberRepository memberRepository;

    // 未登入與已登入授權測試。
    @Test
    void addressesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/addresses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedMemberCanGetAddresses() throws Exception {
        when(addressService.getAddresses(1)).thenReturn(List.of());

        mockMvc.perform(get("/api/addresses")
                        .with(authentication(authenticationForMember(1))))
                .andExpect(status().isOk());
    }

    // Request DTO validation 測試。
    @Test
    void createAddressValidatesRequest() throws Exception {
        mockMvc.perform(post("/api/addresses")
                        .with(authentication(authenticationForMember(1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("輸入資料驗證失敗"))
                .andExpect(jsonPath("$.fieldErrors.receiverName").isNotEmpty());

        verifyNoInteractions(addressService);
    }

    @Test
    void createAddressRejectsOverlongDetailAddress() throws Exception {
        String requestBody = """
                {
                  "receiverName": "王小明",
                  "receiverPhone": "0912345678",
                  "postalCode": "100",
                  "city": "台北市",
                  "district": "中正區",
                  "detailAddress": "%s",
                  "isDefault": false
                }
                """.formatted("路".repeat(256));

        mockMvc.perform(post("/api/addresses")
                        .with(authentication(authenticationForMember(1)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("輸入資料驗證失敗"))
                .andExpect(jsonPath("$.fieldErrors.detailAddress").isNotEmpty());

        verifyNoInteractions(addressService);
    }

    @Test
    void getAddressRejectsNonPositiveAddressIdWithStructuredError() throws Exception {
        mockMvc.perform(get("/api/addresses/0")
                        .with(authentication(authenticationForMember(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("請求參數驗證失敗"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());

        verifyNoInteractions(addressService);
    }

    // 建立符合專案 JWT principal 型別的測試 Authentication。
    private UsernamePasswordAuthenticationToken authenticationForMember(Integer memberId) {
        AuthenticatedMember principal = new AuthenticatedMember(memberId, "user@example.com");
        return new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
    }
}
