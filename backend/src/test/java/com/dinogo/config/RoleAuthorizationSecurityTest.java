package com.dinogo.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

import com.dinogo.catalog.controller.ProductController;
import com.dinogo.catalog.service.ProductService;
import com.dinogo.coupon.controller.CouponController;
import com.dinogo.coupon.service.CouponService;
import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;
import com.dinogo.sales.controller.OrderController;
import com.dinogo.sales.controller.ShipmentController;
import com.dinogo.sales.entity.OrderStatus;
import com.dinogo.sales.service.OrderService;
import com.dinogo.sales.service.ShipmentService;
import com.dinogo.security.AuthenticatedMember;
import com.dinogo.security.JwtAuthenticationFilter;
import com.dinogo.security.JwtTokenUtil;
import com.dinogo.seller.controller.SellerProductController;
import com.dinogo.seller.service.CurrentSellerService;
import com.dinogo.seller.service.SellerProductService;

@WebMvcTest({
        SellerProductController.class,
        CouponController.class,
        ProductController.class,
        OrderController.class,
        ShipmentController.class
})
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class RoleAuthorizationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SellerProductService sellerProductService;

    @MockitoBean
    private CouponService couponService;

    @MockitoBean
    private CurrentSellerService currentSellerService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private ShipmentService shipmentService;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void sellerOperationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/seller/products").param("sellerId", "1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/products/1/images/2"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(sellerProductService, productService, orderService);
    }

    @Test
    void buyerCannotAccessSellerEndpointsOrProductWrites() throws Exception {
        mockMvc.perform(get("/api/seller/products")
                        .param("sellerId", "1")
                        .with(authentication(authenticationForRole("BUYER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(authentication(authenticationForRole("BUYER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/products/1")
                        .with(authentication(authenticationForRole("BUYER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/products/1/images/2")
                        .with(authentication(authenticationForRole("BUYER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/seller/coupons")
                        .param("sellerId", "1")
                        .with(authentication(authenticationForRole("BUYER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(sellerProductService, couponService, productService);
    }

    @Test
    void buyerCannotUpdateOrderStatus() throws Exception {
        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}")
                        .with(authentication(authenticationForRole("BUYER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(orderService);
    }

    @Test
    void buyerCannotUpdateShipmentTrackingInfo() throws Exception {
        mockMvc.perform(patch("/api/orders/1/shipment/tracking-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carrierName\":\"Black Cat\",\"trackingNo\":\"TRACK-1\"}")
                        .with(authentication(authenticationForRole("BUYER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(shipmentService);
    }

    @Test
    void buyerCannotCreateShipmentOrUpdateShipmentStatus() throws Exception {
        mockMvc.perform(post("/api/orders/1/shipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carrierName\":\"Black Cat\",\"trackingNo\":\"TRACK-1\"}")
                        .with(authentication(authenticationForRole("BUYER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/orders/1/shipment/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}")
                        .with(authentication(authenticationForRole("BUYER"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(shipmentService);
    }

    @Test
    void adminCannotAccessSellerEndpointsUntilAnAdminContractExists() throws Exception {
        mockMvc.perform(get("/api/seller/coupons")
                        .param("sellerId", "1")
                        .with(authentication(authenticationForRole("ADMIN"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(couponService);
    }

    @Test
    void legacyTokenWithoutRolesCannotAccessSellerEndpoints() throws Exception {
        Member member = new Member();
        member.setMemberId(1);
        when(jwtTokenUtil.extractSubject("legacy-token")).thenReturn("member@example.com");
        when(jwtTokenUtil.extractMemberId("legacy-token")).thenReturn(1);
        when(jwtTokenUtil.extractRoles("legacy-token")).thenReturn(List.of());
        when(memberRepository.findById(1)).thenReturn(java.util.Optional.of(member));

        mockMvc.perform(get("/api/seller/products")
                        .param("sellerId", "1")
                        .header("Authorization", "Bearer legacy-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(sellerProductService);
    }

    @Test
    void publicEndpointWithInvalidTokenAllowsAccess() throws Exception {
        when(jwtTokenUtil.extractSubject("invalid-token")).thenThrow(new IllegalArgumentException("invalid token"));

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpointWithInvalidTokenRejectsAccessWithUnauthorized() throws Exception {
        when(jwtTokenUtil.extractSubject("invalid-token")).thenThrow(new IllegalArgumentException("invalid token"));

        mockMvc.perform(get("/api/seller/products")
                        .param("sellerId", "1")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(sellerProductService);
    }

    @Test
    void sellerCanReachSellerEndpointsAndOrderStatusUpdate() throws Exception {
        when(sellerProductService.getProducts(1)).thenReturn(List.of());
        when(couponService.getCoupons(1)).thenReturn(List.of());
        when(currentSellerService.requireMatchingActiveSellerId(1, 1)).thenReturn(1);

        mockMvc.perform(get("/api/seller/products")
                        .param("sellerId", "1")
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/seller/coupons")
                        .param("sellerId", "1")
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/products/1")
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/products/1/images/2")
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}")
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isOk());

        verify(sellerProductService).getProducts(1);
        verify(couponService).getCoupons(1);
        verify(productService).createProduct(any(), eq(1));
        verify(productService).deleteProduct(1, 1);
        verify(productService).deleteImage(1, 2, 1);
        verify(orderService).updateStatusBySeller(1, 1, OrderStatus.PROCESSING, null);
    }

    @Test
    void sellerCanReachShipmentTrackingInfoUpdate() throws Exception {
        mockMvc.perform(patch("/api/orders/1/shipment/tracking-info")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carrierName\":\"Black Cat\",\"trackingNo\":\"TRACK-1\"}")
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isOk());

        verify(shipmentService).updateShipmentTrackingInfo(eq(1), eq(1), any());
    }

    @Test
    void sellerCanCreateShipmentAndUpdateShipmentStatus() throws Exception {
        mockMvc.perform(post("/api/orders/1/shipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carrierName\":\"Black Cat\",\"trackingNo\":\"TRACK-1\"}")
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/orders/1/shipment/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}")
                        .with(authentication(authenticationForRole("SELLER"))))
                .andExpect(status().isOk());

        verify(shipmentService).createShipment(eq(1), eq(1), any());
        verify(shipmentService).updateShipmentStatus(eq(1), eq(1), any());
    }

    private UsernamePasswordAuthenticationToken authenticationForRole(String role) {
        AuthenticatedMember principal = new AuthenticatedMember(1, "member@example.com");
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }
}
