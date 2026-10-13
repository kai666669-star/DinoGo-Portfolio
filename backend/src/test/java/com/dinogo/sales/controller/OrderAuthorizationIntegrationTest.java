package com.dinogo.sales.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dinogo.catalog.repository.ProductSkuRepository;
import com.dinogo.config.SecurityConfig;
import com.dinogo.coupon.service.CouponUsageService;
import com.dinogo.member.repository.AddressRepository;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.sales.service.OrderService;
import com.dinogo.security.AuthenticatedMember;
import com.dinogo.security.JwtAuthenticationFilter;
import com.dinogo.security.JwtTokenUtil;
import com.dinogo.seller.repository.SellerRepository;

@WebMvcTest(OrderController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        OrderService.class
})
class OrderAuthorizationIntegrationTest {

    private static final String ENDPOINT = "/api/orders/99";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private AddressRepository addressRepository;

    @MockitoBean
    private ProductSkuRepository productSkuRepository;

    @MockitoBean
    private SellerRepository sellerRepository;

    @MockitoBean
    private CouponUsageService couponUsageService;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void memberCannotReadAnotherMembersOrder() throws Exception {
        Order memberBOrder = new Order();
        memberBOrder.setOrderId(99);
        memberBOrder.setBuyerId(7);
        when(orderRepository.findByOrderIdAndBuyerId(99, 7))
                .thenReturn(Optional.of(memberBOrder));
        when(orderRepository.findByOrderIdAndBuyerId(99, 6))
                .thenReturn(Optional.empty());

        mockMvc.perform(get(ENDPOINT)
                        .with(authentication(memberAAuthentication())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order does not exist"))
                .andExpect(jsonPath("$.path").value(ENDPOINT));

        verify(orderRepository).findByOrderIdAndBuyerId(99, 6);
        verify(orderRepository, never()).findByOrderIdAndBuyerId(99, 7);
        verify(orderRepository, never()).findById(99);
    }

    private UsernamePasswordAuthenticationToken memberAAuthentication() {
        AuthenticatedMember principal = new AuthenticatedMember(6, "member-a@example.com");
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_BUYER")));
    }
}
