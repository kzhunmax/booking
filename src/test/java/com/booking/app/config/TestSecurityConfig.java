package com.booking.app.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
@EnableMethodSecurity
public class TestSecurityConfig {

    @Bean
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/auth/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/resources/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/bookings/available-slots")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .build();
    }
}
