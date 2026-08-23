package com.masesas.exercises.demo1.rbac;

import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import com.masesas.exercises.demo1.security.LoginAttemptService;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.RequestBuilder;

import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RbacKaryawanTest {

    private static final String ADMIN = "admin@masesas.test";
    private static final String MARKETING = "marketing@masesas.test";
    private static final String MANAGER_SALES = "manager.sales@masesas.test";
    private static final String TANPA_ROLE = "tanparole@masesas.test";
    private static final String TANPA_PASSWORD = "karyawan0100@masesas.test";
    private static final String ID_TIDAK_ADA = "999999";
    private static final String PERIODE = "2026-08-01";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Autowired
    private LoginAttemptService loginAttempts;

    @Value("${app.security.password}")
    private String demoPassword;

    @BeforeEach
    @AfterEach
    void bersihkanPenguncian() {
        loginAttempts.reset(TANPA_PASSWORD);
    }

    @Test
    @DisplayName("login admin mengembalikan token, tipe KARYAWAN, dan peran dari tabel karyawan_role")
    void loginAdminMembawaPeranDariDatabase() throws Exception {
        mockMvc.perform(loginKaryawan(ADMIN, demoPassword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.tipe").value("KARYAWAN"))
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"));
    }

    @Test
    @DisplayName("satu karyawan boleh punya lebih dari satu peran")
    void karyawanBisaPunyaBanyakPeran() throws Exception {
        mockMvc.perform(loginKaryawan(MANAGER_SALES, demoPassword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles.length()").value(2));
    }

    @Test
    @DisplayName("ADMIN lolos otorisasi DELETE karyawan — ditolak datanya, bukan perannya")
    void adminBolehMenghapusKaryawan() throws Exception {
        mockMvc.perform(delete("/api/karyawan/" + ID_TIDAK_ADA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(ADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("MARKETING boleh membaca karyawan tapi dilarang mengubahnya")
    void marketingHanyaBolehMembaca() throws Exception {
        mockMvc.perform(get("/api/karyawan/all")
                        .header(HttpHeaders.AUTHORIZATION, bearer(MARKETING)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/karyawan/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nama\":\"Percobaan Marketing\",\"status\":\"AKTIF\"}")
                        .header(HttpHeaders.AUTHORIZATION, bearer(MARKETING)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MARKETING tidak boleh menyentuh slip gaji sama sekali")
    void marketingDitolakDiPayroll() throws Exception {
        mockMvc.perform(get("/api/payroll/" + ID_TIDAK_ADA + "/" + PERIODE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(MARKETING)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("MANAGER lolos otorisasi slip gaji")
    void managerBolehMembacaPayroll() throws Exception {
        mockMvc.perform(get("/api/payroll/" + ID_TIDAK_ADA + "/" + PERIODE)
                        .header(HttpHeaders.AUTHORIZATION, bearer(MANAGER_SALES)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("karyawan tanpa peran tetap bisa login, tapi setiap endpoint membalas 403")
    void tanpaPeranLoginBerhasilAksesDitolak() throws Exception {
        mockMvc.perform(loginKaryawan(TANPA_ROLE, demoPassword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles.length()").value(0));

        mockMvc.perform(get("/api/karyawan/all")
                        .header(HttpHeaders.AUTHORIZATION, bearer(TANPA_ROLE)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("karyawan yang password-nya NULL dibalas 401, bukan 500")
    void karyawanTanpaPasswordDitolak() throws Exception {
        mockMvc.perform(loginKaryawan(TANPA_PASSWORD, demoPassword))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("peran dibaca dari database, bukan dari klaim di dalam token")
    void klaimPeranDiTokenTidakMenentukan() throws Exception {
        AppUser palsu = new AppUser(
                MARKETING, "tidak-dipakai", List.of("ADMIN"), List.of(), 3, AppUser.TIPE_KARYAWAN);
        String token = jwtService.issue(palsu, Instant.now());

        mockMvc.perform(delete("/api/karyawan/" + ID_TIDAK_ADA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private RequestBuilder loginKaryawan(String email, String password) {
        return post("/api/auth/karyawan/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    private String bearer(String email) {
        AppUser user = userDetailsService.findKaryawan(email).orElseThrow();
        return "Bearer " + jwtService.issue(user, Instant.now());
    }
}
