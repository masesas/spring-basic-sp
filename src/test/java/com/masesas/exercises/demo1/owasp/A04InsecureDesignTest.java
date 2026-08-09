package com.masesas.exercises.demo1.owasp;

import com.masesas.exercises.demo1.owasp.safe.RateLimitFilter;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("owasp-demo")
@Transactional
class A04InsecureDesignTest {

    private static final int BATAS_PER_MENIT = 10;
    private static final LocalDate PERIODE = LocalDate.now().withDayOfMonth(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Autowired
    private LoginAttemptService loginAttempts;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserDetailsService userDetailsService;

    private Integer idKaryawan;

    @BeforeEach
    void siapkan() {
        rateLimitFilter.bersihkan();
        loginAttempts.reset("hr");

        idKaryawan = jdbcTemplate.queryForObject(
                "INSERT INTO masesas.karyawan (nama, alamat, dob, status, created_date) "
                        + "VALUES ('Uji A04', 'Jakarta', DATE '1990-01-01', 'AKTIF', now()) RETURNING id",
                Integer.class);

        jdbcTemplate.update(
                "INSERT INTO masesas.payroll_karyawan "
                        + "(id_karyawan, periode, gaji_pokok, tunjangan, potongan, status, created_date) "
                        + "VALUES (?, ?, 5000000, 0, 0, 'APPROVED', now())",
                idKaryawan, Date.valueOf(PERIODE));
    }

    @Test
    @DisplayName("RENTAN: login bisa dicoba tanpa batas")
    void vuln_tanpaPembatasanLaju() throws Exception {
        for (int i = 0; i < BATAS_PER_MENIT + 5; i++) {
            mockMvc.perform(loginRequest("/api/vuln/login"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("AMAN: permintaan ke-11 dalam satu menit dibalas 429")
    void safe_pembatasanLajuBekerja() throws Exception {
        for (int i = 0; i < BATAS_PER_MENIT; i++) {
            mockMvc.perform(loginRequest("/api/safe/login"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(loginRequest("/api/safe/login"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("RENTAN: slip gaji yang sudah APPROVED tetap bisa diubah diam-diam")
    void vuln_slipApprovedBisaDiubah() throws Exception {
        mockMvc.perform(put("/api/vuln/payroll/" + idKaryawan + "/" + PERIODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gajiPokok\":1}"))
                .andExpect(status().isOk());

        assertThat(gajiPokokTersimpan()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("AMAN: revisi slip gaji yang sudah APPROVED dibalas 422")
    void safe_slipApprovedTerkunci() throws Exception {
        mockMvc.perform(put("/api/payroll/" + idKaryawan + "/" + PERIODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gajiPokok\":1}")
                        .header(HttpHeaders.AUTHORIZATION, bearer("hr")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("sudah disetujui")));

        assertThat(gajiPokokTersimpan()).isEqualByComparingTo("5000000");
    }

    @Test
    @DisplayName("AMAN: slip yang masih DRAFT tetap bisa direvisi")
    void safe_slipDraftMasihBisaDirevisi() throws Exception {
        jdbcTemplate.update(
                "UPDATE masesas.payroll_karyawan SET status = 'DRAFT' "
                        + "WHERE id_karyawan = ? AND periode = ?",
                idKaryawan, Date.valueOf(PERIODE));

        mockMvc.perform(put("/api/payroll/" + idKaryawan + "/" + PERIODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gajiPokok\":7000000}")
                        .header(HttpHeaders.AUTHORIZATION, bearer("hr")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    private java.math.BigDecimal gajiPokokTersimpan() {
        return jdbcTemplate.queryForObject(
                "SELECT gaji_pokok FROM masesas.payroll_karyawan "
                        + "WHERE id_karyawan = ? AND periode = ?",
                java.math.BigDecimal.class,
                idKaryawan, Date.valueOf(PERIODE));
    }

    private org.springframework.test.web.servlet.RequestBuilder loginRequest(String path) {
        return post(path)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"hr\",\"password\":\"Password123!\"}");
    }

    private String bearer(String username) {
        AppUser user = userDetailsService.loadUserByUsername(username);
        return "Bearer " + jwtService.issue(user, Instant.now());
    }
}
