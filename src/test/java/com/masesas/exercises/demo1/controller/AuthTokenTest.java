package com.masesas.exercises.demo1.controller;

import com.jayway.jsonpath.JsonPath;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthTokenTest {

    private static final String KARYAWAN = "hr@masesas.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoginAttemptService loginAttempts;

    @Value("${app.security.password}")
    private String password;

    @Value("${app.security.jwt-ttl-minutes}")
    private long ttlMenit;

    @BeforeEach
    void bersihkanKuncianLogin() {
        loginAttempts.reset(KARYAWAN);
    }

    @Test
    @DisplayName("token karyawan terbit dari form-urlencoded ala OAuth2 password flow")
    void tokenKaryawan_kredensialBenar() throws Exception {
        mockMvc.perform(tokenRequest("/api/auth/karyawan/token", KARYAWAN, password))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(ttlMenit * 60));
    }

    @Test
    @DisplayName("token hasil password flow bisa langsung dipakai memanggil endpoint ber-peran")
    void tokenKaryawan_bisaDipakai() throws Exception {
        String body = mockMvc.perform(tokenRequest("/api/auth/karyawan/token", KARYAWAN, password))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = JsonPath.read(body, "$.access_token");

        mockMvc.perform(get("/api/karyawan/all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("password salah ditolak 401")
    void tokenKaryawan_passwordSalah() throws Exception {
        mockMvc.perform(tokenRequest("/api/auth/karyawan/token", KARYAWAN, "password-salah"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("penguncian login tetap berlaku di endpoint token")
    void tokenKaryawan_terkunciSetelahGagalBerulang() throws Exception {
        while (!loginAttempts.isLocked(KARYAWAN)) {
            mockMvc.perform(tokenRequest("/api/auth/karyawan/token", KARYAWAN, "password-salah"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(tokenRequest("/api/auth/karyawan/token", KARYAWAN, password))
                .andExpect(status().isLocked());
    }

    @Test
    @DisplayName("username kosong ditolak sebelum kredensial diperiksa")
    void tokenKaryawan_usernameKosong() throws Exception {
        mockMvc.perform(tokenRequest("/api/auth/karyawan/token", "", password))
                .andExpect(status().isBadRequest());
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder tokenRequest(
            String path, String username, String password) {
        return post(path)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("grant_type", "password")
                .param("username", username)
                .param("password", password);
    }
}
