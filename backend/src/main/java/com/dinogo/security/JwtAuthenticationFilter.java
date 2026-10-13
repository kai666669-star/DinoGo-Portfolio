package com.dinogo.security;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.dinogo.member.entity.Member;
import com.dinogo.member.repository.MemberRepository;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenUtil jwtTokenUtil;
    private final MemberRepository memberRepository;

    public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil, MemberRepository memberRepository) {
        this.jwtTokenUtil = jwtTokenUtil;
        this.memberRepository = memberRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (!StringUtils.hasText(authorization)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String subject = jwtTokenUtil.extractSubject(token);
            Integer memberId = jwtTokenUtil.extractMemberId(token);
            int tokenAuthVersion = jwtTokenUtil.extractAuthVersion(token);
            List<GrantedAuthority> authorities = jwtTokenUtil.extractRoles(token).stream()
                    .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)))
                    .toList();
            if (memberId == null) {
                logger.debug("JWT memberId claim is missing in token");
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            Member member = memberRepository.findById(memberId).orElse(null);
            if (member == null || !"ACTIVE".equals(member.getStatus())
                    || member.getAuthVersion() != tokenAuthVersion) {
                logger.debug("JWT is no longer valid for member " + memberId);
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                AuthenticatedMember principal = new AuthenticatedMember(memberId, subject);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal,
                        null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException exception) {
            logger.debug("Invalid or expired token: " + exception.getMessage());
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }
}
