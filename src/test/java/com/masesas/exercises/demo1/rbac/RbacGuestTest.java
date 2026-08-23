package com.masesas.exercises.demo1.rbac;

import com.masesas.exercises.demo1.security.AppUser;
import com.masesas.exercises.demo1.security.AppUserDetailsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RbacGuestTest {

    private static final String HR = "hr@masesas.test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AppUserDetailsService userDetailsService;

    @Test
    @DisplayName("ROLE_GUEST tidak membuka endpoint terlindung — tanpa token tetap 401")
    void anonimTetapDitolakDiEndpointTerlindung() throws Exception {
        mockMvc.perform(get("/api/karyawan/all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("ROLE_GUEST hanya berlaku untuk endpoint yang publik")
    void guestHanyaMendapatEndpointPublik() throws Exception {
        mockMvc.perform(get("/api/rolemap/GUEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endpoint[?(@.public==false)]").isEmpty());
    }

    @Test
    @DisplayName("pengguna yang sudah login juga membawa ROLE_GUEST di samping peran databasenya")
    void penggunaLoginJugaMembawaRoleGuest() {
        AppUser hr = userDetailsService.findKaryawan(HR).orElseThrow();

        assertThat(hr.getAuthorities().stream().map(GrantedAuthority::getAuthority))
                .containsExactly("ROLE_HR", AppUser.ROLE_GUEST);
    }
}
