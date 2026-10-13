package com.dinogo.sales.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dinogo.config.SecurityConfig;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.sales.dto.OrderListResponse;
import com.dinogo.sales.dto.order.OrderShipmentSummary;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.entity.ShipmentStatus;
import com.dinogo.sales.service.OrderService;
import com.dinogo.security.AuthenticatedMember;
import com.dinogo.security.JwtAuthenticationFilter;
import com.dinogo.security.JwtTokenUtil;

@WebMvcTest(OrderController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class OrderControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void memberOrderListIncludesShipmentStatusInJson() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 20, 10, 0);
        OrderShipmentSummary shipment = new OrderShipmentSummary(
                20,
                "Black Cat",
                "TRACK-1",
                ShipmentStatus.AVAILABLE_FOR_PICKUP,
                createdAt,
                createdAt.plusDays(1),
                null);
        OrderListResponse order = new OrderListResponse(
                10,
                "ORD202608200001",
                30,
                OrderStatus.SHIPPED,
                BigDecimal.valueOf(500),
                createdAt,
                List.of(),
                shipment);
        when(orderService.getMemberOrders(6)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/member")
                        .with(authentication(buyerAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderId").value(10))
                .andExpect(jsonPath("$[0].shipment.status")
                        .value("AVAILABLE_FOR_PICKUP"));

        verify(orderService).getMemberOrders(6);
    }

    private UsernamePasswordAuthenticationToken buyerAuthentication() {
        AuthenticatedMember principal = new AuthenticatedMember(6, "buyer@example.com");
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_BUYER")));
    }
}
