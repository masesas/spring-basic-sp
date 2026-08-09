package com.masesas.exercises.demo1.config;

import com.masesas.exercises.demo1.owasp.safe.RateLimitFilter;
import com.masesas.exercises.demo1.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

/**
 * Konfigurasi keamanan aplikasi.
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

    private final List<String> allowedOrigins;

    public SecurityConfig(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        this.allowedOrigins = List.copyOf(allowedOrigins);
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtAuthFilter jwtAuthFilter, RateLimitFilter rateLimitFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .frameOptions(frame -> frame.deny()))
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

    /**
     * Daftar origin ditulis eksplisit, bukan {@code *}. Dengan {@code allowCredentials}
     * menyala, wildcard ditolak spesifikasi CORS — dan seandainya diizinkan pun, itu
     * berarti situs mana pun boleh memanggil API ini memakai kredensial korban.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration konfigurasi = new CorsConfiguration();
        konfigurasi.setAllowedOrigins(allowedOrigins);
        konfigurasi.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        konfigurasi.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        konfigurasi.setAllowCredentials(true);
        konfigurasi.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource sumber = new UrlBasedCorsConfigurationSource();
        sumber.registerCorsConfiguration("/api/**", konfigurasi);
        return sumber;
    }

    /**
     * {@link DelegatingPasswordEncoder} menyimpan pengenal algoritma sebagai awalan
     * pada hash-nya ({@code {bcrypt}$2a$12$...}). Tanpa itu, mengganti algoritma di
     * kemudian hari berarti seluruh password lama tidak bisa lagi diverifikasi.
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        String baku = "bcrypt";
        Map<String, PasswordEncoder> encoders = Map.of(baku, new BCryptPasswordEncoder(12));
        return new DelegatingPasswordEncoder(baku, encoders);
    }
}
