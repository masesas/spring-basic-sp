package com.masesas.exercises.demo1.owasp;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.masesas.exercises.demo1.owasp.safe.A03XssSafeController;
import com.masesas.exercises.demo1.owasp.vuln.A03XssVulnController;
import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("owasp-demo")
@Transactional
class A03XssTest {

    private static final String PAYLOAD_XSS = "<script>alert(1)</script>";
    private static final String PAYLOAD_LOG =
            "budi\nWARN  Saldo kas berhasil ditransfer ke rekening penyerang";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Test
    @DisplayName("RENTAN: payload script tersimpan utuh dan dikembalikan apa adanya")
    void vuln_storedXssTersimpan() throws Exception {
        mockMvc.perform(post("/api/vuln/karyawan/teks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PAYLOAD_XSS)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nama").value(PAYLOAD_XSS));
    }

    @Test
    @DisplayName("AMAN: payload script ditolak 400 sebelum menyentuh database")
    void safe_storedXssDitolak() throws Exception {
        mockMvc.perform(post("/api/safe/karyawan/teks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(PAYLOAD_XSS))
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("markup HTML")));
    }

    @Test
    @DisplayName("AMAN: nama wajar tetap bisa disimpan")
    void safe_namaWajarTetapDiterima() throws Exception {
        mockMvc.perform(post("/api/safe/karyawan/teks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Budi Santoso"))
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nama").value("Budi Santoso"));
    }

    @Test
    @DisplayName("RENTAN: baris baru dari input memalsukan baris log baru")
    void vuln_logInjectionBerhasil() throws Exception {
        ListAppender<ILoggingEvent> appender = pasangPenyadap(A03XssVulnController.class);

        mockMvc.perform(get("/api/vuln/karyawan/log").param("keyword", PAYLOAD_LOG))
                .andExpect(status().isOk());

        assertThat(pesanTerakhir(appender)).contains("\n");
    }

    @Test
    @DisplayName("AMAN: baris baru dibuang sehingga log tetap satu baris")
    void safe_logInjectionDicegah() throws Exception {
        ListAppender<ILoggingEvent> appender = pasangPenyadap(A03XssSafeController.class);

        mockMvc.perform(get("/api/safe/karyawan/log")
                        .param("keyword", PAYLOAD_LOG)
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk());

        assertThat(pesanTerakhir(appender)).doesNotContain("\n").contains("budi_WARN");
    }

    @Test
    @DisplayName("AMAN: response memuat header X-Content-Type-Options: nosniff")
    void safe_headerNosniffTerpasang() throws Exception {
        mockMvc.perform(get("/api/safe/karyawan/log")
                        .param("keyword", "budi")
                        .header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    private ListAppender<ILoggingEvent> pasangPenyadap(Class<?> kelas) {
        Logger logger = (Logger) LoggerFactory.getLogger(kelas);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private String pesanTerakhir(ListAppender<ILoggingEvent> appender) {
        assertThat(appender.list).isNotEmpty();
        return appender.list.get(appender.list.size() - 1).getFormattedMessage();
    }

    private String body(String nama) {
        return "{\"nama\":" + quote(nama) + ",\"alamat\":\"Jakarta\"}";
    }

    private String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private String bearer() {
        AppUser user = userDetailsService.loadUserByUsername("hr@masesas.test");
        return "Bearer " + jwtService.issue(user, Instant.now());
    }
}
