package com.masesas.exercises.demo1.config;

import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import com.masesas.exercises.demo1.security.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Menjaga aturan otorisasi di SecurityConfig setelah A01 diterapkan.
 *
 * <p>Sebelum A01 kelas ini membuktikan kebalikannya — bahwa semua endpoint terbuka
 * tanpa login. Premis itu sengaja dibalik: sekarang endpoint bisnis menuntut token,
 * dan yang tetap terbuka hanyalah endpoint login serta package demo /api/vuln.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityWhitelistTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Autowired
    private LoginAttemptService loginAttempts;

    /**
     * Penghitung kegagalan login hidup di Redis yang dipakai bersama, bukan di memori
     * proses test. Tanpa reset ini, kelas test lain yang sengaja mengunci akun "hr"
     * membuat test login di sini gagal dengan 423.
     */
    @BeforeEach
    void bersihkanKuncianLogin() {
        loginAttempts.reset("hr");
    }

    @Test
    @DisplayName("GET tanpa token dibalas 401")
    void getTanpaToken_ditolak() throws Exception {
        mockMvc.perform(get("/api/karyawan/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET dengan token yang sah dibalas 200")
    void getDenganToken_diterima() throws Exception {
        mockMvc.perform(get("/api/karyawan/all")
                        .header(HttpHeaders.AUTHORIZATION, bearer("hr")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("endpoint login tetap terbuka tanpa token")
    void login_tetapTerbuka() throws Exception {
        mockMvc.perform(post("/api/safe/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"hr\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("path yang tidak terdaftar dibalas 401 saat tanpa token")
    void pathTidakDikenal_tanpaToken() throws Exception {
        mockMvc.perform(get("/path-yang-tidak-ada"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("path yang tidak terdaftar dibalas 404 saat token sah")
    void pathTidakDikenal_denganToken() throws Exception {
        mockMvc.perform(get("/path-yang-tidak-ada")
                        .header(HttpHeaders.AUTHORIZATION, bearer("hr")))
                .andExpect(status().isNotFound());
    }

    private String bearer(String username) {
        AppUser user = userDetailsService.loadUserByUsername(username);
        return "Bearer " + jwtService.issue(user, Instant.now());
    }
}
