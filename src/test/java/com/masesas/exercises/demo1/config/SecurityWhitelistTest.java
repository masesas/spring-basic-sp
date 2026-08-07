package com.masesas.exercises.demo1.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Membuktikan semua endpoint bisa dipanggil tanpa login.
 * MockMvc di sini melewati rantai filter Spring Security yang sebenarnya,
 * jadi kalau whitelist-nya salah, test ini akan mendapat 401 atau 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityWhitelistTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET tanpa login dibalas 200, bukan 401")
    void getTanpaLogin_tidakDitolak() throws Exception {
        mockMvc.perform(get("/api/karyawan/all"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST tanpa login dan tanpa token CSRF tidak dibalas 401/403")
    void postTanpaLogin_tidakDitolakCsrf() throws Exception {
        String body = """
                {"nama":"", "alamat":"Jakarta", "dob":"1990-01-01", "status":"AKTIF"}
                """;

        // Request lolos filter keamanan dan sampai ke service: yang muncul adalah
        // validasi bisnis "nama wajib diisi", bukan penolakan 401/403 dari Spring Security.
        // GlobalExceptionHandler memetakan InvalidRequestException itu ke 400 —
        // sebelumnya exception-nya lolos keluar karena belum ada handler sama sekali.
        mockMvc.perform(post("/api/karyawan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("nama wajib diisi"));
    }

    @Test
    @DisplayName("path yang tidak terdaftar dibalas 404, bukan 401")
    void pathTidakDikenal_dibalas404() throws Exception {
        mockMvc.perform(get("/path-yang-tidak-ada"))
                .andExpect(status().isNotFound());
    }
}
