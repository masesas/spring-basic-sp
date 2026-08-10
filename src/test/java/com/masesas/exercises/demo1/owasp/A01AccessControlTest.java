package com.masesas.exercises.demo1.owasp;

import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("owasp-demo")
class A01AccessControlTest {

    private static final int ID_KARYAWAN_SENDIRI = 8;
    private static final int ID_KARYAWAN_ORANG_LAIN = 2;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Test
    @DisplayName("RENTAN: tanpa token pun slip gaji orang lain terbaca di /api/vuln")
    void vuln_idorTerbukaTanpaToken() throws Exception {
        mockMvc.perform(get("/api/vuln/payroll/" + ID_KARYAWAN_ORANG_LAIN))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AMAN: karyawan membaca slip gaji orang lain dibalas 403")
    void safe_idorDitolak() throws Exception {
        mockMvc.perform(get("/api/safe/payroll/" + ID_KARYAWAN_ORANG_LAIN)
                        .header(HttpHeaders.AUTHORIZATION, bearer("karyawan@masesas.test")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AMAN: karyawan tetap boleh membaca slip gajinya sendiri")
    void safe_slipSendiriBolehDibaca() throws Exception {
        mockMvc.perform(get("/api/safe/payroll/" + ID_KARYAWAN_SENDIRI)
                        .header(HttpHeaders.AUTHORIZATION, bearer("karyawan@masesas.test")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AMAN: HR boleh membaca slip gaji karyawan mana pun")
    void safe_hrBolehMembacaSemua() throws Exception {
        mockMvc.perform(get("/api/safe/payroll/" + ID_KARYAWAN_ORANG_LAIN)
                        .header(HttpHeaders.AUTHORIZATION, bearer("hr@masesas.test")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RENTAN: karyawan biasa bisa menghapus karyawan lewat /api/vuln")
    void vuln_hapusTanpaCekPeran() throws Exception {
        mockMvc.perform(delete("/api/vuln/karyawan/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("AMAN: karyawan biasa dilarang menghapus, dibalas 403")
    void safe_hapusButuhPeranAdmin() throws Exception {
        mockMvc.perform(delete("/api/safe/karyawan/999999")
                        .header(HttpHeaders.AUTHORIZATION, bearer("karyawan@masesas.test")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AMAN: karyawan biasa dilarang membuat slip gaji di /api/payroll")
    void safe_tulisPayrollButuhPeranHr() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/payroll")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"idKaryawan\":1,\"periode\":\"2026-08-01\",\"gajiPokok\":1000}")
                        .header(HttpHeaders.AUTHORIZATION, bearer("karyawan@masesas.test")))
                .andExpect(status().isForbidden());
    }

    private String bearer(String username) {
        AppUser user = userDetailsService.loadUserByUsername(username);
        return "Bearer " + jwtService.issue(user, Instant.now());
    }
}
