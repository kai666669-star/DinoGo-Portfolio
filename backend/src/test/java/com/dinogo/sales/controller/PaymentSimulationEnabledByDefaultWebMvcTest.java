package com.dinogo.sales.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dinogo.config.SecurityConfig;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.sales.service.PaymentMethodService;
import com.dinogo.sales.service.PaymentService;
import com.dinogo.security.AuthenticatedMember;
import com.dinogo.security.JwtAuthenticationFilter;
import com.dinogo.security.JwtTokenUtil;

@WebMvcTest(controllers = { PaymentController.class, PaymentCapabilityController.class })
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class PaymentSimulationEnabledByDefaultWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private PaymentMethodService paymentMethodService;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void simulationIsEnabledByDefault() throws Exception {
        mockMvc.perform(post("/api/orders/10/payments/20/simulate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUCCESS\"}")
                        .with(authentication(buyerAuthentication())))
                .andExpect(status().isOk());

        verify(paymentService).simulatePaymentResult(eq(10), eq(20), eq(6), any());
    }

    @Test
    void buyerCanReadEnabledPaymentCapabilities() throws Exception {
        mockMvc.perform(get("/api/payments/capabilities")
                        .with(authentication(buyerAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationEnabled").value(true));
    }

    @Test
    void unauthenticatedMemberCannotReadPaymentCapabilities() throws Exception {
        mockMvc.perform(get("/api/payments/capabilities"))
                .andExpect(status().isUnauthorized());
    }

    private UsernamePasswordAuthenticationToken buyerAuthentication() {
        AuthenticatedMember principal = new AuthenticatedMember(6, "buyer@example.com");
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_BUYER")));
    }
}
