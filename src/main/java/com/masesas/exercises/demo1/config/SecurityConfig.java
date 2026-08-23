package com.masesas.exercises.demo1.config;

import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtAuthFilter;
import com.masesas.exercises.demo1.security.UnauthorizedHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Configuration
public class SecurityConfig {

    public static final List<String> DOCS_ENDPOINTS = List.of(
            "/docs",
            "/docs/**"
    );

    public static final List<String> WHITELIST_ENDPOINTS = Stream.concat(
            Stream.of("/api/auth/**", "/api/rolemap/**"),
            DOCS_ENDPOINTS.stream()
    ).toList();

    private static final String CSP_API =
            "default-src 'none'; frame-ancestors 'none'; base-uri 'none'";

    private static final String CSP_DOCS =
            "default-src 'none'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; "
                    + "img-src 'self' data:; font-src 'self' data:; connect-src 'self' https://proxy.scalar.com https://api.scalar.com;"
                    + "frame-ancestors 'none'; base-uri 'none'";

    public static final List<String> CHILDREN_ROLE = List.of(
            "ADMIN",
            "MANAGER",
            "MARKETING",
            "SALES",
            "HR",
            "KARYAWAN",
            AppUserDetailsService.ROLE_CUSTOMER
    );

    @Value("${app.security.cors-allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    @Order(1)
    SecurityFilterChain docsFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(DOCS_ENDPOINTS.toArray(String[]::new))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP_DOCS))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .frameOptions(frame -> frame.deny()))
                .authorizeHttpRequests(request -> request.anyRequest().permitAll())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthFilter jwtAuthFilter,
            UnauthorizedHandler unauthorizedHandler) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                //.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .cors(Customizer.withDefaults())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP_API))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .frameOptions(frame -> frame.deny()))
                .authorizeHttpRequests(request -> request
                        .requestMatchers(WHITELIST_ENDPOINTS.toArray(String[]::new)).permitAll()
                        .anyRequest().authenticated()
                )
                .anonymous(anonymous -> anonymous
                        .principal(AppUser.PRINCIPAL_GUEST)
                        .authorities(AppUser.ROLE_GUEST))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(unauthorizedHandler))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role(AppUser.ROLE_SUPERADMIN)
                .implies(CHILDREN_ROLE.toArray(String[]::new))
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
        //konfigurasi.setAllowedOrigins(allowedOrigins); // comment for praticing angular FE
        konfigurasi.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "https://*.api-morpkhai.web.id"
        ));
        //konfigurasi.setAllowedOrigins(List.of("*"));
        konfigurasi.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
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
