package com.dinogo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {

        return new WebMvcConfigurer() {

            @Override
            public void addCorsMappings(CorsRegistry registry) {

                // ECPay posts payment notifications and browser return forms from its own origin.
                // These endpoints expose no application data and authenticate the callback signature.
                registry.addMapping("/api/ecpay/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("POST")
                        .allowedHeaders("*");

                registry.addMapping("/**")
                        .allowedOrigins(
                                "http://localhost:5173",
                                "http://localhost:8088",
                                "https://dinogo-shop.site")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);

            }
        };
    }
}
