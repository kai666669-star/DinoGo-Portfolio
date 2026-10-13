package com.dinogo.sales.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

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
import com.dinogo.catalog.repository.ProductRepository;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.sales.entity.Order;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.entity.Shipment;
import com.dinogo.sales.repository.OrderRepository;
import com.dinogo.sales.repository.PaymentRepository;
import com.dinogo.sales.repository.ShipmentRepository;
import com.dinogo.sales.repository.ShipmentEventRepository;
import com.dinogo.sales.service.ShipmentService;
import com.dinogo.security.AuthenticatedMember;
import com.dinogo.security.JwtAuthenticationFilter;
import com.dinogo.security.JwtTokenUtil;
import com.dinogo.seller.entity.Seller;
import com.dinogo.seller.repository.SellerRepository;

@WebMvcTest(ShipmentController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        ShipmentService.class
})
class ShipmentCreationIntegrationTest {

    private static final String ENDPOINT = "/api/orders/10/shipment";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ShipmentRepository shipmentRepository;

    @MockitoBean
    private ShipmentEventRepository shipmentEventRepository;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private PaymentRepository paymentRepository;

    @MockitoBean
    private SellerRepository sellerRepository;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void validFrontendPayloadPassesControllerValidationAndCreatesShipment() throws Exception {
        Seller seller = mock(Seller.class);
        when(seller.getSellerId()).thenReturn(30);
        when(seller.getStatus()).thenReturn("ACTIVE");
        when(sellerRepository.findByMember_MemberId(8)).thenReturn(Optional.of(seller));

        Order order = new Order();
        order.setOrderId(10);
        order.setSellerId(30);
        order.setStatus(OrderStatus.PAID);
        when(orderRepository.findForShipmentCreation(10, 30)).thenReturn(Optional.of(order));
        when(shipmentRepository.existsByOrderOrderId(10)).thenReturn(false);
        when(shipmentRepository.save(any(Shipment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"carrierName":"Black Cat","trackingNo":"TRACK-1"}
                                """)
                        .with(authentication(sellerAuthentication())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(10))
                .andExpect(jsonPath("$.carrierName").value("Black Cat"))
                .andExpect(jsonPath("$.trackingNo").value("TRACK-1"))
                .andExpect(jsonPath("$.status").value("PREPARING"));

        verify(shipmentRepository).save(any(Shipment.class));
    }

    @Test
    void blankFrontendPayloadStopsAtControllerValidationBeforeServiceRepositories() throws Exception {
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carrierName\":\"   \",\"trackingNo\":\"\"}")
                        .with(authentication(sellerAuthentication())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(
                shipmentRepository,
                orderRepository,
                paymentRepository,
                sellerRepository,
                productRepository);
    }

    private UsernamePasswordAuthenticationToken sellerAuthentication() {
        AuthenticatedMember principal = new AuthenticatedMember(8, "seller@example.com");
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_SELLER")));
    }
}
