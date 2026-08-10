package com.masesas.exercises.demo1.owasp;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.masesas.exercises.demo1.owasp.safe.CorrelationIdFilter;
import com.masesas.exercises.demo1.owasp.safe.RateLimitFilter;
import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import com.masesas.exercises.demo1.security.LoginAttemptService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("owasp-demo")
@Transactional
class A09LoggingTest {

    private static final LocalDate PERIODE = LocalDate.now().withDayOfMonth(1);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Autowired
    private LoginAttemptService loginAttempts;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Value("${app.security.password}")
    private String password;

    private Integer idKaryawan;
    private ListAppender<ILoggingEvent> audit;
    private Logger auditLogger;

    @BeforeEach
    void siapkan() {
        loginAttempts.reset("hr@masesas.test");
        rateLimitFilter.bersihkan();

        auditLogger = (Logger) LoggerFactory.getLogger("AUDIT");
        audit = new ListAppender<>();
        audit.start();
        auditLogger.addAppender(audit);

        idKaryawan = jdbcTemplate.queryForObject(
                "INSERT INTO masesas.karyawan (nama, alamat, dob, status, created_date) "
                        + "VALUES ('Uji A09', 'Jakarta', DATE '1990-01-01', 'AKTIF', now()) RETURNING id",
                Integer.class);

        jdbcTemplate.update(
                "INSERT INTO masesas.payroll_karyawan "
                        + "(id_karyawan, periode, gaji_pokok, tunjangan, potongan, status, version, created_date) "
                        + "VALUES (?, ?, 5000000, 0, 0, 'DRAFT', 0, now())",
                idKaryawan, Date.valueOf(PERIODE));
    }

    @AfterEach
    void bersihkan() {
        auditLogger.detachAppender(audit);
    }

    @Test
    @DisplayName("RENTAN: mengubah slip gaji lewat /api/vuln tidak meninggalkan jejak audit")
    void vuln_tanpaJejakAudit() throws Exception {
        mockMvc.perform(put("/api/vuln/payroll/" + idKaryawan + "/" + PERIODE + "/tanpa-versi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gajiPokok\":1}"))
                .andExpect(status().isOk());

        assertThat(barisAudit()).isEmpty();
    }

    @Test
    @DisplayName("AMAN: revisi slip gaji mencatat aksi, aktor, peran, dan IP")
    void safe_jejakAuditLengkap() throws Exception {
        mockMvc.perform(put("/api/payroll/" + idKaryawan + "/" + PERIODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gajiPokok\":6000000}")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk());

        assertThat(barisAudit()).hasSize(1);
        assertThat(barisAudit().get(0))
                .contains("aksi=payroll.update")
                .contains("aktor=hr@masesas.test")
                .contains("peran=ROLE_HR")
                .contains("hasil=BERHASIL");
    }

    @Test
    @DisplayName("AMAN: operasi yang gagal tetap tercatat, bukan hanya yang berhasil")
    void safe_kegagalanIkutTercatat() throws Exception {
        mockMvc.perform(put("/api/payroll/999999/" + PERIODE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gajiPokok\":6000000}")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNotFound());

        assertThat(barisAudit()).hasSize(1);
        assertThat(barisAudit().get(0)).contains("hasil=GAGAL:ResourceNotFoundException");
    }

    @Test
    @DisplayName("AMAN: login gagal dan berhasil sama-sama tercatat")
    void safe_eventLoginTercatat() throws Exception {
        mockMvc.perform(loginRequest("salah")).andExpect(status().isUnauthorized());
        mockMvc.perform(loginRequest(password)).andExpect(status().isOk());

        assertThat(barisAudit()).hasSize(2);
        assertThat(barisAudit().get(0)).contains("aksi=auth.login", "hasil=GAGAL:KREDENSIAL");
        assertThat(barisAudit().get(1)).contains("aksi=auth.login", "hasil=BERHASIL");
    }

    @Test
    @DisplayName("AMAN: correlation id dibuatkan dan dikembalikan di response")
    void safe_correlationIdDibuatkan() throws Exception {
        mockMvc.perform(get("/api/karyawan/all").header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().exists(CorrelationIdFilter.HEADER));
    }

    @Test
    @DisplayName("AMAN: correlation id dari klien dipakai kembali tapi dibersihkan dulu")
    void safe_correlationIdKlienDisanitasi() throws Exception {
        mockMvc.perform(get("/api/karyawan/all")
                        .header(CorrelationIdFilter.HEADER, "abc123\nINFO baris palsu")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(header().string(CorrelationIdFilter.HEADER,
                        "abc123_INFO baris palsu"));
    }

    private List<String> barisAudit() {
        return audit.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private org.springframework.test.web.servlet.RequestBuilder loginRequest(String password) {
        return post("/api/safe/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"hr@masesas.test\",\"password\":\"" + password + "\"}");
    }

    private String bearer() {
        AppUser user = userDetailsService.loadUserByUsername("hr@masesas.test");
        return "Bearer " + jwtService.issue(user, Instant.now());
    }
}
