package com.masesas.exercises.demo1.owasp;

import com.masesas.exercises.demo1.owasp.safe.RateLimitFilter;
import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class A05MisconfigTest {

    private static final String[] KREDENSIAL_YANG_TIDAK_BOLEH_ADA = {
            "binar_bc_password",
            "binar_admin",
            "binar_app",
            "129.226.195.9",
            "Password123!"
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void bersihkan() {
        rateLimitFilter.bersihkan();
    }

    @Test
    @DisplayName("application.properties tidak memuat satu pun kredensial literal")
    void propertiesBebasKredensial() throws Exception {
        String isi = new String(
                new ClassPathResource("application.properties").getContentAsByteArray(),
                StandardCharsets.UTF_8);

        assertThat(isi).doesNotContain(KREDENSIAL_YANG_TIDAK_BOLEH_ADA);
    }

    @Test
    @DisplayName("kredensial dibaca lewat placeholder, bukan nilai tertanam")
    void propertiesMemakaiPlaceholder() throws Exception {
        String isi = new String(
                new ClassPathResource("application.properties").getContentAsByteArray(),
                StandardCharsets.UTF_8);

        assertThat(isi)
                .contains("spring.datasource.password=${DB_PASSWORD}")
                .contains("app.crypto.key=${CRYPTO_KEY}")
                .contains("spring.config.import=optional:file:.env[.properties]");
    }

    @Test
    @DisplayName("response memuat header keamanan yang wajib")
    void headerKeamananTerpasang() throws Exception {
        mockMvc.perform(get("/api/karyawan/all").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Content-Security-Policy",
                        "default-src 'none'; frame-ancestors 'none'; base-uri 'none'"));
    }

    @Test
    @DisplayName("HSTS dikirim pada koneksi aman")
    void hstsPadaKoneksiAman() throws Exception {
        mockMvc.perform(get("/api/karyawan/all")
                        .secure(true)
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(header().string("Strict-Transport-Security",
                        "max-age=31536000 ; includeSubDomains"));
    }

    @Test
    @DisplayName("CORS menerima origin yang terdaftar")
    void corsOriginTerdaftarDiterima() throws Exception {
        mockMvc.perform(options("/api/karyawan/all")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
    }

    @Test
    @DisplayName("CORS menolak origin yang tidak terdaftar")
    void corsOriginAsingDitolak() throws Exception {
        mockMvc.perform(options("/api/karyawan/all")
                        .header(HttpHeaders.ORIGIN, "https://situs-penyerang.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("error tak terduga tidak membocorkan stacktrace ke klien")
    void errorTidakMembocorkanInternal() throws Exception {
        mockMvc.perform(get("/api/payroll/999999/2026-08-01")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    private String bearer() {
        AppUser user = userDetailsService.loadUserByUsername("hr");
        return "Bearer " + jwtService.issue(user, Instant.now());
    }
}
