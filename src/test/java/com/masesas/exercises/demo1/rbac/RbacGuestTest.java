package com.masesas.exercises.demo1.rbac;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.masesas.exercises.demo1.owasp.safe.RateLimitFilter;
import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.LoginAttemptService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RbacGuestTest {

    private static final String HR = "hr@masesas.test";
    private static final String PENGUNJUNG = "pengunjung@masesas.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Autowired
    private LoginAttemptService loginAttempts;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    private ListAppender<ILoggingEvent> audit;
    private Logger auditLogger;

    @BeforeEach
    void siapkan() {
        loginAttempts.reset(PENGUNJUNG);
        rateLimitFilter.bersihkan();

        auditLogger = (Logger) LoggerFactory.getLogger("AUDIT");
        audit = new ListAppender<>();
        audit.start();
        auditLogger.addAppender(audit);
    }

    @AfterEach
    void bersihkan() {
        auditLogger.detachAppender(audit);
        loginAttempts.reset(PENGUNJUNG);
    }

    @Test
    @DisplayName("pengunjung tanpa token berjalan sebagai principal guest dengan ROLE_GUEST")
    void anonimMembawaRoleGuest() throws Exception {
        mockMvc.perform(post("/api/safe/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + PENGUNJUNG + "\",\"password\":\"salah\"}"))
                .andExpect(status().isUnauthorized());

        assertThat(audit.list).hasSize(1);
        assertThat(audit.list.get(0).getFormattedMessage())
                .contains("aktor=guest")
                .contains("peran=ROLE_GUEST");
    }

    @Test
    @DisplayName("ROLE_GUEST tidak membuka endpoint terlindung — tanpa token tetap 401")
    void anonimTetapDitolakDiEndpointTerlindung() throws Exception {
        mockMvc.perform(get("/api/karyawan/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("pengguna yang sudah login juga membawa ROLE_GUEST di samping peran databasenya")
    void penggunaLoginJugaMembawaRoleGuest() {
        AppUser hr = userDetailsService.findKaryawan(HR).orElseThrow();

        assertThat(hr.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactly("ROLE_HR", AppUser.ROLE_GUEST);
    }
}
