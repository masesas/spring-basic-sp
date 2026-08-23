package com.masesas.exercises.demo1.rbac;

import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import com.masesas.exercises.demo1.security.JwtService;
import com.masesas.exercises.demo1.service.RoleMapService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RbacSuperadminTest {

    private static final String SUPERADMIN = "superadmin@masesas.test";
    private static final String MARKETING = "marketing@masesas.test";
    private static final String KARYAWAN_TIDAK_ADA = "/api/karyawan/999999";
    private static final String PAYROLL_TIDAK_ADA = "/api/payroll/999999/2026-08-01";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Autowired
    private RoleMapService roleMapService;

    @Autowired
    private RoleHierarchy roleHierarchy;

    @Test
    @DisplayName("peran SUPERADMIN dimuat dari database dan tidak diperluas di daftar authority")
    void peranSuperadminDimuatDariDatabase() {
        AppUser superadmin = userDetailsService.findKaryawan(SUPERADMIN).orElseThrow();

        assertThat(superadmin.getRoles()).containsExactly(AppUser.ROLE_SUPERADMIN);
        assertThat(superadmin.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .contains("ROLE_SUPERADMIN", AppUser.ROLE_GUEST)
                .doesNotContain("ROLE_ADMIN", "ROLE_MANAGER", "ROLE_HR");
    }

    @Test
    @DisplayName("RoleHierarchy tidak mengimplikasikan permission, jadi SUPERADMIN memegangnya dari tabel")
    void permissionSuperadminDatangDariTabelBukanHierarki() {
        AppUser superadmin = userDetailsService.findKaryawan(SUPERADMIN).orElseThrow();

        assertThat(superadmin.getPermissions())
                .contains("LOAN_APPLICATION_APPROVE", "LOAN_APPLICATION_DISBURSE");

        List<String> dijangkauHierarki = roleHierarchy
                .getReachableGrantedAuthorities(
                        List.of(new SimpleGrantedAuthority("ROLE_" + AppUser.ROLE_SUPERADMIN)))
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(dijangkauHierarki).doesNotContain("LOAN_APPLICATION_APPROVE");
    }

    @Test
    @DisplayName("SUPERADMIN lolos endpoint yang hanya untuk ADMIN")
    void superadminLolosEndpointAdmin() throws Exception {
        mockMvc.perform(delete(KARYAWAN_TIDAK_ADA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(SUPERADMIN)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete(KARYAWAN_TIDAK_ADA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(MARKETING)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("SUPERADMIN lolos endpoint yang hanya untuk ADMIN dan HR")
    void superadminLolosEndpointHr() throws Exception {
        mockMvc.perform(delete(PAYROLL_TIDAK_ADA)
                        .header(HttpHeaders.AUTHORIZATION, bearer(SUPERADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SUPERADMIN lolos endpoint kelas KaryawanController")
    void superadminLolosEndpointKaryawan() throws Exception {
        mockMvc.perform(get("/api/karyawan/all")
                        .header(HttpHeaders.AUTHORIZATION, bearer(SUPERADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("SUPERADMIN lolos otorisasi endpoint CUSTOMER — 404 karena bukan customer, bukan 403")
    void superadminLolosOtorisasiEndpointCustomer() throws Exception {
        mockMvc.perform(get("/api/customer/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(SUPERADMIN)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("hierarki SUPERADMIN mencakup setiap peran yang dipakai @PreAuthorize")
    void hierarkiMencakupSeluruhPeranDiAnotasi() {
        String[] peranDiAnotasi = roleMapService.semua().stream()
                .flatMap(endpoint -> endpoint.getRoles().stream())
                .distinct()
                .map(peran -> "ROLE_" + peran)
                .toArray(String[]::new);

        List<String> dijangkauSuperadmin = roleHierarchy
                .getReachableGrantedAuthorities(
                        List.of(new SimpleGrantedAuthority("ROLE_" + AppUser.ROLE_SUPERADMIN)))
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        assertThat(dijangkauSuperadmin).contains(peranDiAnotasi);
    }

    @Test
    @DisplayName("rolemap SUPERADMIN memuat seluruh endpoint yang terdaftar")
    void rolemapSuperadminMemuatSeluruhEndpoint() {
        assertThat(roleMapService.untukPeran(AppUser.ROLE_SUPERADMIN).getJumlah())
                .isEqualTo(roleMapService.semua().size());
    }

    private String bearer(String email) {
        AppUser user = userDetailsService.findKaryawan(email).orElseThrow();
        return "Bearer " + jwtService.issue(user, Instant.now());
    }
}
