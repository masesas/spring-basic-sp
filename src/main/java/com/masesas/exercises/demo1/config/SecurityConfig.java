package com.masesas.exercises.demo1.config;

import com.masesas.exercises.demo1.owasp.safe.RateLimitFilter;
import com.masesas.exercises.demo1.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Konfigurasi keamanan tahap awal: SEMUA endpoint dibuka tanpa login.
 *
 * <p>Tanpa bean {@link SecurityFilterChain} ini, Spring Security memakai pengaturan bawaannya
 * yang meminta login basic-auth di setiap request (semua request dibalas 401).
 *
 * <p>Sejak A01 aturannya per-endpoint: tanpa token dibalas 401, token sah tapi peran
 * kurang dibalas 403. Yang tetap terbuka hanya endpoint login dan package demo
 * {@code /api/vuln/**} yang memang sengaja dibiarkan rentan.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthFilter jwtAuthFilter, RateLimitFilter rateLimitFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/api/safe/login").permitAll()
                        .requestMatchers("/api/vuln/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/karyawan/**", "/api/karyawan2/**")
                        .hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
