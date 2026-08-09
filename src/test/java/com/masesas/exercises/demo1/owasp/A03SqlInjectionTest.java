package com.masesas.exercises.demo1.owasp;

import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Menjalankan payload injeksi yang sama ke versi rentan dan versi aman.
 *
 * <p>Kelas ini {@code @Transactional} dan menanam datanya sendiri, jadi hasilnya tidak
 * bergantung pada isi database bersama dan seluruh baris uji di-rollback setelah selesai.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("owasp-demo")
@Transactional
class A03SqlInjectionTest {

    private static final String PAYLOAD = "' OR '1'='1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserDetailsService userDetailsService;

    @BeforeEach
    void tanamData() {
        jdbcTemplate.update(
                "INSERT INTO masesas.karyawan (nama, alamat, dob, status, created_date) "
                        + "VALUES (?, ?, ?, ?, now())",
                "Injeksi Satu", "Jakarta", java.sql.Date.valueOf("1990-01-01"), "AKTIF");
        jdbcTemplate.update(
                "INSERT INTO masesas.karyawan (nama, alamat, dob, status, created_date) "
                        + "VALUES (?, ?, ?, ?, now())",
                "Injeksi Dua", "Bandung", java.sql.Date.valueOf("1991-02-02"), "AKTIF");
    }

    @Test
    @DisplayName("RENTAN: payload ' OR '1'='1 membocorkan seluruh baris")
    void vuln_injeksiMembocorkanSemuaBaris() throws Exception {
        mockMvc.perform(get("/api/vuln/karyawan/search").param("nama", PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(1)));
    }

    @Test
    @DisplayName("AMAN: payload yang sama diperlakukan sebagai teks biasa, 0 baris")
    void safe_injeksiDiperlakukanSebagaiData() throws Exception {
        mockMvc.perform(get("/api/safe/karyawan/search")
                        .param("nama", PAYLOAD)
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("AMAN: nama yang benar tetap ketemu")
    void safe_pencarianNormalTetapBekerja() throws Exception {
        mockMvc.perform(get("/api/safe/karyawan/search")
                        .param("nama", "Injeksi Satu")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("RENTAN: ORDER BY menjalankan ekspresi SQL sembarang")
    void vuln_orderByMenjalankanEkspresi() throws Exception {
        mockMvc.perform(get("/api/vuln/karyawan/sort").param("by", "1/0"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("AMAN: kolom sort di luar allowlist dibalas 400")
    void safe_orderByDitolakAllowlist() throws Exception {
        mockMvc.perform(get("/api/safe/karyawan/sort")
                        .param("by", "1/0")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("kolom sort tidak dikenal")));
    }

    @Test
    @DisplayName("AMAN: kolom sort dalam allowlist tetap bekerja")
    void safe_orderByAllowlistBekerja() throws Exception {
        mockMvc.perform(get("/api/safe/karyawan/sort")
                        .param("by", "nama")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AMAN: parameter kosong dibalas 400 oleh Bean Validation")
    void safe_parameterKosongDitolak() throws Exception {
        mockMvc.perform(get("/api/safe/karyawan/search")
                        .param("nama", "  ")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest());
    }

    private String bearer() {
        AppUser user = userDetailsService.loadUserByUsername("hr");
        return "Bearer " + jwtService.issue(user, Instant.now());
    }
}
