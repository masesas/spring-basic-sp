package com.masesas.exercises.demo1.rbac;

import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RbacRoleDatabaseTest {

    private static final String HR = "hr@masesas.test";
    private static final String MARKETING = "marketing@masesas.test";
    private static final int ID_MARKETING = 3;
    private static final String PAYROLL_TIDAK_ADA = "/api/payroll/999999/2026-08-01";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("akun demo lama yang di-hardcode sudah tidak dikenal lagi")
    void demoUserLamaTidakDikenalLagi() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("hr"))
                .isInstanceOf(UsernameNotFoundException.class);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("karyawan"))
                .isInstanceOf(UsernameNotFoundException.class);

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("admin"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("peran HR dimuat dari tabel karyawan_role, bukan dari kode")
    void peranDimuatDariKaryawanRole() {
        AppUser hr = userDetailsService.findKaryawan(HR).orElseThrow();

        assertThat(hr.getRoles()).containsExactly("HR");
        assertThat(hr.getIdKaryawan()).isNotNull();
    }

    @Test
    @DisplayName("peran HR dari database lolos otorisasi hapus slip gaji, MARKETING tidak")
    void peranHrDariDatabaseLolosOtorisasiPayroll() throws Exception {
        mockMvc.perform(delete(PAYROLL_TIDAK_ADA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(HR)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(PAYROLL_TIDAK_ADA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(MARKETING)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("pencabutan peran di database langsung berlaku tanpa menunggu token kedaluwarsa")
    void pencabutanPeranLangsungBerlaku() {
        assertThat(userDetailsService.findKaryawan(MARKETING).orElseThrow().getRoles())
                .containsExactly("MARKETING");

        jdbcTemplate.update("DELETE FROM masesas.karyawan_role WHERE id_karyawan = ?", ID_MARKETING);

        assertThat(userDetailsService.findKaryawan(MARKETING).orElseThrow().getRoles()).isEmpty();
    }

    private String bearer(String email) {
        AppUser user = userDetailsService.findKaryawan(email).orElseThrow();
        return "Bearer " + jwtService.issue(user, Instant.now());
    }
}
