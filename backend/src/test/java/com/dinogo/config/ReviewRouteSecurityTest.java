package com.dinogo.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dinogo.member.repository.MemberRepository;
import com.dinogo.security.JwtAuthenticationFilter;
import com.dinogo.security.JwtTokenUtil;

/** Verifies the shared SecurityConfig boundary for F's Review API paths. */
@WebMvcTest(ReviewRouteSecurityTest.ReviewSecurityProbeController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        ReviewRouteSecurityTest.ReviewSecurityProbeController.class
})
class ReviewRouteSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void publicReviewSummariesDoNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/reviews/products/1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reviews/products/1/rating-summary"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/reviews/sellers/1/rating-summary"))
                .andExpect(status().isOk());
    }

    @Test
    void memberReviewOperationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/reviews/orders/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/reviews/stars/1"))
                .andExpect(status().isUnauthorized());
    }

    @RestController
    static class ReviewSecurityProbeController {

        @GetMapping("/api/reviews/products/{productId}")
        void getProductReviews() {
        }

        @GetMapping("/api/reviews/products/{productId}/rating-summary")
        void getProductRatingSummary() {
        }

        @GetMapping("/api/reviews/sellers/{sellerId}/rating-summary")
        void getSellerRatingSummary() {
        }

        @GetMapping("/api/reviews/orders/{orderId}")
        void getOrderReviews() {
        }

        @PutMapping("/api/reviews/stars/{orderId}")
        void updateStars() {
        }
    }
}
