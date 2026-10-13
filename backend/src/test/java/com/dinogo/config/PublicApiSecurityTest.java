package com.dinogo.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dinogo.member.repository.MemberRepository;
import com.dinogo.security.AuthenticatedMember;
import com.dinogo.security.JwtAuthenticationFilter;
import com.dinogo.security.JwtTokenUtil;

/** Verifies the anonymous API contract and the default-deny SecurityConfig fallback. */
@WebMvcTest(PublicApiSecurityTest.PublicApiSecurityProbeController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        PublicApiSecurityTest.PublicApiSecurityProbeController.class
})
class PublicApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void confirmedPublicRoutesDoNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/subcategories"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/brands"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/coupons/available"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/stores/search"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/stores/1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/uploads/products/product-1.jpg"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/uploads/seller-logos/seller-1.jpg"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/ecpay/callback"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/ecpay/order-result"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/sysmsg/inbox"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/sysmsg/inbox")
                .with(SecurityMockMvcRequestPostProcessors.authentication(authentication())))
                .andExpect(status().isOk());
    }

    @Test
    void unlistedRouteRequiresAuthenticationAndIsDeniedAfterAuthentication() throws Exception {
        mockMvc.perform(get("/api/security-probe/unlisted"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/security-probe/unlisted")
                .with(SecurityMockMvcRequestPostProcessors.authentication(authentication())))
                .andExpect(status().isForbidden());
    }

    @Test
    void unlistedProductAndStoreSubpathsAreNotPublic() throws Exception {
        mockMvc.perform(get("/api/products/metrics"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/stores/metrics"))
                .andExpect(status().isUnauthorized());
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedMember(1, "member@example.com"),
                null,
                List.of());
    }

    @RestController
    static class PublicApiSecurityProbeController {

        @GetMapping({ "/api/products", "/api/products/{productId}" })
        void getProduct() {
        }

        @GetMapping("/api/categories")
        void getCategories() {
        }

        @GetMapping("/api/subcategories")
        void getSubcategories() {
        }

        @GetMapping("/api/brands")
        void getBrands() {
        }

        @GetMapping("/api/coupons/available")
        void getAvailableCoupons() {
        }

        @GetMapping({ "/api/stores/search", "/api/stores/{sellerId}" })
        void getStore() {
        }

        @GetMapping("/api/products/metrics")
        void getProductMetrics() {
        }

        @GetMapping("/api/stores/metrics")
        void getStoreMetrics() {
        }

        @GetMapping("/api/sysmsg/inbox")
        void getSystemMessageInbox() {
        }

        @GetMapping("/uploads/products/{filename}")
        void getProductImage() {
        }

        @GetMapping("/uploads/seller-logos/{filename}")
        void getSellerLogo() {
        }

        @PostMapping("/api/ecpay/callback")
        void ecpayCallback() {
        }

        @PostMapping("/api/ecpay/order-result")
        void orderResult() {
        }

        @GetMapping("/api/security-probe/unlisted")
        void unlisted() {
        }
    }
}
