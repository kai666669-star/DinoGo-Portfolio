package com.dinogo.sales.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
import com.dinogo.sales.dto.payment.PaymentResponse;
import com.dinogo.sales.entity.PaymentStatus;
import com.dinogo.sales.service.PaymentMethodService;
import com.dinogo.sales.service.PaymentService;
import com.dinogo.security.AuthenticatedMember;
import com.dinogo.security.JwtAuthenticationFilter;
import com.dinogo.security.JwtTokenUtil;

@WebMvcTest(
        controllers = { PaymentController.class, PaymentCapabilityController.class },
        properties = "app.payment.simulation-enabled=true")
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class PaymentControllerWebMvcTest {

    private static final String ENDPOINT = "/api/orders/10/payments/20/simulate";

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
    void buyerCanSimulateSuccessfulPayment() throws Exception {
        when(paymentService.simulatePaymentResult(eq(10), eq(20), eq(6), any()))
                .thenReturn(paymentResponse());

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUCCESS\",\"failureReason\":null}")
                        .with(authentication(buyerAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(20))
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(paymentService).simulatePaymentResult(eq(10), eq(20), eq(6), any());
    }

    @Test
    void missingStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(authentication(buyerAuthentication())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(paymentService);
    }

    @Test
    void unauthenticatedMemberCannotSimulatePayment() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUCCESS\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentService);
    }

    @Test
    void buyerCanReadEnabledPaymentCapabilities() throws Exception {
        mockMvc.perform(get("/api/payments/capabilities")
                        .with(authentication(buyerAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.simulationEnabled").value(true));
    }

    private UsernamePasswordAuthenticationToken buyerAuthentication() {
        AuthenticatedMember principal = new AuthenticatedMember(6, "buyer@example.com");
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_BUYER")));
    }

    private PaymentResponse paymentResponse() {
        return new PaymentResponse(
                20,
                "PAY-TEST",
                10,
                "CREDIT_CARD",
                new BigDecimal("1000.00"),
                PaymentStatus.SUCCESS,
                "SIM-TEST",
                null,
                null,
                null);
    }
}
