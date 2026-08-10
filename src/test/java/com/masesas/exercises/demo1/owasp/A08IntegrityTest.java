package com.masesas.exercises.demo1.owasp;

import com.masesas.exercises.demo1.dto.KaryawanResponse;
import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("owasp-demo")
@Transactional
class A08IntegrityTest {

    private static final LocalDate PERIODE = LocalDate.now().withDayOfMonth(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GenericJacksonJsonRedisSerializer cacheValueSerializer;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserDetailsService userDetailsService;

    private Integer idKaryawan;

    @BeforeEach
    void tanamData() {
        idKaryawan = jdbcTemplate.queryForObject(
                "INSERT INTO masesas.karyawan (nama, alamat, dob, status, created_date) "
                        + "VALUES ('Uji A08', 'Jakarta', DATE '1990-01-01', 'AKTIF', now()) RETURNING id",
                Integer.class);

        jdbcTemplate.update(
                "INSERT INTO masesas.payroll_karyawan "
                        + "(id_karyawan, periode, gaji_pokok, tunjangan, potongan, status, version, created_date) "
                        + "VALUES (?, ?, 5000000, 0, 0, 'DRAFT', 0, now())",
                idKaryawan, Date.valueOf(PERIODE));
    }

    @Test
    @DisplayName("RENTAN: dua penulisan berurutan menimpa satu sama lain tanpa peringatan")
    void vuln_lostUpdateTerjadiDiam() throws Exception {
        mockMvc.perform(put(pathVuln()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"gajiPokok\":6000000}"))
                .andExpect(status().isOk());

        mockMvc.perform(put(pathVuln()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"gajiPokok\":7000000}"))
                .andExpect(status().isOk());

        assertThat(gajiPokokTersimpan()).isEqualByComparingTo("7000000");
    }

    @Test
    @DisplayName("AMAN: penulisan kedua dengan versi basi dibalas 409")
    void safe_versiBasiDitolak() throws Exception {
        mockMvc.perform(put(pathAman()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"gajiPokok\":6000000}")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(put(pathAman()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"gajiPokok\":7000000}")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("sudah diubah orang lain")));

        assertThat(gajiPokokTersimpan()).isEqualByComparingTo("6000000");
    }

    @Test
    @DisplayName("AMAN: versi yang benar tetap diterima dan version naik")
    void safe_versiBenarDiterima() throws Exception {
        mockMvc.perform(put(pathAman()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"gajiPokok\":6000000}")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk());

        mockMvc.perform(put(pathAman()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"gajiPokok\":7000000}")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    @DisplayName("AMAN: tipe dalam allowlist tetap bisa dibaca kembali dari cache")
    void safe_tipeDiizinkanTetapBekerja() {
        KaryawanResponse asli = new KaryawanResponse(
                1, "Budi", "Jakarta", LocalDate.of(1990, 1, 1), "AKTIF", null,
                Instant.parse("2024-01-01T00:00:00Z"), Instant.parse("2024-01-01T00:00:00Z"));

        Object hasil = cacheValueSerializer.deserialize(cacheValueSerializer.serialize(asli));

        assertThat(hasil).isEqualTo(asli);
    }

    @Test
    @DisplayName("AMAN: kelas di luar allowlist ditolak saat dibaca dari cache")
    void safe_tipeDiluarAllowlistDitolak() {
        byte[] jahat = "{\"@class\":\"java.io.File\",\"path\":\"/etc/passwd\"}"
                .getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> cacheValueSerializer.deserialize(jahat))
                .isInstanceOf(Exception.class);
    }

    private String pathVuln() {
        return "/api/vuln/payroll/" + idKaryawan + "/" + PERIODE + "/tanpa-versi";
    }

    private String pathAman() {
        return "/api/payroll/" + idKaryawan + "/" + PERIODE;
    }

    private BigDecimal gajiPokokTersimpan() {
        return jdbcTemplate.queryForObject(
                "SELECT gaji_pokok FROM masesas.payroll_karyawan WHERE id_karyawan = ? AND periode = ?",
                BigDecimal.class, idKaryawan, Date.valueOf(PERIODE));
    }

    private String bearer() {
        AppUser user = userDetailsService.loadUserByUsername("hr@masesas.test");
        return "Bearer " + jwtService.issue(user, Instant.now());
    }
}
