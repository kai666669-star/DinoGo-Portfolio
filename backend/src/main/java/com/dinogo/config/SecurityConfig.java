package com.dinogo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.dinogo.security.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable())
                                .cors(Customizer.withDefaults())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .httpBasic(httpBasic -> httpBasic.disable())
                                .formLogin(formLogin -> formLogin.disable())
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers("/error").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/ecpay/callback").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/ecpay/order-result").permitAll()
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/products",
                                                                "/api/products/{productId:\\d+}",
                                                                "/api/categories/**",
                                                                "/api/subcategories/**",
                                                                "/api/brands/**",
                                                                "/api/coupons/available",
                                                                "/api/stores/search",
                                                                "/api/stores/{sellerId:\\d+}",
                                                                "/api/stores/{sellerId:\\d+}/summary",
                                                                "/uploads/products/**",
                                                                "/uploads/seller-logos/**")
                                                .permitAll()
                                                // 商品與商家評價摘要是公開內容；其他 Review 操作須登入。
                                                .requestMatchers(HttpMethod.GET,
                                                                "/api/reviews/products/{productId:\\d+}",
                                                                "/api/reviews/products/{productId:\\d+}/rating-summary",
                                                                "/api/reviews/sellers/{sellerId:\\d+}/rating-summary")
                                                .permitAll()
                                                .requestMatchers("/api/seller/**").hasRole("SELLER")
                                                .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("SELLER")
                                                .requestMatchers(HttpMethod.PATCH, "/api/products/**").hasRole("SELLER")
                                                .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("SELLER")
                                                .requestMatchers(HttpMethod.DELETE, "/api/products/**")
                                                .hasRole("SELLER")
                                                .requestMatchers(HttpMethod.PATCH, "/api/orders/*/status")
                                                .hasRole("SELLER")
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/orders/*/shipment")
                                                .hasRole("SELLER")
                                                .requestMatchers(
                                                                HttpMethod.PATCH,
                                                                "/api/orders/*/shipment/status")
                                                .hasRole("SELLER")
                                                .requestMatchers(
                                                                HttpMethod.PATCH,
                                                                "/api/orders/*/shipment/tracking-info")
                                                .hasRole("SELLER")
                                                .requestMatchers(
                                                                "/api/cart/**",
                                                                "/api/favorites/**",
                                                                "/api/checkout/**",
                                                                "/api/orders/**",
                                                                "/api/payments/**",
                                                                // 收件地址包含個資，只允許已登入會員存取。
                                                                "/api/addresses/**",
                                                                "/api/member/**",
                                                                "/api/sysmsg/**")
                                                .authenticated()
                                                // 評論資格與資料歸屬由 Review 模組以 JWT principal 驗證；
                                                // 缺少有效 JWT 時由 Security 層統一回傳 401。
                                                .requestMatchers(
                                                                "/api/reviews/orders/**",
                                                                "/api/reviews/stars/**")
                                                .authenticated()
                                                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                                                .anyRequest().denyAll())
                                .exceptionHandling(exceptionHandling -> exceptionHandling
                                                .authenticationEntryPoint((request, response, exception) -> response
                                                                .sendError(401,
                                                                                "Authentication is required"))
                                                .accessDeniedHandler((request, response, exception) -> response
                                                                .sendError(403, "Insufficient role")))
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}
