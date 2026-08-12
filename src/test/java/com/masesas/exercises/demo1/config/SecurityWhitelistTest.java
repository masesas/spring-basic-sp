package com.masesas.exercises.demo1.config;

import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import com.masesas.exercises.demo1.security.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Value("${app.security.password}")
    private String password;

    @BeforeEach
    void bersihkanKuncianLogin() {
        loginAttempts.reset("hr@masesas.test");
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
                        .header(HttpHeaders.AUTHORIZATION, bearer("hr@masesas.test")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("endpoint login tetap terbuka tanpa token")
    void login_tetapTerbuka() throws Exception {
        mockMvc.perform(post("/api/auth/karyawan/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"hr@masesas.test\",\"password\":\"" + password + "\"}"))
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
                        .header(HttpHeaders.AUTHORIZATION, bearer("hr@masesas.test")))
                .andExpect(status().isNotFound());
    }

    private String bearer(String username) {
        AppUser user = userDetailsService.loadUserByUsername(username);
        return "Bearer " + jwtService.issue(user, Instant.now());
    }
}
