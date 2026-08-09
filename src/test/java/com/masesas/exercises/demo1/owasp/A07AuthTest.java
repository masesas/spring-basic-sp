package com.masesas.exercises.demo1.owasp;

import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import com.masesas.exercises.demo1.security.LoginAttemptService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("owasp-demo")
class A07AuthTest {

    private static final String USERNAME = "hr";
    private static final String PASSWORD = "Password123!";
    private static final String PASSWORD_SALAH = "salah";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Autowired
    private LoginAttemptService loginAttempts;

    @BeforeEach
    @AfterEach
    void bersihkanPenghitung() {
        loginAttempts.reset(USERNAME);
    }

    @Test
    @DisplayName("RENTAN: token dari /api/vuln/login tidak punya masa berlaku")
    void vuln_tokenTanpaMasaBerlaku() throws Exception {
        Claims claims = jwtService.parse(login("/api/vuln/login", PASSWORD));

        assertThat(claims.getExpiration()).isNull();
    }

    @Test
    @DisplayName("AMAN: token dari /api/safe/login kedaluwarsa dalam 15 menit")
    void safe_tokenPunyaMasaBerlaku() throws Exception {
        Claims claims = jwtService.parse(login("/api/safe/login", PASSWORD));

        assertThat(claims.getExpiration()).isNotNull();
        long menit = ChronoUnit.MINUTES.between(
                claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant());
        assertThat(menit).isEqualTo(15);
    }

    @Test
    @DisplayName("AMAN: token yang sudah kedaluwarsa ditolak 401")
    void safe_tokenKedaluwarsaDitolak() throws Exception {
        AppUser user = userDetailsService.loadUserByUsername(USERNAME);
        String kedaluwarsa = jwtService.issue(user, Instant.now().minus(20, ChronoUnit.MINUTES));

        mockMvc.perform(get("/api/karyawan/all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + kedaluwarsa))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("RENTAN: 10 kali password salah di /api/vuln/login tidak pernah mengunci akun")
    void vuln_percobaanTidakTerbatas() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(loginRequest("/api/vuln/login", PASSWORD_SALAH))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(loginRequest("/api/vuln/login", PASSWORD))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AMAN: percobaan ke-6 di /api/safe/login dibalas 423 Locked")
    void safe_akunTerkunciSetelahLimaKegagalan() throws Exception {
        for (int i = 0; i < LoginAttemptService.MAX_ATTEMPTS; i++) {
            mockMvc.perform(loginRequest("/api/safe/login", PASSWORD_SALAH))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(loginRequest("/api/safe/login", PASSWORD))
                .andExpect(status().isLocked());
    }

    private String login(String path, String password) throws Exception {
        MvcResult result = mockMvc.perform(loginRequest(path, password))
                .andExpect(status().isOk())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    private org.springframework.test.web.servlet.RequestBuilder loginRequest(String path, String password) {
        return post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + USERNAME + "\",\"password\":\"" + password + "\"}");
    }
}
