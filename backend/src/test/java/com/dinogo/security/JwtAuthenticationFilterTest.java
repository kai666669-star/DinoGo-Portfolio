package com.dinogo.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;

class JwtAuthenticationFilterTest {

    @BeforeEach
    void clearSecurityContextBeforeTest() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validBearerTokenCreatesAuthentication() throws Exception {
        JwtTokenUtil jwtTokenUtil = mock(JwtTokenUtil.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        when(jwtTokenUtil.extractSubject("valid-token")).thenReturn("user@example.com");
        when(jwtTokenUtil.extractMemberId("valid-token")).thenReturn(6);
        when(jwtTokenUtil.extractAuthVersion("valid-token")).thenReturn(1);
        when(jwtTokenUtil.extractRoles("valid-token")).thenReturn(List.of("buyer", "seller"));
        when(memberRepository.findById(6)).thenReturn(java.util.Optional.of(member(6, 1)));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenUtil, memberRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo(new AuthenticatedMember(6, "user@example.com"));
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_BUYER", "ROLE_SELLER");
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void tokenWithoutRolesClaimCreatesAuthenticationWithoutAuthorities() throws Exception {
        JwtTokenUtil jwtTokenUtil = mock(JwtTokenUtil.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        when(jwtTokenUtil.extractSubject("legacy-token")).thenReturn("user@example.com");
        when(jwtTokenUtil.extractMemberId("legacy-token")).thenReturn(6);
        when(jwtTokenUtil.extractAuthVersion("legacy-token")).thenReturn(0);
        when(jwtTokenUtil.extractRoles("legacy-token")).thenReturn(List.of());
        when(memberRepository.findById(6)).thenReturn(java.util.Optional.of(member(6, 0)));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenUtil, memberRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer legacy-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).isEmpty();
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void invalidBearerTokenClearsAuthenticationAndContinuesChain() throws Exception {
        JwtTokenUtil jwtTokenUtil = mock(JwtTokenUtil.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        when(jwtTokenUtil.extractSubject("invalid-token"))
                .thenThrow(new IllegalArgumentException("invalid token"));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenUtil, memberRepository);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void authEndpointSkipsJwtFilter() throws Exception {
        JwtTokenUtil jwtTokenUtil = mock(JwtTokenUtil.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenUtil, memberRepository);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setServletPath("/api/auth/login");
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void staleTokenDoesNotCreateAuthentication() throws Exception {
        JwtTokenUtil jwtTokenUtil = mock(JwtTokenUtil.class);
        MemberRepository memberRepository = mock(MemberRepository.class);
        when(jwtTokenUtil.extractSubject("stale-token")).thenReturn("user@example.com");
        when(jwtTokenUtil.extractMemberId("stale-token")).thenReturn(6);
        when(jwtTokenUtil.extractAuthVersion("stale-token")).thenReturn(1);
        when(jwtTokenUtil.extractRoles("stale-token")).thenReturn(List.of("buyer"));
        when(memberRepository.findById(6)).thenReturn(java.util.Optional.of(member(6, 2)));
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenUtil, memberRepository);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer stale-token");
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private Member member(int memberId, int authVersion) {
        Member member = new Member();
        member.setMemberId(memberId);
        member.setAuthVersion(authVersion);
        return member;
    }
}
