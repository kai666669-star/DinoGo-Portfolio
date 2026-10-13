package com.dinogo.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.dinogo.member.repository.MemberRepository;
import com.dinogo.review.controller.ReviewController;
import com.dinogo.review.service.ReviewImageService;
import com.dinogo.review.service.ReviewService;
import com.dinogo.security.JwtAuthenticationFilter;
import com.dinogo.security.JwtTokenUtil;

//review-start，總共1次修改，第1次//
/** 驗證公開評分查詢與會員訂單評論的 Security 邊界。 */
@WebMvcTest(ReviewController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class })
class ReviewSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private ReviewImageService reviewImageService;

    @MockitoBean
    private JwtTokenUtil jwtTokenUtil;

    @MockitoBean
    private MemberRepository memberRepository;

    @Test
    void publicProductReviewsDoNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/reviews/products/1"))
                .andExpect(status().isOk());
    }

    @Test
    void memberReviewOperationsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/reviews/orders/1"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/reviews/stars/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fiveStar\":5}"))
                .andExpect(status().isUnauthorized());
    }
}
//review-end，總共1次修改，第1次//
