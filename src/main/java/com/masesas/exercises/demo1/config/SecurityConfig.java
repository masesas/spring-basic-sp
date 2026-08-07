package com.masesas.exercises.demo1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Konfigurasi keamanan tahap awal: SEMUA endpoint dibuka tanpa login.
 *
 * <p>Tanpa bean {@link SecurityFilterChain} ini, Spring Security memakai pengaturan bawaannya
 * yang meminta login basic-auth di setiap request (semua request dibalas 401).
 *
 * <p><b>Hanya untuk development/training.</b> Sebelum dipakai di lingkungan nyata, ganti
 * {@code permitAll()} dengan aturan per-endpoint dan aktifkan kembali autentikasi.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(request -> request.anyRequest().permitAll())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
