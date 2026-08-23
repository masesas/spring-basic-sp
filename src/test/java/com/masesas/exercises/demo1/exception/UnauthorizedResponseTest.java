package com.masesas.exercises.demo1.exception;

import com.masesas.exercises.demo1.security.LoginAttemptService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UnauthorizedResponseTest {

    private static final String ADMIN = "admin@masesas.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoginAttemptService loginAttempts;

    @BeforeEach
    @AfterEach
    void bersihkanPenguncian() {
        loginAttempts.reset(ADMIN);
    }

    @Test
    @DisplayName("tanpa token dibalas 401 berbentuk JSON seragam")
    void tanpaToken() throws Exception {
        mockMvc.perform(get("/api/karyawan/all"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Autentikasi diperlukan"));
    }

    @Test
    @DisplayName("token rusak dibalas 401 dengan pesan token tidak valid")
    void tokenRusak() throws Exception {
        mockMvc.perform(get("/api/karyawan/all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token.tidak.valid"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.message").value("Token tidak valid"));
    }

    @Test
    @DisplayName("login dengan password salah dibalas 401 berbentuk JSON seragam")
    void passwordSalah() throws Exception {
        mockMvc.perform(post("/api/auth/karyawan/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + ADMIN + "\",\"password\":\"salah-sekali\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(401))
                .andExpect(jsonPath("$.message").value("Username atau password salah"));
    }

    @Test
    @DisplayName("username tidak dikenal dibalas pesan yang sama dengan password salah")
    void usernameTidakDikenal() throws Exception {
        mockMvc.perform(post("/api/auth/karyawan/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tidak-ada@masesas.test\",\"password\":\"apa-saja\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Username atau password salah"));
    }
}
